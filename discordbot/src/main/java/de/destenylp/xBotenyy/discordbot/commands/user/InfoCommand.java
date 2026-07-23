package de.destenylp.xBotenyy.discordbot.commands.user;

import de.destenylp.xBotenyy.discordbot.Bot;
import de.destenylp.xBotenyy.discordbot.commands.DiscordCommand;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class InfoCommand implements DiscordCommand {
    private final Bot bot;

    public InfoCommand(Bot bot) {
        this.bot = bot;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("info", "Bots Information");
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Bot Information");
        eb.setDescription("Alle wichtigen Informationen bezüglich des Bottes.");
        eb.setColor(DiscordColors.brand());
        eb.addField("Author", "Desteny (xDestenyy)", false);
        eb.addField("Language", "Java", true);
        eb.addField("Library", "JDA", true);
        eb.addField("Version", bot.getVersion(), true);
        eb.setFooter("Erstellt für den Community Discord!");

        MessageEmbed embed = eb.build();
        event.replyEmbeds(embed).setEphemeral(true).queue();
    }
}
