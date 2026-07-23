package de.destenylp.xBotenyy.common.automod;

import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.common.core.Prunable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutomodEngine implements Prunable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutomodEngine.class);
    private static final Pattern INVITE_PATTERN = Pattern.compile(
            "(?:https?://)?(?:www\\.)?(?:discord\\.(?:gg|io|me|li)|discord(?:app)?\\.com/invite)/([a-zA-Z0-9-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://([a-zA-Z0-9.-]+)(?:[/:?#][^\\s]*)?", Pattern.CASE_INSENSITIVE);

    private final AutomodSettings settings;
    private final GroqSafeguardClient moderationClient;
    private final AutomodActivityTracker activityTracker = new AutomodActivityTracker();
    private final AutomodStrikeTracker strikeTracker = new AutomodStrikeTracker();
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(2, aiThreadFactory());

    public AutomodEngine(AutomodSettings settings, GroqSafeguardClient moderationClient) {
        this.settings = settings;
        this.moderationClient = moderationClient;
    }

    private static ThreadFactory aiThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "automod-ai");
            thread.setDaemon(true);
            return thread;
        };
    }

    public String getServiceName() {
        return "AutoMod";
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        long expiryMillis = Duration.ofMinutes(Math.max(settings.getStrikeConfig().expiryMinutes(), 1)).toMillis();
        int removedStrikes = strikeTracker.purgeExpired(expiryMillis);
        int removedActivity = activityTracker.purgeStaleEntries(retention.toMillis());
        return removedStrikes + removedActivity;
    }

    public void shutdown() {
        aiExecutor.shutdownNow();
    }

    public AutomodSettings getSettings() {
        return settings;
    }

    public boolean isAiAvailable() {
        return moderationClient != null;
    }

    public boolean isExempt(java.util.Collection<String> memberIdentifiers) {
        if (memberIdentifiers == null) {
            return true;
        }
        return memberIdentifiers.stream().anyMatch(id -> settings.getExemptRoleIds().contains(id));
    }

    public boolean isChannelExempt(String channelId) {
        return channelId != null && settings.getExemptChannelIds().contains(channelId);
    }

    public Optional<AutomodVerdict> evaluate(String content, String activityKey, int mentionCount) {
        long now = System.currentTimeMillis();

        int spamCount = -1;
        AutomodSettings.SpamFilterConfig spamFilter = settings.getSpamFilter();
        if (spamFilter.enabled()) {
            spamCount = activityTracker.registerMessage(activityKey, now, Duration.ofSeconds(spamFilter.windowSeconds()).toMillis());
        }

        int duplicateCount = -1;
        AutomodSettings.DuplicateFilterConfig duplicateFilter = settings.getDuplicateFilter();
        if (duplicateFilter.enabled()) {
            duplicateCount = activityTracker.registerDuplicate(activityKey, AutomodTextNormalizer.normalizeSpaced(content), now);
        }

        Optional<AutomodVerdict> textVerdict = evaluateTextOnly(content);
        if (textVerdict.isPresent()) {
            return textVerdict;
        }

        AutomodSettings.MentionFilterConfig mentionFilter = settings.getMentionFilter();
        if (mentionFilter.enabled() && mentionCount > mentionFilter.maxMentions()) {
            return Optional.of(new AutomodVerdict(AutomodRuleType.MASS_MENTIONS, mentionFilter.action(),
                    "Zu viele Erwähnungen: " + mentionCount, String.valueOf(mentionCount)));
        }

        if (spamFilter.enabled() && spamCount > spamFilter.maxMessages()) {
            return Optional.of(new AutomodVerdict(AutomodRuleType.SPAM, spamFilter.action(),
                    "Zu viele Nachrichten in kurzer Zeit: " + spamCount, String.valueOf(spamCount)));
        }

        if (duplicateFilter.enabled() && duplicateCount >= duplicateFilter.maxRepeats()) {
            return Optional.of(new AutomodVerdict(AutomodRuleType.DUPLICATE_MESSAGES, duplicateFilter.action(),
                    "Nachricht mehrfach wiederholt: " + duplicateCount + "x", String.valueOf(duplicateCount)));
        }

        return Optional.empty();
    }

    public Optional<AutomodVerdict> evaluateTextOnly(String content) {
        Optional<String> wordMatch = settings.getWordFilter().findMatch(content);
        if (wordMatch.isPresent()) {
            return Optional.of(new AutomodVerdict(AutomodRuleType.BANNED_WORDS, settings.getWordFilter().getAction(),
                    "Verbotenes Wort erkannt: " + wordMatch.get(), wordMatch.get()));
        }

        AutomodSettings.InviteFilterConfig inviteFilter = settings.getInviteFilter();
        if (inviteFilter.enabled()) {
            Matcher inviteMatcher = INVITE_PATTERN.matcher(content);
            while (inviteMatcher.find()) {
                String code = inviteMatcher.group(1).toLowerCase(Locale.ROOT);
                if (!inviteFilter.whitelistCodes().contains(code)) {
                    return Optional.of(new AutomodVerdict(AutomodRuleType.INVITE_LINKS, inviteFilter.action(),
                            "Nicht erlaubter Discord-Invite: " + code, code));
                }
            }
        }

        AutomodSettings.CapsFilterConfig capsFilter = settings.getCapsFilter();
        if (capsFilter.enabled() && content.length() >= capsFilter.minLength()) {
            int percentage = computeCapsPercentage(content);
            if (percentage > capsFilter.maxPercentage()) {
                return Optional.of(new AutomodVerdict(AutomodRuleType.EXCESSIVE_CAPS, capsFilter.action(),
                        "Zu viel Großschreibung: " + percentage + "%", percentage + "%"));
            }
        }

        AutomodSettings.LinkFilterConfig linkFilter = settings.getLinkFilter();
        if (linkFilter.enabled()) {
            Matcher urlMatcher = URL_PATTERN.matcher(content);
            while (urlMatcher.find()) {
                String host = urlMatcher.group(1).toLowerCase(Locale.ROOT);
                if (linkFilter.whitelistDomains().stream().noneMatch(domain -> host.equals(domain) || host.endsWith("." + domain))) {
                    return Optional.of(new AutomodVerdict(AutomodRuleType.LINKS, linkFilter.action(),
                            "Nicht erlaubter Link: " + host, host));
                }
            }
        }

        return Optional.empty();
    }

    public void evaluateAiAsync(String content, Consumer<Optional<AutomodVerdict>> callback) {
        AutomodSettings.AiFilterConfig aiFilter = settings.getAiFilter();
        if (!aiFilter.enabled() || moderationClient == null || content == null || content.isBlank() || content.length() < 3) {
            callback.accept(Optional.empty());
            return;
        }
        aiExecutor.submit(() -> {
            Optional<GroqSafeguardClient.ModerationResult> result = moderationClient.moderate(content);
            if (result.isEmpty()) {
                LOGGER.warn("Groq Content-Moderation lieferte kein Ergebnis fuer eine Nachricht (API-Fehler oder Timeout, siehe vorherige Logzeile)");
            } else {
                LOGGER.debug("Groq Content-Moderation Ergebnis: flagged={} topCategory={}",
                        result.get().flagged(), result.get().topCategory());
            }
            Optional<AutomodVerdict> verdict = result
                    .filter(GroqSafeguardClient.ModerationResult::flagged)
                    .map(value -> new AutomodVerdict(AutomodRuleType.AI_TOXICITY, aiFilter.action(),
                            String.format(Locale.GERMANY, "Groq Content-Moderation: Kategorie '%s' (%s)",
                                    value.topCategory(), value.rationale()),
                            content));
            callback.accept(verdict);
        });
    }

    public AutomodAction registerViolationAndEscalate(String scopeId, String actorId, AutomodAction baseAction) {
        AutomodSettings.StrikeConfig strikeConfig = settings.getStrikeConfig();
        if (!strikeConfig.enabled()) {
            return baseAction;
        }
        String key = scopeId + ":" + actorId;
        long expiryMillis = Duration.ofMinutes(Math.max(strikeConfig.expiryMinutes(), 1)).toMillis();
        int strikes = strikeTracker.recordStrike(key, System.currentTimeMillis(), expiryMillis);

        AutomodAction escalated = AutomodAction.NONE;
        if (strikeConfig.banThreshold() > 0 && strikes >= strikeConfig.banThreshold()) {
            escalated = AutomodAction.BAN;
        } else if (strikeConfig.kickThreshold() > 0 && strikes >= strikeConfig.kickThreshold()) {
            escalated = AutomodAction.KICK;
        } else if (strikeConfig.timeoutThreshold() > 0 && strikes >= strikeConfig.timeoutThreshold()) {
            escalated = AutomodAction.TIMEOUT;
        }
        return AutomodAction.max(baseAction, escalated);
    }

    public int getCurrentStrikes(String scopeId, String actorId) {
        return strikeTracker.getCurrentStrikes(scopeId + ":" + actorId);
    }

    public Duration getTimeoutDuration() {
        return Duration.ofMinutes(Math.max(settings.getStrikeConfig().timeoutDurationMinutes(), 1));
    }

    private int computeCapsPercentage(String content) {
        int letters = 0;
        int uppercase = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    uppercase++;
                }
            }
        }
        if (letters == 0) {
            return 0;
        }
        return (int) Math.round((uppercase * 100.0) / letters);
    }
}
