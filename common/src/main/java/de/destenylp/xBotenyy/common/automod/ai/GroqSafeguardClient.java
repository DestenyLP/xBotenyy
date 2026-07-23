package de.destenylp.xBotenyy.common.automod.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.destenylp.xBotenyy.common.core.AbstractHttpApiClient;
import de.destenylp.xBotenyy.common.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public class GroqSafeguardClient extends AbstractHttpApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroqSafeguardClient.class);
    private static final String MODERATION_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-safeguard-20b";

    private static final String POLICY = """
            # Discord Chat Moderation Policy

            ## AUFGABE
            Bewerte den folgenden Chat-Nachrichtentext eines Discord-Servers und entscheide, ob er gegen die Community-Regeln verstoesst. Antworte ausschliesslich mit einem JSON-Objekt, ohne zusaetzlichen Text.

            ## VERSTOSS (1)
            - Gewaltandrohungen oder Verherrlichung von Gewalt
            - Hassrede oder Diskriminierung aufgrund von Herkunft, Religion, Geschlecht, sexueller Orientierung oder Behinderung
            - Sexuelle Inhalte, insbesondere jede Form von sexualisierten Inhalten mit oder ueber Minderjaehrige
            - Aufforderung oder Anleitung zu Selbstverletzung oder Suizid
            - Gezielte Beleidigung, Belaestigung oder Mobbing einzelner Personen
            - Anleitung oder Werbung fuer illegale Aktivitaeten, Betrug oder Scams

            ## KEIN VERSTOSS (0)
            - Normale Unterhaltung, Meinungsaeusserung oder Kritik ohne persoenliche Angriffe
            - Derbe, aber nicht diskriminierende oder bedrohliche Umgangssprache
            - Fiktionale oder thematische Erwaehnung sensibler Themen ohne Verherrlichung oder Anleitung

            ## ANTWORTFORMAT
            Antworte ausschliesslich mit einem JSON-Objekt in exakt dieser Form:
            {"violation": 0 oder 1, "category": "Kurzname der Kategorie oder null", "rationale": "kurze Begruendung"}
            """;

    private final String apiKey;

    public GroqSafeguardClient(String apiKey, Duration requestTimeout) {
        super(requestTimeout);
        this.apiKey = apiKey;
    }

    public Optional<ModerationResult> moderate(String text) {
        if (apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", POLICY);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", text);

            JsonArray messages = new JsonArray();
            messages.add(systemMessage);
            messages.add(userMessage);

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.add("messages", messages);

            HttpRequest request = requestBuilder(URI.create(MODERATION_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOGGER.warn("Groq Content-Moderation API antwortete mit Status {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String content = json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString()
                    .trim();

            return parseVerdict(content);
        } catch (Exception e) {
            LOGGER.warn("Fehler bei der Kommunikation mit der Groq Content-Moderation API: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ModerationResult> parseVerdict(String content) {
        try {
            String jsonPart = extractJsonObject(content);
            JsonObject verdict = JsonParser.parseString(jsonPart).getAsJsonObject();

            boolean flagged = verdict.get("violation").getAsInt() == 1;
            String category = JsonUtil.optString(verdict, "category", "unbekannt");
            String rationale = JsonUtil.optString(verdict, "rationale", "");

            return Optional.of(new ModerationResult(flagged, category, rationale));
        } catch (Exception e) {
            LOGGER.warn("Konnte Antwort der Groq Content-Moderation API nicht auswerten: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start == -1 || end == -1 || end < start) {
            throw new IllegalArgumentException("Kein JSON-Objekt in der Antwort gefunden");
        }
        return content.substring(start, end + 1);
    }

    public record ModerationResult(boolean flagged, String topCategory, String rationale) {
    }
}
