package de.destenylp.xBotenyy.discordbot.automod;

import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.common.automod.AutomodEngine;
import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.common.automod.ai.GroqSafeguardClient;
import de.destenylp.xBotenyy.discordbot.core.PrunableGuildService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AutomodService implements PrunableGuildService {
    private final AutomodEngine engine;

    public AutomodService(AutomodSettings settings, GroqSafeguardClient moderationClient) {
        this.engine = new AutomodEngine(settings, moderationClient);
    }

    @Override
    public String getServiceName() {
        return engine.getServiceName();
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        return engine.pruneOldEntries(retention);
    }

    public void shutdown() {
        engine.shutdown();
    }

    public AutomodSettings getSettings() {
        return engine.getSettings();
    }

    public boolean isAiAvailable() {
        return engine.isAiAvailable();
    }

    public boolean isExempt(Member member) {
        if (member == null) {
            return true;
        }
        if (getSettings().isBypassAdministrator() && member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        if (getSettings().isBypassManageServer() && member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }
        List<String> roleIds = member.getRoles().stream().map(role -> role.getId()).collect(Collectors.toList());
        return engine.isExempt(roleIds);
    }

    public boolean isChannelExempt(String channelId) {
        return engine.isChannelExempt(channelId);
    }

    public Optional<AutomodVerdict> evaluate(Message message, String guildId, String memberId) {
        String content = message.getContentRaw();
        String activityKey = guildId + ":" + memberId;

        int mentionCount = message.getMentions().getUsers().size() + message.getMentions().getRoles().size();
        if (message.getMentions().mentionsEveryone()) {
            mentionCount++;
        }

        return engine.evaluate(content, activityKey, mentionCount);
    }

    public Optional<AutomodVerdict> evaluateTextOnly(String content) {
        return engine.evaluateTextOnly(content);
    }

    public void evaluateAiAsync(Message message, Consumer<Optional<AutomodVerdict>> callback) {
        engine.evaluateAiAsync(message.getContentRaw(), callback);
    }

    public AutomodAction registerViolationAndEscalate(String guildId, String memberId, AutomodAction baseAction) {
        return engine.registerViolationAndEscalate(guildId, memberId, baseAction);
    }

    public int getCurrentStrikes(String guildId, String memberId) {
        return engine.getCurrentStrikes(guildId, memberId);
    }

    public Duration getTimeoutDuration() {
        return engine.getTimeoutDuration();
    }
}
