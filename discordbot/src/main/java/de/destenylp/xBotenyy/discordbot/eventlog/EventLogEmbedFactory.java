package de.destenylp.xBotenyy.discordbot.eventlog;

import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.time.OffsetDateTime;
import java.util.List;

public final class EventLogEmbedFactory extends AbstractEmbedFactory {
    private EventLogEmbedFactory() {
    }

    private static EmbedBuilder base(LogEventType type) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(type.getColor());
        timestampNow(eb);
        return eb;
    }

    public static MessageEmbed buildMemberJoin(Member member) {
        EmbedBuilder eb = base(LogEventType.MEMBER_JOIN);
        eb.setTitle(LogEventType.MEMBER_JOIN.getEmoji() + " Mitglied beigetreten");
        eb.setDescription(member.getAsMention() + " (" + member.getUser().getAsTag() + ") ist dem Server beigetreten.");
        eb.addField("Konto erstellt", "<t:" + member.getUser().getTimeCreated().toEpochSecond() + ":R>", true);
        eb.addField("Mitgliederzahl", String.valueOf(member.getGuild().getMemberCount()), true);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildMemberLeave(User user, Member member, int memberCount) {
        EmbedBuilder eb = base(LogEventType.MEMBER_LEAVE);
        eb.setTitle(LogEventType.MEMBER_LEAVE.getEmoji() + " Mitglied verlassen");
        eb.setDescription(user.getAsTag() + " hat den Server verlassen.");
        if (member != null && member.getTimeJoined() != null) {
            eb.addField("Beigetreten", "<t:" + member.getTimeJoined().toEpochSecond() + ":R>", true);
        }
        eb.addField("Mitgliederzahl", String.valueOf(memberCount), true);
        appendUserFooter(eb, user.getId(), user.getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildBoost(Member member) {
        EmbedBuilder eb = base(LogEventType.MEMBER_BOOST);
        eb.setTitle(LogEventType.MEMBER_BOOST.getEmoji() + " Server geboostet");
        eb.setDescription(member.getAsMention() + " boostet jetzt den Server! Vielen Dank für die Unterstützung.");
        eb.addField("Boosts gesamt", String.valueOf(member.getGuild().getBoostCount()), true);
        eb.addField("Boost-Level", String.valueOf(member.getGuild().getBoostTier().getKey()), true);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildUnboost(Member member) {
        EmbedBuilder eb = base(LogEventType.MEMBER_UNBOOST);
        eb.setTitle(LogEventType.MEMBER_UNBOOST.getEmoji() + " Boost entfernt");
        eb.setDescription(member.getAsMention() + " boostet den Server nicht mehr.");
        eb.addField("Boosts gesamt", String.valueOf(member.getGuild().getBoostCount()), true);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildBan(User user) {
        EmbedBuilder eb = base(LogEventType.MEMBER_BAN);
        eb.setTitle(LogEventType.MEMBER_BAN.getEmoji() + " Mitglied gebannt");
        eb.setDescription(user.getAsTag() + " wurde vom Server gebannt.");
        appendUserFooter(eb, user.getId(), user.getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildUnban(User user) {
        EmbedBuilder eb = base(LogEventType.MEMBER_UNBAN);
        eb.setTitle(LogEventType.MEMBER_UNBAN.getEmoji() + " Bann aufgehoben");
        eb.setDescription("Der Bann von " + user.getAsTag() + " wurde aufgehoben.");
        appendUserFooter(eb, user.getId(), user.getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildNicknameChange(Member member, String oldNickname, String newNickname) {
        EmbedBuilder eb = base(LogEventType.NICKNAME_CHANGE);
        eb.setTitle(LogEventType.NICKNAME_CHANGE.getEmoji() + " Nickname geändert");
        eb.setDescription(member.getAsMention() + " hat den Nickname geändert.");
        eb.addField("Vorher", oldNickname != null ? oldNickname : "*(kein Nickname)*", true);
        eb.addField("Nachher", newNickname != null ? newNickname : "*(kein Nickname)*", true);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildRoleUpdate(Member member, List<Role> roles, boolean added) {
        EmbedBuilder eb = base(LogEventType.ROLE_UPDATE);
        eb.setTitle(LogEventType.ROLE_UPDATE.getEmoji() + " Rollen aktualisiert");
        eb.setDescription(member.getAsMention() + (added ? " hat neue Rollen erhalten." : " wurden Rollen entzogen."));
        String roleList = roles.stream().map(Role::getAsMention).reduce((a, b) -> a + ", " + b).orElse("-");
        eb.addField(added ? "Hinzugefügt" : "Entfernt", roleList, false);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildVoiceUpdate(Member member, AudioChannel left, AudioChannel joined) {
        EmbedBuilder eb = base(LogEventType.VOICE_UPDATE);
        String description;
        if (left != null && joined != null) {
            eb.setTitle(LogEventType.VOICE_UPDATE.getEmoji() + " Voice-Channel gewechselt");
            description = member.getAsMention() + " ist von " + left.getAsMention() + " zu " + joined.getAsMention() + " gewechselt.";
        } else if (joined != null) {
            eb.setTitle(LogEventType.VOICE_UPDATE.getEmoji() + " Voice-Channel betreten");
            description = member.getAsMention() + " hat " + joined.getAsMention() + " betreten.";
        } else {
            eb.setTitle(LogEventType.VOICE_UPDATE.getEmoji() + " Voice-Channel verlassen");
            description = member.getAsMention() + " hat " + (left != null ? left.getAsMention() : "einen Voice-Channel") + " verlassen.";
        }
        eb.setDescription(description);
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildTimeout(Member member, OffsetDateTime timeoutEnd) {
        EmbedBuilder eb = base(LogEventType.MEMBER_TIMEOUT);
        eb.setTitle(LogEventType.MEMBER_TIMEOUT.getEmoji() + " Mitglied Timeout");
        eb.setDescription(member.getAsMention() + " wurde stummgeschaltet (Timeout).");
        if (timeoutEnd != null) {
            eb.addField("Endet", "<t:" + timeoutEnd.toEpochSecond() + ":R>", true);
        }
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildTimeoutRemoved(Member member) {
        EmbedBuilder eb = base(LogEventType.MEMBER_TIMEOUT_REMOVED);
        eb.setTitle(LogEventType.MEMBER_TIMEOUT_REMOVED.getEmoji() + " Timeout aufgehoben");
        eb.setDescription("Der Timeout von " + member.getAsMention() + " wurde aufgehoben oder ist abgelaufen.");
        appendUserFooter(eb, member.getUser().getId(), member.getUser().getEffectiveAvatarUrl());
        return eb.build();
    }

    public static MessageEmbed buildChannelCreate(GuildChannel channel) {
        EmbedBuilder eb = base(LogEventType.CHANNEL_CREATE);
        eb.setTitle(LogEventType.CHANNEL_CREATE.getEmoji() + " Channel erstellt");
        eb.setDescription(describeChannel(channel) + " wurde erstellt.");
        eb.setFooter("Channel-ID: " + channel.getId());
        return eb.build();
    }

    public static MessageEmbed buildChannelDelete(GuildChannel channel) {
        EmbedBuilder eb = base(LogEventType.CHANNEL_DELETE);
        eb.setTitle(LogEventType.CHANNEL_DELETE.getEmoji() + " Channel gelöscht");
        eb.setDescription("**#" + channel.getName() + "** (" + channel.getType().name() + ") wurde gelöscht.");
        eb.setFooter("Channel-ID: " + channel.getId());
        return eb.build();
    }

    public static MessageEmbed buildMessageDelete(String channelMention, String messageId, RecentMessageCache.CachedMessage cached, int contentMaxLength) {
        EmbedBuilder eb = base(LogEventType.MESSAGE_DELETE);
        eb.setTitle(LogEventType.MESSAGE_DELETE.getEmoji() + " Nachricht gelöscht");
        String authorInfo = cached != null ? "<@" + cached.authorId() + "> (" + cached.authorTag() + ")" : "Unbekannter Autor";
        eb.setDescription("Eine Nachricht von " + authorInfo + " in " + channelMention + " wurde gelöscht.");
        String content = cached != null ? cached.content() : null;
        if (content != null && !content.isBlank()) {
            eb.addField("Inhalt", content.length() > contentMaxLength ? content.substring(0, contentMaxLength) + "…" : content, false);
        } else {
            eb.addField("Inhalt", "*(nicht verfügbar - Nachricht war nicht im Cache, z. B. weil sie vor dem Bot-Start gesendet wurde)*", false);
        }
        eb.setFooter("Nachricht-ID: " + messageId);
        if (cached != null && cached.authorAvatarUrl() != null) {
            eb.setThumbnail(cached.authorAvatarUrl());
        }
        return eb.build();
    }

    private static String describeChannel(GuildChannel channel) {
        if (channel.getType() == ChannelType.TEXT || channel.getType() == ChannelType.VOICE
                || channel.getType() == ChannelType.STAGE || channel.getType() == ChannelType.FORUM) {
            return "**#" + channel.getName() + "** (" + channel.getType().name() + ")";
        }
        return "**" + channel.getName() + "** (" + channel.getType().name() + ")";
    }
}
