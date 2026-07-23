package de.destenylp.xBotenyy.discordbot.giveaways;

import de.destenylp.xBotenyy.discordbot.core.PrunableGuildService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GiveawayService implements PrunableGuildService {
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([smhdw])$");

    private final GiveawayRepository manager;
    private final int minWinners;
    private final int maxWinners;
    private final Duration minDuration;
    private final Duration maxDuration;

    public GiveawayService(GiveawayRepository manager) {
        this(manager, 1, 20, Duration.ofSeconds(10), Duration.ofDays(60));
    }

    public GiveawayService(GiveawayRepository manager, int minWinners, int maxWinners, Duration minDuration, Duration maxDuration) {
        this.manager = manager;
        this.minWinners = minWinners;
        this.maxWinners = maxWinners;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
    }

    public int getMinWinners() {
        return minWinners;
    }

    public int getMaxWinners() {
        return maxWinners;
    }

    public Duration getMinDuration() {
        return minDuration;
    }

    public Duration getMaxDuration() {
        return maxDuration;
    }

    @Override
    public String getServiceName() {
        return "Gewinnspiele";
    }

    @Override
    public int pruneOldEntries(Duration retention) {
        return pruneFinishedGiveaways(retention);
    }

    public Optional<Duration> parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = DURATION_PATTERN.matcher(raw.trim().toLowerCase());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        long amount = Long.parseLong(matcher.group(1));
        if (amount <= 0) {
            return Optional.empty();
        }
        Duration duration = switch (matcher.group(2)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(amount * 7);
            default -> null;
        };
        return Optional.ofNullable(duration);
    }

    public Giveaway createGiveaway(String guildId, String prize, String description, int winnerCount,
                                    String hostId, String hostName, String requiredRoleId, long endAt) {
        Giveaway draft = new Giveaway(guildId, prize, description, winnerCount, hostId, hostName, requiredRoleId, endAt);
        return manager.createGiveaway(draft);
    }

    public void attachMessage(String guildId, String giveawayId, String channelId, String messageId) {
        manager.attachMessage(guildId, giveawayId, channelId, messageId);
    }

    public Optional<Giveaway> getGiveaway(String guildId, String id) {
        return manager.getGiveaway(guildId, id);
    }

    public Optional<Giveaway> getGiveawayByMessage(String guildId, String messageId) {
        return manager.getGiveawayByMessage(guildId, messageId);
    }

    public List<Giveaway> getGiveaways(String guildId) {
        return manager.getGiveaways(guildId);
    }

    public List<Giveaway> getRunningGiveaways(String guildId) {
        return manager.getRunningGiveaways(guildId);
    }

    public enum ToggleResult {
        JOINED, LEFT
    }

    public ToggleResult toggleParticipation(Giveaway giveaway, String memberId) {
        ToggleResult result;
        if (giveaway.isParticipant(memberId)) {
            giveaway.removeParticipant(memberId);
            result = ToggleResult.LEFT;
        } else {
            giveaway.addParticipant(memberId);
            result = ToggleResult.JOINED;
        }

        manager.persistParticipant(giveaway.getGuildId(), giveaway.getId(), memberId, result == ToggleResult.JOINED);
        return result;
    }

    public List<String> rollWinners(Giveaway giveaway) {
        List<String> participants = new ArrayList<>(giveaway.getParticipantIds());
        Collections.shuffle(participants);
        return participants.stream().limit(giveaway.getWinnerCount()).toList();
    }

    public boolean end(String guildId, String giveawayId) {
        Optional<Giveaway> giveawayOpt = manager.getGiveaway(guildId, giveawayId);
        if (giveawayOpt.isEmpty() || !giveawayOpt.get().isRunning()) {
            return false;
        }
        Giveaway giveaway = giveawayOpt.get();
        giveaway.end(rollWinners(giveaway));
        manager.save(giveaway);
        return true;
    }

    public boolean cancel(String guildId, String giveawayId) {
        Optional<Giveaway> giveawayOpt = manager.getGiveaway(guildId, giveawayId);
        if (giveawayOpt.isEmpty() || !giveawayOpt.get().isRunning()) {
            return false;
        }
        Giveaway giveaway = giveawayOpt.get();
        giveaway.cancel();
        manager.save(giveaway);
        return true;
    }

    public boolean reroll(String guildId, String giveawayId) {
        Optional<Giveaway> giveawayOpt = manager.getGiveaway(guildId, giveawayId);
        if (giveawayOpt.isEmpty() || giveawayOpt.get().getStatus() != GiveawayStatus.ENDED) {
            return false;
        }
        Giveaway giveaway = giveawayOpt.get();
        if (giveaway.getParticipantIds().isEmpty()) {
            return false;
        }
        giveaway.reroll(rollWinners(giveaway));
        manager.save(giveaway);
        return true;
    }

    public Map<String, List<Giveaway>> findDueGiveaways() {
        Map<String, List<Giveaway>> due = new HashMap<>();
        manager.getAllRunningGiveawaysByGuild().forEach((guildId, giveaways) -> {
            List<Giveaway> matches = giveaways.stream().filter(Giveaway::isDue).toList();
            if (!matches.isEmpty()) {
                due.put(guildId, matches);
            }
        });
        return due;
    }

    public int pruneFinishedGiveaways(Duration retention) {
        return manager.pruneFinishedGiveaways(retention);
    }
}
