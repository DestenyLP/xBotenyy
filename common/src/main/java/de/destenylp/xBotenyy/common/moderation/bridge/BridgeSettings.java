package de.destenylp.xBotenyy.common.moderation.bridge;

/**
 * Konfiguration der Moderation-Bridge zwischen Discord- und Twitch-Bot.
 * <p>
 * Wenn {@code tlsEnabled} gesetzt ist, kommuniziert die Bridge ausschliesslich ueber HTTPS
 * (TLS 1.2/1.3). Der Server praesentiert dabei das Zertifikat aus {@code keystorePath}. Ist
 * zusaetzlich {@code mutualTlsEnabled} gesetzt, verlangt der Server ein gueltiges Client-Zertifikat
 * (mTLS) und der Client praesentiert seinerseits das eigene Zertifikat aus dem Keystore. Die
 * Vertrauensbasis fuer eingehende/ausgehende Zertifikate wird ueber {@code truststorePath}
 * festgelegt; bleibt dieser leer, nutzt der Client den Standard-Trust-Store der JVM (oeffentliche
 * CAs) - das ist bei einem selbstsignierten Zertifikat NICHT ausreichend und muss dann gesetzt sein.
 */
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

    /**
     * Eindeutiger Fingerabdruck der TLS-relevanten Einstellungen. Wird genutzt, um erzeugte
     * SSLContext/HttpClient-Instanzen zwischenzuspeichern und nur bei tatsaechlichen Aenderungen
     * (z. B. nach einem Neuladen der Konfiguration) neu aufzubauen.
     */
    public String tlsFingerprint() {
        return tlsEnabled + "|" + keystorePath + "|" + truststorePath + "|" + mutualTlsEnabled;
    }
}
