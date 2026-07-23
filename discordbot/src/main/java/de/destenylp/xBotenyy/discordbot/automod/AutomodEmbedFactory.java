package de.destenylp.xBotenyy.discordbot.automod;

import de.destenylp.xBotenyy.common.automod.AutomodAction;
import de.destenylp.xBotenyy.common.automod.AutomodVerdict;
import de.destenylp.xBotenyy.discordbot.core.AbstractEmbedFactory;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public final class AutomodEmbedFactory extends AbstractEmbedFactory {
    private AutomodEmbedFactory() {
    }

    public static MessageEmbed buildLogEmbed(Member member, MessageChannel channel, AutomodVerdict verdict,
                                              AutomodAction finalAction, int strikeCount, String messageContent) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("🛡️ AutoMod: " + verdict.ruleType().getLabel());
        eb.addField("Mitglied", "<@" + member.getId() + "> (`" + member.getId() + "`)", true);
        eb.addField("Kanal", "<#" + channel.getId() + ">", true);
        eb.addField("Strikes", String.valueOf(strikeCount), true);
        eb.addField("Grund", verdict.reason(), false);
        eb.addField("Ausgeführte Aktion", finalAction.getLabel(), false);
        if (messageContent != null && !messageContent.isBlank()) {
            String preview = messageContent.length() > 500 ? messageContent.substring(0, 500) + "…" : messageContent;
            eb.addField("Nachricht", preview, false);
        }
        timestampNow(eb);
        return eb.build();
    }
}
