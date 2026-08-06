package de.destenylp.xBotenyy.discordbot.commands;
import de.destenylp.xBotenyy.common.moderation.ModerationAction;
import de.destenylp.xBotenyy.common.moderation.ModerationCase;
import de.destenylp.xBotenyy.common.moderation.ModerationCaseRepository;
import de.destenylp.xBotenyy.common.moderation.ModerationPlatform;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.moderation.DiscordModerationService;
import de.destenylp.xBotenyy.discordbot.moderation.DiscordModerationSyncTrigger;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationPermissionGuard;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationRoleSettingsRepository;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;
public class ModerationCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationCommand.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneOffset.UTC);
    private final DiscordModerationService moderationService;
    private final ModerationRoleSettingsRepository roleSettingsRepository;
    private final ModerationCaseRepository caseRepository;
    private final DiscordModerationSyncTrigger syncTrigger;
    public ModerationCommand(DiscordModerationService moderationService,
                              ModerationRoleSettingsRepository roleSettingsRepository,
                              ModerationCaseRepository caseRepository,
                              DiscordModerationSyncTrigger syncTrigger) {
        this.moderationService = moderationService;
        this.roleSettingsRepository = roleSettingsRepository;
        this.caseRepository = caseRepository;
        this.syncTrigger = syncTrigger;
    }
    @Override
    public CommandData getCommandData() {
        OptionData reasonOption = new OptionData(OptionType.STRING, "grund", "Grund", false);
        OptionData durationOption = new OptionData(OptionType.STRING, "dauer", "Dauer (z. B. 10m, 1h, 1d, 1w)", true);
        return Commands.slash("mod", "Moderations-Werkzeuge")
                .addSubcommands(
                        new SubcommandData("warn", "Verwarnt ein Mitglied")
                                .addOptions(new OptionData(OptionType.USER, "user", "Mitglied", true), reasonOption),
                        new SubcommandData("timeout", "Setzt ein Mitglied auf Timeout")
                                .addOptions(new OptionData(OptionType.USER, "user", "Mitglied", true), durationOption, reasonOption),
                        new SubcommandData("untimeout", "Hebt einen Timeout auf")
                                .addOptions(new OptionData(OptionType.USER, "user", "Mitglied", true), reasonOption),
                        new SubcommandData("kick", "Kickt ein Mitglied")
                                .addOptions(new OptionData(OptionType.USER, "user", "Mitglied", true), reasonOption),
                        new SubcommandData("ban", "Bannt einen Nutzer (auch per ID)")
                                .addOptions(new OptionData(OptionType.USER, "user", "Nutzer", true), reasonOption),
                        new SubcommandData("unban", "Hebt einen Bann auf")
                                .addOptions(new OptionData(OptionType.USER, "user", "Nutzer", true), reasonOption),
                        new SubcommandData("cases", "Zeigt die Moderations-Historie eines Nutzers")
                                .addOptions(new OptionData(OptionType.USER, "user", "Nutzer", true))
                );
    }
    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        var roleSettings = roleSettingsRepository.getOrEmpty(guild.getId());
        if (!ModerationPermissionGuard.requireModerator(event, roleSettings)) {
            return;
        }
        switch (subcommand) {
            case "warn" -> handleWarn(event, guild);
            case "timeout" -> handleTimeout(event, guild);
            case "untimeout" -> handleUntimeout(event, guild);
            case "kick" -> handleKick(event, guild);
            case "ban" -> handleBan(event, guild);
            case "unban" -> handleUnban(event, guild);
            case "cases" -> handleCases(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }
    private void handleWarn(SlashCommandInteractionEvent event, Guild guild) {
        Member target = event.getOption("user").getAsMember();
        if (target == null) {
            event.reply("Dieser Nutzer ist kein Mitglied dieses Servers.").setEphemeral(true).queue();
            return;
        }
        String reason = optionalReason(event);
        moderationService.warn(guild, target, event.getMember(), reason, () -> {
            int total = caseRepository.countActiveWarnings(ModerationPlatform.DISCORD, guild.getId(), target.getId());
            event.reply("\u26A0\uFE0F " + target.getAsMention() + " wurde verwarnt (" + total + ". Verwarnung). Grund: " + reason)
                    .queue();
            AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_WARN", "target=" + target.getId());
            syncTrigger.trigger(target.getId(), ModerationAction.WARN, reason, 0, event.getUser().getName());
        }, failure -> replyError(event, failure));
    }
    private void handleTimeout(SlashCommandInteractionEvent event, Guild guild) {
        Member target = event.getOption("user").getAsMember();
        if (target == null) {
            event.reply("Dieser Nutzer ist kein Mitglied dieses Servers.").setEphemeral(true).queue();
            return;
        }
        Duration duration = parseDuration(event.getOption("dauer").getAsString());
        if (duration == null) {
            event.reply("Ungueltige Dauer. Beispiele: `10m`, `1h`, `1d`, `1w` (max. 28 Tage).").setEphemeral(true).queue();
            return;
        }
        String reason = optionalReason(event);
        moderationService.timeout(guild, target, event.getMember(), reason, duration, () -> {
            event.reply("\uD83D\uDD07 " + target.getAsMention() + " wurde fuer " + formatDuration(duration)
                    + " getimeoutet. Grund: " + reason).queue();
            AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_TIMEOUT", "target=" + target.getId());
            syncTrigger.trigger(target.getId(), ModerationAction.TIMEOUT, reason, duration.getSeconds(), event.getUser().getName());
        }, failure -> replyError(event, failure));
    }
    private void handleUntimeout(SlashCommandInteractionEvent event, Guild guild) {
        Member target = event.getOption("user").getAsMember();
        if (target == null) {
            event.reply("Dieser Nutzer ist kein Mitglied dieses Servers.").setEphemeral(true).queue();
            return;
        }
        String reason = optionalReason(event);
        moderationService.untimeout(guild, target, event.getMember(), reason, () -> {
            event.reply("\uD83D\uDD0A Timeout von " + target.getAsMention() + " wurde aufgehoben.").queue();
            AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_UNTIMEOUT", "target=" + target.getId());
            syncTrigger.trigger(target.getId(), ModerationAction.UNTIMEOUT, reason, 0, event.getUser().getName());
        }, failure -> replyError(event, failure));
    }
    private void handleKick(SlashCommandInteractionEvent event, Guild guild) {
        Member target = event.getOption("user").getAsMember();
        if (target == null) {
            event.reply("Dieser Nutzer ist kein Mitglied dieses Servers.").setEphemeral(true).queue();
            return;
        }
        String reason = optionalReason(event);
        moderationService.kick(guild, target, event.getMember(), reason, () -> {
            event.reply("\uD83D\uDC62 " + target.getUser().getAsTag() + " wurde gekickt. Grund: " + reason).queue();
            AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_KICK", "target=" + target.getId());
        }, failure -> replyError(event, failure));
    }
    private void handleBan(SlashCommandInteractionEvent event, Guild guild) {
        User target = event.getOption("user").getAsUser();
        String reason = optionalReason(event);
        moderationService.ban(guild, UserSnowflake.fromId(target.getId()), target.getId(), target.getName(),
                event.getMember(), reason, () -> {
                    event.reply("\uD83D\uDD28 " + target.getAsTag() + " wurde gebannt. Grund: " + reason).queue();
                    AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_BAN", "target=" + target.getId());
                    syncTrigger.trigger(target.getId(), ModerationAction.BAN, reason, 0, event.getUser().getName());
                }, failure -> replyError(event, failure));
    }
    private void handleUnban(SlashCommandInteractionEvent event, Guild guild) {
        User target = event.getOption("user").getAsUser();
        String reason = optionalReason(event);
        moderationService.unban(guild, UserSnowflake.fromId(target.getId()), target.getId(), target.getName(),
                event.getMember(), reason, () -> {
                    event.reply("\u2696\uFE0F Bann von " + target.getAsTag() + " wurde aufgehoben.").queue();
                    AuditLog.record(guild.getId(), event.getUser().getId(), "MOD_UNBAN", "target=" + target.getId());
                    syncTrigger.trigger(target.getId(), ModerationAction.UNBAN, reason, 0, event.getUser().getName());
                }, failure -> replyError(event, failure));
    }
    private void handleCases(SlashCommandInteractionEvent event, Guild guild) {
        User target = event.getOption("user").getAsUser();
        List<ModerationCase> cases = caseRepository.findByTarget(ModerationPlatform.DISCORD, guild.getId(), target.getId(), 15);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDCC1 Moderations-Historie – " + target.getAsTag());
        if (cases.isEmpty()) {
            eb.setDescription("Keine Eintraege vorhanden.");
        } else {
            for (ModerationCase moderationCase : cases) {
                String timestamp = TIME_FORMAT.format(Instant.ofEpochMilli(moderationCase.createdAt()));
                String status = moderationCase.active() ? "aktiv" : "aufgehoben";
                String source = moderationCase.synced() ? " (synchronisiert von Twitch)" : "";
                eb.addField(moderationCase.action().name() + " – " + timestamp,
                        "Von " + moderationCase.moderatorName() + " · " + status + source + "\nGrund: "
                                + (moderationCase.reason() == null || moderationCase.reason().isBlank() ? "-" : moderationCase.reason()),
                        false);
            }
        }
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
    private String optionalReason(SlashCommandInteractionEvent event) {
        OptionMapping option = event.getOption("grund");
        return option != null ? option.getAsString() : "Kein Grund angegeben";
    }
    private void replyError(SlashCommandInteractionEvent event, Throwable failure) {
        LOGGER.error("Moderation action failed: ", failure);
        String message = "Die Aktion konnte nicht ausgefuehrt werden. Hat der Bot die noetigen Berechtigungen und eine hoehere Rolle als das Ziel?";
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(message).queue();
        } else {
            event.reply(message).setEphemeral(true).queue();
        }
    }
    private static Duration parseDuration(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim().toLowerCase();
        try {
            char unit = trimmed.charAt(trimmed.length() - 1);
            long amount = Long.parseLong(trimmed.substring(0, trimmed.length() - 1));
            Duration duration = switch (unit) {
                case 's' -> Duration.ofSeconds(amount);
                case 'm' -> Duration.ofMinutes(amount);
                case 'h' -> Duration.ofHours(amount);
                case 'd' -> Duration.ofDays(amount);
                case 'w' -> Duration.ofDays(amount * 7);
                default -> null;
            };
            if (duration == null || duration.isNegative() || duration.isZero() || duration.toDays() > 28) {
                return null;
            }
            return duration;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return null;
        }
    }
    private static String formatDuration(Duration duration) {
        if (duration.toDays() >= 1) {
            return duration.toDays() + " Tag(e)";
        }
        if (duration.toHours() >= 1) {
            return duration.toHours() + " Stunde(n)";
        }
        if (duration.toMinutes() >= 1) {
            return duration.toMinutes() + " Minute(n)";
        }
        return duration.getSeconds() + " Sekunde(n)";
    }
}
