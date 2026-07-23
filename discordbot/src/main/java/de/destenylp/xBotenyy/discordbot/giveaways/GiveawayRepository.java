package de.destenylp.xBotenyy.discordbot.giveaways;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GiveawayRepository {
    Giveaway createGiveaway(Giveaway draft);
    void save(Giveaway giveaway);
    void persistParticipant(String guildId, String giveawayId, String memberId, boolean joined);
    void attachMessage(String guildId, String giveawayId, String channelId, String messageId);
    Optional<Giveaway> getGiveaway(String guildId, String id);
    Optional<Giveaway> getGiveawayByMessage(String guildId, String messageId);
    List<Giveaway> getGiveaways(String guildId);
    List<Giveaway> getRunningGiveaways(String guildId);
    Map<String, List<Giveaway>> getAllRunningGiveawaysByGuild();
    int pruneFinishedGiveaways(Duration retention);
}
