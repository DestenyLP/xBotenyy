package de.destenylp.xBotenyy.discordbot.listeners;
import de.destenylp.xBotenyy.discordbot.commands.DiscordCommandManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class CommandListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandListener.class);
    private final DiscordCommandManager commandManager;
    public CommandListener(DiscordCommandManager commandManager) {
        this.commandManager = commandManager;
    }
    @Override
    public void onReady(ReadyEvent event) {
        LOGGER.info("JDA is ready! Logged in as {}#{}",
                event.getJDA().getSelfUser().getName(), event.getJDA().getSelfUser().getId());
    }
    @Override
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        LOGGER.warn("Discord connection lost (code: {}, reason: {}, closed by server: {})",
                event.getCloseCode() != null ? event.getCloseCode().getCode() : "unknown",
                event.getCloseCode() != null ? event.getCloseCode().getMeaning() : "unknown",
                event.isClosedByServer());
    }
    @Override
    public void onSessionResume(SessionResumeEvent event) {
        LOGGER.info("Discord connection restored.");
    }
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        commandManager.handle(event);
    }
}
