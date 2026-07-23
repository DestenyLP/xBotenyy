package de.destenylp.xBotenyy.discordbot.giveaways;

import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public final class GiveawayEndTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayEndTask.class);

    private final JDA jda;
    private final GiveawayService service;
    private final GiveawayEndCoordinator coordinator;

    public GiveawayEndTask(JDA jda, GiveawayService service, GiveawayEndCoordinator coordinator) {
        this.jda = jda;
        this.service = service;
        this.coordinator = coordinator;
    }

    @Override
    public void run() {
        try {
            endDueGiveaways();
        } catch (Exception e) {
            LOGGER.error("Fehler beim automatischen Beenden fälliger Gewinnspiele: ", e);
        }
    }

    private void endDueGiveaways() {
        Map<String, List<Giveaway>> due = service.findDueGiveaways();
        due.forEach((guildId, giveaways) -> giveaways.forEach(giveaway -> {
            boolean success = service.end(guildId, giveaway.getId());
            if (!success) {
                return;
            }
            Giveaway ended = service.getGiveaway(guildId, giveaway.getId()).orElse(giveaway);
            coordinator.announceEnd(jda, ended);
            LOGGER.info("Giveaway {} in guild {} automatisch beendet", giveaway.getId(), guildId);
        }));
    }
}
