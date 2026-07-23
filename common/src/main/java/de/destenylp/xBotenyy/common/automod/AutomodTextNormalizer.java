package de.destenylp.xBotenyy.common.automod;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class AutomodTextNormalizer {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200F\\uFEFF\\u2060]");
    private static final Pattern NON_LETTER_RUN = Pattern.compile("[^a-z0-9]+");
    private static final Pattern NON_LETTER = Pattern.compile("[^a-z0-9]");

    private AutomodTextNormalizer() {
    }

    public static String normalizeSpaced(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = baseClean(input);
        return NON_LETTER_RUN.matcher(cleaned).replaceAll(" ").trim();
    }

    public static String normalizeCompact(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = baseClean(input);
        return NON_LETTER.matcher(cleaned).replaceAll("");
    }

    private static String baseClean(String input) {
        String withoutZeroWidth = ZERO_WIDTH.matcher(input).replaceAll("");
        String decomposed = Normalizer.normalize(withoutZeroWidth, Normalizer.Form.NFKD);
        String withoutDiacritics = DIACRITICS.matcher(decomposed).replaceAll("");
        return applyLeetSubstitutions(withoutDiacritics.toLowerCase());
    }

    private static String applyLeetSubstitutions(String input) {
        StringBuilder builder = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            builder.append(switch (c) {
                case '0' -> 'o';
                case '1', '!', '|' -> 'i';
                case '3' -> 'e';
                case '4', '@' -> 'a';
                case '5', '$' -> 's';
                case '7', '+' -> 't';
                case '8' -> 'b';
                default -> c;
            });
        }
        return builder.toString();
    }
}
