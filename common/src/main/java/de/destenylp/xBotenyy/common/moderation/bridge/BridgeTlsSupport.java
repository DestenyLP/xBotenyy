package de.destenylp.xBotenyy.common.moderation.bridge;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
public final class BridgeTlsSupport {
    public static final String[] ALLOWED_PROTOCOLS = {"TLSv1.3", "TLSv1.2"};
    private BridgeTlsSupport() {
    }
    public static SSLContext buildServerContext(BridgeSettings settings) throws GeneralSecurityException, IOException {
        if (settings.keystorePath() == null || settings.keystorePath().isBlank()) {
            throw new IllegalStateException(
                    "bridge.tls.enabled=true, aber bridge.tls.keystore.path ist nicht gesetzt.");
        }
        KeyManager[] keyManagers = loadKeyManagers(settings.keystorePath(), settings.keystorePassword(),
                settings.keyPassword());
        TrustManager[] trustManagers = settings.mutualTlsEnabled()
                ? loadTrustManagers(settings.truststorePath(), settings.truststorePassword())
                : null;
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        context.init(keyManagers, trustManagers, new SecureRandom());
        return context;
    }
    public static SSLContext buildClientContext(BridgeSettings settings) throws GeneralSecurityException, IOException {
        KeyManager[] keyManagers = settings.mutualTlsEnabled()
                ? loadKeyManagers(settings.keystorePath(), settings.keystorePassword(), settings.keyPassword())
                : null;
        boolean hasCustomTruststore = settings.truststorePath() != null && !settings.truststorePath().isBlank();
        TrustManager[] trustManagers = hasCustomTruststore
                ? loadTrustManagers(settings.truststorePath(), settings.truststorePassword())
                : null;
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        context.init(keyManagers, trustManagers, new SecureRandom());
        return context;
    }
    public static SSLParameters hardenedParameters(SSLContext context) {
        SSLParameters params = context.getDefaultSSLParameters();
        params.setProtocols(ALLOWED_PROTOCOLS);
        return params;
    }
    private static KeyManager[] loadKeyManagers(String path, String storePassword, String keyPassword)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] storePass = toCharArray(storePassword);
        try (InputStream in = openStrict(path)) {
            keyStore.load(in, storePass);
        }
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] keyPass = (keyPassword != null && !keyPassword.isBlank()) ? keyPassword.toCharArray() : storePass;
        factory.init(keyStore, keyPass);
        return factory.getKeyManagers();
    }
    private static TrustManager[] loadTrustManagers(String path, String password)
            throws GeneralSecurityException, IOException {
        if (path == null || path.isBlank()) {
            throw new IllegalStateException(
                    "bridge.tls.mutual-auth=true, aber bridge.tls.truststore.path ist nicht gesetzt.");
        }
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        char[] pass = toCharArray(password);
        try (InputStream in = openStrict(path)) {
            trustStore.load(in, pass);
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory.getTrustManagers();
    }
    private static InputStream openStrict(String path) throws IOException {
        Path resolved = Path.of(path);
        if (!Files.exists(resolved)) {
            throw new IOException("TLS-Store nicht gefunden: " + resolved.toAbsolutePath());
        }
        return new FileInputStream(resolved.toFile());
    }
    private static char[] toCharArray(String value) {
        return value != null ? value.toCharArray() : new char[0];
    }
}
