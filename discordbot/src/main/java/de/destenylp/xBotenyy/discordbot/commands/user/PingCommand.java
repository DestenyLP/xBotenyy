package de.destenylp.xBotenyy.discordbot.commands.user;

import de.destenylp.xBotenyy.discordbot.commands.DiscordCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class PingCommand implements DiscordCommand {
    @Override
    public CommandData getCommandData() {
        return Commands.slash("ping", "Bots Latency");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        event.replyFormat("Current Ping: %dms", ping).queue();
    }
}
