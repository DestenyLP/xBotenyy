package de.destenylp.xBotenyy.discordbot.core;

import de.destenylp.xBotenyy.discordbot.commands.DiscordCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public abstract class AbstractGuildCommand implements DiscordCommand {
    @Override
    public final void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            replyGuildOnly(event);
            return;
        }

        if (!hasSubcommands()) {
            executeInGuild(event, guild, null);
            return;
        }

        String subcommand = event.getSubcommandName();
        if (subcommand == null) {
            replyUnknownCommand(event);
            return;
        }

        executeInGuild(event, guild, subcommand);
    }

    protected boolean hasSubcommands() {
        return true;
    }

    protected abstract void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand);

    protected void replyGuildOnly(SlashCommandInteractionEvent event) {
        event.reply("Dieser Befehl kann nur auf einem Server genutzt werden.").setEphemeral(true).queue();
    }

    protected void replyUnknownCommand(SlashCommandInteractionEvent event) {
        event.reply("Unbekannter Befehl.").setEphemeral(true).queue();
    }

    protected void replyUnknownSubcommand(SlashCommandInteractionEvent event) {
        event.reply("Unbekannter Unterbefehl.").setEphemeral(true).queue();
    }
}
