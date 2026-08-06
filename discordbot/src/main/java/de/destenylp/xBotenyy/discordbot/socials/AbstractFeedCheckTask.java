package de.destenylp.xBotenyy.discordbot.socials;
import de.destenylp.xBotenyy.discordbot.messaging.MessageDispatcher;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
public abstract class AbstractFeedCheckTask<T> implements Runnable {
    protected final JDA jda;
    protected final SocialService service;
    protected AbstractFeedCheckTask(JDA jda, SocialService service) {
        this.jda = jda;
        this.service = service;
    }
    @Override
    public final void run() {
        recordPollAttempt();
        try {
            checkAccounts();
        } catch (Exception e) {
            recordError(e.getMessage());
            logger().error("Error while checking {} accounts: ", platformName(), e);
        }
    }
    private void checkAccounts() {
        Map<String, List<SocialAccount>> byGuild = findAccounts();
        if (byGuild.isEmpty()) {
            return;
        }
        Map<String, List<GuildAccount>> bySource = new HashMap<>();
        byGuild.forEach((guildId, accounts) -> accounts.forEach(account ->
                bySource.computeIfAbsent(sourceIdOf(account), key -> new ArrayList<>())
                        .add(new GuildAccount(guildId, account))));
        bySource.forEach(this::checkSource);
    }
    private void checkSource(String sourceId, List<GuildAccount> guildAccounts) {
        try {
            Optional<T> latest = fetchLatest(sourceId);
            if (latest.isEmpty()) {
                return;
            }
            T item = latest.get();
            for (GuildAccount guildAccount : guildAccounts) {
                handleAccount(guildAccount.guildId(), guildAccount.account(), item);
            }
        } catch (Exception e) {
            recordError(describeSource(sourceId) + ": " + e.getMessage());
            logger().warn("Error while checking {} source {}: {}", platformName(), sourceId, e.getMessage());
        }
    }
    private void handleAccount(String guildId, SocialAccount account, T item) {
        String itemId = idOf(item);
        String lastId = lastIdOf(account);
        if (lastId == null) {
            setLastId(account, itemId);
            service.saveAccount(guildId, account);
            return;
        }
        if (lastId.equals(itemId)) {
            return;
        }
        setLastId(account, itemId);
        service.saveAccount(guildId, account);
        announce(guildId, account, item);
    }
    private void announce(String guildId, SocialAccount account, T item) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            return;
        }
        TextChannel channel = jda.getChannelById(TextChannel.class, account.getChannelId());
        if (channel == null) {
            logger().warn("Announcement channel {} for social account {} in guild {} no longer exists",
                    account.getChannelId(), account.getId(), guildId);
            return;
        }
        RenderedMessage message = buildMessage(account, item, guild);
        MessageDispatcher.prepare(channel, message).ifPresent(action -> action.queue(
                success -> onAnnounced(),
                failure -> logger().warn("Could not send {} announcement for account {}: {}",
                        platformName(), account.getId(), failure.getMessage())));
        logger().info("New {} item for account {} announced in guild {}: {}",
                platformName(), account.getId(), guildId, idOf(item));
    }
    protected abstract Logger logger();
    protected abstract String platformName();
    protected abstract Map<String, List<SocialAccount>> findAccounts();
    protected abstract String sourceIdOf(SocialAccount account);
    protected abstract String describeSource(String sourceId);
    protected abstract Optional<T> fetchLatest(String sourceId);
    protected abstract String idOf(T item);
    protected abstract String lastIdOf(SocialAccount account);
    protected abstract void setLastId(SocialAccount account, String id);
    protected abstract RenderedMessage buildMessage(SocialAccount account, T item, Guild guild);
    protected abstract void recordPollAttempt();
    protected abstract void recordError(String message);
    protected abstract void onAnnounced();
    private record GuildAccount(String guildId, SocialAccount account) {
    }
}
