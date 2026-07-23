package de.destenylp.xBotenyy.twitchbot.automod;

import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.common.automod.AutomodEngine;
import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.common.observability.Metrics;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatClient;
import de.destenylp.xBotenyy.twitchbot.chat.TwitchChatMessage;
import de.destenylp.xBotenyy.twitchbot.eventlog.TwitchEventLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchAutomodAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitchAutomodAdapter.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");
    private static final long KICK_FALLBACK_TIMEOUT_SECONDS = 60;

    private final AutomodEngine engine;
    private final TwitchChatClient chatClient;
    private final TwitchModerationApiClient moderationApiClient;
    private final String warnMessageTemplate;
    private final String moderatorUserId;
    private final TwitchEventLogService eventLogService;
    private final Map<String, String> broadcasterIdCache = new ConcurrentHashMap<>();
    private final ExecutorService moderationExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "twitch-automod");
        thread.setDaemon(true);
        return thread;
    });

    public TwitchAutomodAdapter(AutomodSettings settings,
                                 GroqSafeguardClient moderationClient,
                                 TwitchChatClient chatClient,
                                 TwitchModerationApiClient moderationApiClient,
                                 String warnMessageTemplate,
                                 String moderatorUserId,
                                 TwitchEventLogService eventLogService) {
        this.engine = new AutomodEngine(settings, moderationClient);
        this.chatClient = chatClient;
        this.moderationApiClient = moderationApiClient;
        this.warnMessageTemplate = warnMessageTemplate;
        this.moderatorUserId = moderatorUserId;
        this.eventLogService = eventLogService;
    }

    public AutomodEngine getEngine() {
        return engine;
    }

    public void shutdown() {
        engine.shutdown();
        moderationExecutor.shutdownNow();
    }

    public boolean handleMessage(TwitchChatMessage message) {
        if (!engine.getSettings().isEnabled()) {
            return false;
        }
        if (engine.isChannelExempt(message.channelLogin())) {
            return false;
        }
        boolean bypassBroadcaster = engine.getSettings().isBypassAdministrator() && message.broadcaster();
        boolean bypassModerator = engine.getSettings().isBypassManageServer() && message.moderator();
        if (bypassBroadcaster || bypassModerator) {
            return false;
        }
        if (engine.isExempt(List.of(message.userLogin()))) {
            return false;
        }

        String activityKey = message.channelLogin() + ":" + message.userId();
        int mentionCount = countMentions(message.content());

        Optional<AutomodVerdict> verdict = engine.evaluate(message.content(), activityKey, mentionCount);
        if (verdict.isPresent()) {
            applyVerdict(message, verdict.get());
            return true;
        }

        engine.evaluateAiAsync(message.content(), aiVerdict -> aiVerdict.ifPresent(v -> applyVerdict(message, v)));
        return false;
    }

    private int countMentions(String content) {
        Matcher matcher = MENTION_PATTERN.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private void applyVerdict(TwitchChatMessage message, AutomodVerdict verdict) {
        AutomodAction finalAction = engine.registerViolationAndEscalate(message.channelLogin(), message.userId(), verdict.action());
        int strikes = engine.getCurrentStrikes(message.channelLogin(), message.userId());

        LOGGER.info("AutoMod: {} durch {} in Kanal {} (Aktion: {}, Strikes: {})",
                verdict.ruleType(), message.userLogin(), message.channelLogin(), finalAction, strikes);
        eventLogService.record(message.channelLogin(), message.userId(), "AUTOMOD_" + verdict.ruleType(),
                finalAction + " (Strikes: " + strikes + ") - " + verdict.reason());
        Metrics.increment("twitch.automod_violations_detected");

        moderationExecutor.submit(() -> {
            String broadcasterId = resolveBroadcasterId(message.channelLogin());
            if (broadcasterId == null) {
                LOGGER.warn("Konnte Broadcaster-ID fuer Kanal {} nicht aufloesen, Aktion wird uebersprungen.", message.channelLogin());
                return;
            }

            if (finalAction.deletesMessage() && message.messageId() != null) {
                moderationApiClient.deleteMessage(broadcasterId, moderatorUserId, message.messageId());
            }

            String reason = "AutoMod: " + verdict.reason();
            switch (finalAction) {
                case WARN -> chatClient.sendMessage(message.channelLogin(), warnMessageTemplate
                        .replace("{user}", message.displayName())
                        .replace("{reason}", verdict.reason()));
                case TIMEOUT -> moderationApiClient.banUser(broadcasterId, moderatorUserId, message.userId(), reason,
                        engine.getTimeoutDuration().toSeconds());

                case KICK -> moderationApiClient.banUser(broadcasterId, moderatorUserId, message.userId(), reason,
                        KICK_FALLBACK_TIMEOUT_SECONDS);
                case BAN -> moderationApiClient.banUser(broadcasterId, moderatorUserId, message.userId(), reason, 0);
                default -> {
                }
            }
        });
    }

    private String resolveBroadcasterId(String channelLogin) {
        return broadcasterIdCache.computeIfAbsent(channelLogin,
                login -> moderationApiClient.resolveUserId(login).orElse(null));
    }
}
