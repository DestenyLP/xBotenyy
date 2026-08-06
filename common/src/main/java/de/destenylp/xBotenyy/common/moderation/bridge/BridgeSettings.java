package de.destenylp.xBotenyy.common.moderation.bridge;
public record BridgeSettings(
        boolean enabled,
        String bindHost,
        int port,
        String token,
        String peerUrl,
        boolean tlsEnabled,
        String keystorePath,
        String keystorePassword,
        String keyPassword,
        String truststorePath,
        String truststorePassword,
        boolean mutualTlsEnabled) {
    public boolean isServerEnabled() {
        return enabled && token != null && !token.isBlank();
    }
    public boolean isPeerConfigured() {
        return enabled && peerUrl != null && !peerUrl.isBlank() && token != null && !token.isBlank();
    }
    public boolean isTlsProperlyConfigured() {
        return tlsEnabled && keystorePath != null && !keystorePath.isBlank();
    }
    public String tlsFingerprint() {
        return tlsEnabled + "|" + keystorePath + "|" + truststorePath + "|" + mutualTlsEnabled;
    }
}
