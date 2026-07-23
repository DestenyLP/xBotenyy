package de.destenylp.xBotenyy.discordbot.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ImageUrlValidator {
    private static final List<String> ALLOWED_SCHEMES = List.of("http", "https");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    private ImageUrlValidator() {
    }

    public static boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return true;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            return false;
        }

        if (uri.getHost() == null) {
            return false;
        }

        String path = uri.getPath() != null ? uri.getPath().toLowerCase(Locale.ROOT) : "";
        return ALLOWED_EXTENSIONS.stream().anyMatch(path::endsWith);
    }
}
