package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.giveaways.Giveaway;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayEmbedFactory;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GiveawayListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayListener.class);

    private final GiveawayService service;

    public GiveawayListener(GiveawayService service) {
        this.service = service;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            handleButtonInteraction(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Gewinnspiel-Button {} in Guild {}: ",
                    event.getComponentId(), event.getGuild() != null ? event.getGuild().getId() : "unknown", e);
            if (!event.isAcknowledged()) {
                event.reply("Es ist ein unerwarteter Fehler aufgetreten.").setEphemeral(true).queue();
            }
        }
    }

    private void handleButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("giveaway:enter:")) {
            return;
        }

        String[] parts = componentId.split(":");
        if (parts.length != 3) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        String giveawayId = parts[2];
        Optional<Giveaway> giveawayOpt = service.getGiveaway(guild.getId(), giveawayId);
        if (giveawayOpt.isEmpty()) {
            event.reply("Dieses Gewinnspiel existiert nicht mehr.").setEphemeral(true).queue();
            return;
        }

        Giveaway giveaway = giveawayOpt.get();
        if (!giveaway.isRunning()) {
            event.reply("Dieses Gewinnspiel ist bereits beendet.").setEphemeral(true).queue();
            return;
        }

        if (giveaway.getRequiredRoleId() != null) {
            Role requiredRole = guild.getRoleById(giveaway.getRequiredRoleId());
            boolean hasRole = requiredRole != null && member.getRoles().contains(requiredRole);
            if (!hasRole) {
                event.reply("Du benötigst die Rolle <@&" + giveaway.getRequiredRoleId() + ">, um teilzunehmen.").setEphemeral(true).queue();
                return;
            }
        }

        GiveawayService.ToggleResult result = service.toggleParticipation(giveaway, member.getId());
        String confirmation = result == GiveawayService.ToggleResult.JOINED
                ? "\uD83C\uDF89 Du nimmst jetzt am Gewinnspiel für **" + giveaway.getPrize() + "** teil!"
                : "Du nimmst nicht mehr am Gewinnspiel für **" + giveaway.getPrize() + "** teil.";
        event.reply(confirmation).setEphemeral(true).queue();

        event.getMessage().editMessageEmbeds(GiveawayEmbedFactory.buildAnnouncementEmbed(giveaway))
                .setComponents(GiveawayEmbedFactory.buildEnterComponents(giveaway))
                .queue(success -> { }, failure -> LOGGER.warn("Gewinnspiel-Nachricht {} konnte nicht aktualisiert werden: {}",
                        giveaway.getMessageId(), failure.getMessage()));
    }
}
