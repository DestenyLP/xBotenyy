package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.eventlog.EventLogEmbedFactory;
import de.destenylp.xBotenyy.discordbot.eventlog.EventLogService;
import de.destenylp.xBotenyy.discordbot.eventlog.LogEventType;
import de.destenylp.xBotenyy.discordbot.eventlog.RecentMessageCache;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class EventLogListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventLogListener.class);

    private final EventLogService service;
    private final RecentMessageCache messageCache;
    private final int messageDeleteContentMaxLength;

    public EventLogListener(EventLogService service, int messageCacheMaxSize, int messageDeleteContentMaxLength) {
        this.service = service;
        this.messageCache = new RecentMessageCache(messageCacheMaxSize);
        this.messageDeleteContentMaxLength = messageDeleteContentMaxLength;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.MEMBER_JOIN, event.getJDA(),
                () -> EventLogEmbedFactory.buildMemberJoin(event.getMember()));
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.MEMBER_LEAVE, event.getJDA(),
                () -> EventLogEmbedFactory.buildMemberLeave(event.getUser(), event.getMember(), event.getGuild().getMemberCount()));
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.MEMBER_BAN, event.getJDA(),
                () -> EventLogEmbedFactory.buildBan(event.getUser()));
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.MEMBER_UNBAN, event.getJDA(),
                () -> EventLogEmbedFactory.buildUnban(event.getUser()));
    }

    @Override
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {
        boolean started = event.getOldTimeBoosted() == null && event.getNewTimeBoosted() != null;
        boolean stopped = event.getOldTimeBoosted() != null && event.getNewTimeBoosted() == null;
        if (started) {
            dispatch(event.getGuild().getId(), LogEventType.MEMBER_BOOST, event.getJDA(),
                    () -> EventLogEmbedFactory.buildBoost(event.getMember()));
        } else if (stopped) {
            dispatch(event.getGuild().getId(), LogEventType.MEMBER_UNBOOST, event.getJDA(),
                    () -> EventLogEmbedFactory.buildUnboost(event.getMember()));
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.NICKNAME_CHANGE, event.getJDA(),
                () -> EventLogEmbedFactory.buildNicknameChange(event.getMember(), event.getOldNickname(), event.getNewNickname()));
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.ROLE_UPDATE, event.getJDA(),
                () -> EventLogEmbedFactory.buildRoleUpdate(event.getMember(), event.getRoles(), true));
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.ROLE_UPDATE, event.getJDA(),
                () -> EventLogEmbedFactory.buildRoleUpdate(event.getMember(), event.getRoles(), false));
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        dispatch(event.getGuild().getId(), LogEventType.VOICE_UPDATE, event.getJDA(),
                () -> EventLogEmbedFactory.buildVoiceUpdate(event.getEntity(), event.getChannelLeft(), event.getChannelJoined()));
    }

    @Override
    public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
        if (event.getNewTimeOutEnd() != null) {
            dispatch(event.getGuild().getId(), LogEventType.MEMBER_TIMEOUT, event.getJDA(),
                    () -> EventLogEmbedFactory.buildTimeout(event.getMember(), event.getNewTimeOutEnd()));
        } else if (event.getOldTimeOutEnd() != null) {
            dispatch(event.getGuild().getId(), LogEventType.MEMBER_TIMEOUT_REMOVED, event.getJDA(),
                    () -> EventLogEmbedFactory.buildTimeoutRemoved(event.getMember()));
        }
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.isFromGuild() || !(event.getChannel() instanceof GuildChannel guildChannel)) {
            return;
        }
        dispatch(event.getGuild().getId(), LogEventType.CHANNEL_CREATE, event.getJDA(),
                () -> EventLogEmbedFactory.buildChannelCreate(guildChannel));
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.isFromGuild() || !(event.getChannel() instanceof GuildChannel guildChannel)) {
            return;
        }
        dispatch(event.getGuild().getId(), LogEventType.CHANNEL_DELETE, event.getJDA(),
                () -> EventLogEmbedFactory.buildChannelDelete(guildChannel));
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        cacheMessage(event.getMessageIdLong(), event.getAuthor().getIdLong(), event.getAuthor().getAsTag(),
                event.getAuthor().getEffectiveAvatarUrl(), event.getMessage().getContentRaw());
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        cacheMessage(event.getMessageIdLong(), event.getAuthor().getIdLong(), event.getAuthor().getAsTag(),
                event.getAuthor().getEffectiveAvatarUrl(), event.getMessage().getContentRaw());
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        RecentMessageCache.CachedMessage cached = messageCache.remove(event.getMessageIdLong());
        dispatch(event.getGuild().getId(), LogEventType.MESSAGE_DELETE, event.getJDA(),
                () -> EventLogEmbedFactory.buildMessageDelete(event.getChannel().getAsMention(), event.getMessageId(),
                        cached, messageDeleteContentMaxLength));
    }

    private void cacheMessage(long messageId, long authorId, String authorTag, String authorAvatarUrl, String content) {
        messageCache.put(messageId, new RecentMessageCache.CachedMessage(authorId, authorTag, authorAvatarUrl, content));
    }

    private void dispatch(String guildId, LogEventType type, JDA jda, EmbedSupplier supplier) {
        try {
            Optional<String> channelIdOpt = service.resolveChannelId(guildId, type);
            if (channelIdOpt.isEmpty()) {
                return;
            }
            TextChannel channel = jda.getChannelById(TextChannel.class, channelIdOpt.get());
            if (channel == null) {
                LOGGER.warn("Log-Kanal {} für {} in Guild {} nicht gefunden", channelIdOpt.get(), type, guildId);
                return;
            }
            channel.sendMessageEmbeds(supplier.get())
                    .queue(success -> BotMetrics.incrementEventLogsSent(),
                            failure -> LOGGER.warn("Log-Nachricht ({}) konnte nicht gesendet werden: {}", type, failure.getMessage()));
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler beim Event-Logging ({}) für Guild {}: ", type, guildId, e);
        }
    }

    @FunctionalInterface
    private interface EmbedSupplier {
        MessageEmbed get();
    }
}
