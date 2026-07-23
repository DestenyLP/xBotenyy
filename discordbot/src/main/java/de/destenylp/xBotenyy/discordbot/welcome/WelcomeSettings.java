package de.destenylp.xBotenyy.discordbot.welcome;

import java.util.List;
import java.util.function.Supplier;

public class WelcomeSettings {
    private String channelId;
    private boolean enabled;
    private boolean dmEnabled;
    private final Supplier<List<WelcomeVariant>> variantsSupplier;

    public WelcomeSettings() {
        this(List::of);
    }

    WelcomeSettings(Supplier<List<WelcomeVariant>> variantsSupplier) {
        this.variantsSupplier = variantsSupplier;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDmEnabled() {
        return dmEnabled;
    }

    public void setDmEnabled(boolean dmEnabled) {
        this.dmEnabled = dmEnabled;
    }

    public List<WelcomeVariant> getVariants() {
        return variantsSupplier.get();
    }
}
