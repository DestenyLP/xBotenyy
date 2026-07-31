package de.destenylp.xBotenyy.common.moderation.bridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class ModerationBridgeServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationBridgeServer.class);

    private final BridgeSettings settings;
    private final ModerationBridgeHandler handler;
    private HttpServer server;

    public ModerationBridgeServer(BridgeSettings settings, ModerationBridgeHandler handler) {
        this.settings = settings;
        this.handler = handler;
    }

    public void start() {
        try {
            if (settings.tlsEnabled()) {
                server = createHttpsServer();
                LOGGER.info("Moderation-Bridge-Server auf {}:{} gestartet (HTTPS, TLS 1.2/1.3{}).",
                        settings.bindHost(), settings.port(), settings.mutualTlsEnabled() ? ", mTLS aktiv" : "");
            } else {
                server = HttpServer.create(new InetSocketAddress(settings.bindHost(), settings.port()), 0);
                LOGGER.warn("Moderation-Bridge-Server auf {}:{} gestartet OHNE TLS (Klartext-HTTP). "
                                + "Nur fuer localhost/vertrauenswuerdige, isolierte Netzwerke geeignet - "
                                + "fuer Verbindungen ueber das Internet 'bridge.tls.enabled=true' setzen!",
                        settings.bindHost(), settings.port());
            }
            server.setExecutor(Executors.newCachedThreadPool(runnable -> {
                Thread thread = new Thread(runnable, "moderation-bridge-server");
                thread.setDaemon(true);
                return thread;
            }));
            server.createContext("/bridge/v1/action", this::handleAction);
            server.createContext("/bridge/v1/link/confirm", this::handleLinkConfirm);
            server.createContext("/bridge/v1/roles/sync", this::handleRoleSync);
            server.start();
        } catch (Exception e) {
            LOGGER.error("Konnte Moderation-Bridge-Server nicht auf Port {} starten: {}", settings.port(),
                    e.getMessage());
        }
    }

    private HttpsServer createHttpsServer() throws Exception {
        SSLContext sslContext = BridgeTlsSupport.buildServerContext(settings);
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(settings.bindHost(), settings.port()), 0);
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters sslParameters = BridgeTlsSupport.hardenedParameters(getSSLContext());
                sslParameters.setNeedClientAuth(settings.mutualTlsEnabled());
                params.setSSLParameters(sslParameters);
                params.setProtocols(BridgeTlsSupport.ALLOWED_PROTOCOLS);
                params.setNeedClientAuth(settings.mutualTlsEnabled());
            }
        });
        return httpsServer;
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleAction(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        try {
            JsonObject body = readJson(exchange);
            BridgeActionRequest request = BridgeActionRequest.fromJson(body);
            BridgeActionResult result = handler.applyAction(request);
            respond(exchange, 200, result.toJson());
        } catch (Exception e) {
            LOGGER.warn("Fehler bei eingehender Bridge-Aktion: {}", e.getMessage());
            respond(exchange, 500, new BridgeActionResult(false, "Interner Fehler").toJson());
        }
    }

    private void handleLinkConfirm(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        try {
            JsonObject body = readJson(exchange);
            BridgeLinkConfirmRequest request = BridgeLinkConfirmRequest.fromJson(body);
            BridgeLinkConfirmResult result = handler.confirmLink(request);
            respond(exchange, 200, result.toJson());
        } catch (Exception e) {
            LOGGER.warn("Fehler bei eingehender Bridge-Link-Bestaetigung: {}", e.getMessage());
            respond(exchange, 500, new BridgeLinkConfirmResult(false, null, null, null, null, "Interner Fehler").toJson());
        }
    }

    private void handleRoleSync(HttpExchange exchange) throws IOException {
        if (!authorize(exchange)) {
            return;
        }
        try {
            JsonObject body = readJson(exchange);
            BridgeRoleSyncRequest request = BridgeRoleSyncRequest.fromJson(body);
            BridgeRoleSyncResult result = handler.syncRoles(request);
            respond(exchange, 200, result.toJson());
        } catch (Exception e) {
            LOGGER.warn("Fehler bei eingehender Rollen-Sync-Anfrage: {}", e.getMessage());
            respond(exchange, 500, new BridgeRoleSyncResult(false, "Interner Fehler").toJson());
        }
    }

    private boolean authorize(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return false;
        }
        if (settings.mutualTlsEnabled() && !hasVerifiedClientCertificate(exchange)) {
            LOGGER.warn("Bridge-Anfrage ohne gueltiges Client-Zertifikat abgelehnt (mTLS erforderlich).");
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return false;
        }
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String expected = "Bearer " + settings.token();
        boolean valid = header != null
                && java.security.MessageDigest.isEqual(header.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
            return false;
        }
        return true;
    }

    private boolean hasVerifiedClientCertificate(HttpExchange exchange) {
        if (!(exchange instanceof HttpsExchange httpsExchange)) {
            return false;
        }
        try {
            return httpsExchange.getSSLSession().getPeerCertificates().length > 0;
        } catch (SSLPeerUnverifiedException e) {
            return false;
        }
    }

    private JsonObject readJson(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonParser.parseString(body).getAsJsonObject();
        }
    }

    private void respond(HttpExchange exchange, int status, JsonObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
