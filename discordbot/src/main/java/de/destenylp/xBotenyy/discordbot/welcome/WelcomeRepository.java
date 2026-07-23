package de.destenylp.xBotenyy.discordbot.welcome;

import java.util.List;
import java.util.Optional;

public interface WelcomeRepository {
    Optional<WelcomeSettings> getSettings(String guildId);
    void updateChannel(String guildId, String channelId);
    void updateEnabled(String guildId, boolean enabled);
    void updateDm(String guildId, boolean dmEnabled);
    WelcomeVariant addVariant(String guildId, WelcomeVariant draft);
    void save(String guildId, WelcomeVariant variant);
    Optional<WelcomeVariant> getVariant(String guildId, String id);
    boolean removeVariant(String guildId, String id);
    List<WelcomeVariant> getVariants(String guildId);
    Optional<WelcomeVariant> getRandomVariant(String guildId);
}
