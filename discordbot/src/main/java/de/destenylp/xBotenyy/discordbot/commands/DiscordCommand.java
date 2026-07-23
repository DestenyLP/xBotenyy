package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.common.commands.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface DiscordCommand extends Command<SlashCommandInteractionEvent> {
    CommandData getCommandData();

    @Override
    default String getName() {
        return getCommandData().getName();
    }

    @Override
    default String getDescription() {
        return ((SlashCommandData) getCommandData()).getDescription();
    }

    @Override
    void execute(SlashCommandInteractionEvent event);
}
