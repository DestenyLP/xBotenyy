package de.destenylp.xBotenyy.common.automod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutomodSettings {
    private final boolean enabled;
    private final Set<String> exemptRoleIds;
    private final Set<String> exemptChannelIds;
    private final boolean bypassManageServer;
    private final boolean bypassAdministrator;
    private final String logChannelId;
    private final WordFilterConfig wordFilter;
    private final InviteFilterConfig inviteFilter;
    private final MentionFilterConfig mentionFilter;
    private final CapsFilterConfig capsFilter;
    private final SpamFilterConfig spamFilter;
    private final DuplicateFilterConfig duplicateFilter;
    private final LinkFilterConfig linkFilter;
    private final AiFilterConfig aiFilter;
    private final StrikeConfig strikeConfig;

    public AutomodSettings(boolean enabled, Set<String> exemptRoleIds, Set<String> exemptChannelIds,
                            boolean bypassManageServer, boolean bypassAdministrator, String logChannelId,
                            WordFilterConfig wordFilter,
                            InviteFilterConfig inviteFilter, MentionFilterConfig mentionFilter,
                            CapsFilterConfig capsFilter, SpamFilterConfig spamFilter,
                            DuplicateFilterConfig duplicateFilter, LinkFilterConfig linkFilter,
                            AiFilterConfig aiFilter, StrikeConfig strikeConfig) {
        this.enabled = enabled;
        this.exemptRoleIds = Set.copyOf(exemptRoleIds);
        this.exemptChannelIds = Set.copyOf(exemptChannelIds);
        this.bypassManageServer = bypassManageServer;
        this.bypassAdministrator = bypassAdministrator;
        this.logChannelId = logChannelId;
        this.wordFilter = wordFilter;
        this.inviteFilter = inviteFilter;
        this.mentionFilter = mentionFilter;
        this.capsFilter = capsFilter;
        this.spamFilter = spamFilter;
        this.duplicateFilter = duplicateFilter;
        this.linkFilter = linkFilter;
        this.aiFilter = aiFilter;
        this.strikeConfig = strikeConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getExemptRoleIds() {
        return exemptRoleIds;
    }

    public Set<String> getExemptChannelIds() {
        return exemptChannelIds;
    }

    public boolean isBypassManageServer() {
        return bypassManageServer;
    }

    public boolean isBypassAdministrator() {
        return bypassAdministrator;
    }

    public String getLogChannelId() {
        return logChannelId;
    }

    public WordFilterConfig getWordFilter() {
        return wordFilter;
    }

    public InviteFilterConfig getInviteFilter() {
        return inviteFilter;
    }

    public MentionFilterConfig getMentionFilter() {
        return mentionFilter;
    }

    public CapsFilterConfig getCapsFilter() {
        return capsFilter;
    }

    public SpamFilterConfig getSpamFilter() {
        return spamFilter;
    }

    public DuplicateFilterConfig getDuplicateFilter() {
        return duplicateFilter;
    }

    public LinkFilterConfig getLinkFilter() {
        return linkFilter;
    }

    public AiFilterConfig getAiFilter() {
        return aiFilter;
    }

    public StrikeConfig getStrikeConfig() {
        return strikeConfig;
    }

    public static final class WordFilterConfig {
        private final boolean enabled;
        private final AutomodAction action;
        private final Set<String> compactWords;
        private final Pattern spacedPattern;

        public WordFilterConfig(boolean enabled, AutomodAction action, Set<String> rawWords) {
            this.enabled = enabled;
            this.action = action;

            Set<String> compact = new HashSet<>();
            List<String> patternParts = new ArrayList<>();
            for (String raw : rawWords) {
                String spaced = AutomodTextNormalizer.normalizeSpaced(raw);
                if (spaced.isBlank()) {
                    continue;
                }
                patternParts.add(Pattern.quote(spaced));
                String compactWord = AutomodTextNormalizer.normalizeCompact(raw);
                if (compactWord.length() >= 5) {
                    compact.add(compactWord);
                }
            }
            this.compactWords = Set.copyOf(compact);
            this.spacedPattern = patternParts.isEmpty() ? null
                    : Pattern.compile("\\b(" + String.join("|", patternParts) + ")\\b");
        }

        public boolean isEnabled() {
            return enabled;
        }

        public AutomodAction getAction() {
            return action;
        }

        public Optional<String> findMatch(String rawContent) {
            if (!enabled || rawContent == null || rawContent.isBlank()) {
                return Optional.empty();
            }
            if (spacedPattern != null) {
                Matcher matcher = spacedPattern.matcher(AutomodTextNormalizer.normalizeSpaced(rawContent));
                if (matcher.find()) {
                    return Optional.of(matcher.group(1));
                }
            }
            String compactContent = AutomodTextNormalizer.normalizeCompact(rawContent);
            for (String word : compactWords) {
                if (compactContent.contains(word)) {
                    return Optional.of(word);
                }
            }
            return Optional.empty();
        }
    }

    public record InviteFilterConfig(boolean enabled, Set<String> whitelistCodes, AutomodAction action) {
    }

    public record MentionFilterConfig(boolean enabled, int maxMentions, AutomodAction action) {
    }

    public record CapsFilterConfig(boolean enabled, int minLength, int maxPercentage, AutomodAction action) {
    }

    public record SpamFilterConfig(boolean enabled, int maxMessages, int windowSeconds, AutomodAction action) {
    }

    public record DuplicateFilterConfig(boolean enabled, int maxRepeats, AutomodAction action) {
    }

    public record LinkFilterConfig(boolean enabled, Set<String> whitelistDomains, AutomodAction action) {
    }

    public record AiFilterConfig(boolean enabled, double threshold, int timeoutSeconds, AutomodAction action) {
    }

    public record StrikeConfig(boolean enabled, int expiryMinutes, int timeoutThreshold, int timeoutDurationMinutes,
                                int kickThreshold, int banThreshold) {
    }
}
