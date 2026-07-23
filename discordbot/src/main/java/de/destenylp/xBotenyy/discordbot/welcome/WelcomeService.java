package de.destenylp.xBotenyy.discordbot.welcome;

import de.destenylp.xBotenyy.discordbot.core.GuildService;

import java.util.List;
import java.util.Optional;

public class WelcomeService implements GuildService {
    private final WelcomeRepository manager;

    public WelcomeService(WelcomeRepository manager) {
        this.manager = manager;
    }

    @Override
    public String getServiceName() {
        return "Welcome";
    }

    public Optional<WelcomeSettings> getSettings(String guildId) {
        return manager.getSettings(guildId);
    }

    public void updateChannel(String guildId, String channelId) {
        manager.updateChannel(guildId, channelId);
    }

    public void updateEnabled(String guildId, boolean enabled) {
        manager.updateEnabled(guildId, enabled);
    }

    public void updateDm(String guildId, boolean dmEnabled) {
        manager.updateDm(guildId, dmEnabled);
    }

    public WelcomeVariant addVariant(String guildId, WelcomeVariant draft) {
        return manager.addVariant(guildId, draft);
    }

    public Optional<WelcomeVariant> getVariant(String guildId, String id) {
        return manager.getVariant(guildId, id);
    }

    public boolean editVariant(String guildId, String id, WelcomeVariantEdit edit) {
        Optional<WelcomeVariant> variantOpt = manager.getVariant(guildId, id);
        if (variantOpt.isEmpty()) {
            return false;
        }
        WelcomeVariant variant = variantOpt.get();
        edit.applyTo(variant);
        manager.save(guildId, variant);
        return true;
    }

    public boolean removeVariant(String guildId, String id) {
        return manager.removeVariant(guildId, id);
    }

    public List<WelcomeVariant> getVariants(String guildId) {
        return manager.getVariants(guildId);
    }

    public Optional<WelcomeVariant> getRandomVariant(String guildId) {
        return manager.getRandomVariant(guildId);
    }

    public static boolean isClearValue(String value) {
        return value != null && (value.equalsIgnoreCase("none") || value.equals("-"));
    }
}
