package de.destenylp.xBotenyy.common.moderation.bridge;

public record BridgeSettings(boolean enabled, String bindHost, int port, String token, String peerUrl) {
    public boolean isServerEnabled() {
        return enabled && token != null && !token.isBlank();
    }

    public boolean isPeerConfigured() {
        return enabled && peerUrl != null && !peerUrl.isBlank() && token != null && !token.isBlank();
    }
}
