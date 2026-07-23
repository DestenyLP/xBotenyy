package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.giveaways.Giveaway;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayEmbedFactory;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayEndCoordinator;
import de.destenylp.xBotenyy.discordbot.giveaways.GiveawayService;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
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
import java.util.Optional;

public class GiveawayCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveawayCommand.class);

    private final GiveawayService service;
    private final GiveawayEndCoordinator coordinator = new GiveawayEndCoordinator();

    public GiveawayCommand(GiveawayService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("giveaway", "Verwalte Gewinnspiele")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("start", "Startet ein neues Gewinnspiel")
                                .addOption(OptionType.STRING, "preis", "Was wird verlost?", true)
                                .addOption(OptionType.INTEGER, "gewinner", "Anzahl der Gewinner", true)
                                .addOption(OptionType.STRING, "dauer", "Laufzeit, z.B. 30s, 10m, 2h, 1d, 1w", true)
                                .addOptions(new OptionData(OptionType.CHANNEL, "kanal", "Kanal, in dem das Gewinnspiel gepostet wird (Standard: aktueller Kanal)", false)
                                        .setChannelTypes(ChannelType.TEXT))
                                .addOption(OptionType.ROLE, "rolle", "Rolle, die zur Teilnahme benötigt wird", false)
                                .addOption(OptionType.STRING, "beschreibung", "Zusätzliche Beschreibung für das Gewinnspiel", false),
                        new SubcommandData("end", "Beendet ein laufendes Gewinnspiel sofort und lost Gewinner aus")
                                .addOption(OptionType.STRING, "gewinnspiel-id", "ID des Gewinnspiels", true),
                        new SubcommandData("reroll", "Lost für ein beendetes Gewinnspiel neue Gewinner aus")
                                .addOption(OptionType.STRING, "gewinnspiel-id", "ID des Gewinnspiels", true),
                        new SubcommandData("cancel", "Bricht ein laufendes Gewinnspiel ohne Auslosung ab")
                                .addOption(OptionType.STRING, "gewinnspiel-id", "ID des Gewinnspiels", true),
                        new SubcommandData("list", "Zeigt alle laufenden Gewinnspiele")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        switch (subcommand) {
            case "start" -> handleStart(event, guild);
            case "end" -> handleEnd(event, guild);
            case "reroll" -> handleReroll(event, guild);
            case "cancel" -> handleCancel(event, guild);
            case "list" -> handleList(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleStart(SlashCommandInteractionEvent event, Guild guild) {
        String prize = event.getOption("preis").getAsString();
        long winners = event.getOption("gewinner").getAsLong();
        String durationRaw = event.getOption("dauer").getAsString();
        OptionMapping channelOpt = event.getOption("kanal");
        OptionMapping roleOpt = event.getOption("rolle");
        OptionMapping descriptionOpt = event.getOption("beschreibung");

        if (winners < service.getMinWinners() || winners > service.getMaxWinners()) {
            event.reply("Die Anzahl der Gewinner muss zwischen " + service.getMinWinners() +
                    " und " + service.getMaxWinners() + " liegen.").setEphemeral(true).queue();
            return;
        }

        Optional<Duration> durationOpt = service.parseDuration(durationRaw);
        if (durationOpt.isEmpty()) {
            event.reply("Ungültige Dauer. Beispiele: `30s`, `10m`, `2h`, `1d`, `1w`.").setEphemeral(true).queue();
            return;
        }

        Duration duration = durationOpt.get();
        if (duration.compareTo(service.getMinDuration()) < 0 || duration.compareTo(service.getMaxDuration()) > 0) {
            event.reply("Die Dauer muss zwischen " + service.getMinDuration().toSeconds() +
                    " Sekunden und " + service.getMaxDuration().toDays() + " Tagen liegen.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = channelOpt != null ? channelOpt.getAsChannel().asTextChannel() : event.getChannel().asTextChannel();
        String requiredRoleId = roleOpt != null ? roleOpt.getAsRole().getId() : null;
        String description = descriptionOpt != null ? descriptionOpt.getAsString() : null;
        Member member = event.getMember();
        long endAt = Instant.now().plus(duration).toEpochMilli();

        Giveaway giveaway = service.createGiveaway(guild.getId(), prize, description, (int) winners,
                member.getId(), member.getUser().getName(), requiredRoleId, endAt);

        event.deferReply(true).queue();

        channel.sendMessageEmbeds(GiveawayEmbedFactory.buildAnnouncementEmbed(giveaway))
                .setComponents(GiveawayEmbedFactory.buildEnterComponents(giveaway))
                .queue(message -> {
                    service.attachMessage(guild.getId(), giveaway.getId(), channel.getId(), message.getId());
                    event.getHook().sendMessage("Gewinnspiel `" + giveaway.getId() + "` wurde in " + channel.getAsMention() + " gestartet!").queue();
                    LOGGER.info("Giveaway {} started in guild {} channel {}", giveaway.getId(), guild.getId(), channel.getId());
                    AuditLog.record(guild.getId(), member.getId(), "GIVEAWAY_START",
                            "id=" + giveaway.getId() + " prize=" + prize + " winners=" + winners + " duration=" + durationRaw);
                    BotMetrics.incrementGiveawaysCreated();
                }, failure -> {
                    LOGGER.error("Failed to post giveaway message: {}", failure.getMessage());
                    event.getHook().sendMessage("Das Gewinnspiel konnte nicht gepostet werden.").queue();
                });
    }

    private void handleEnd(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("gewinnspiel-id").getAsString();
        Optional<Giveaway> giveawayOpt = service.getGiveaway(guild.getId(), id);
        if (giveawayOpt.isEmpty() || !giveawayOpt.get().isRunning()) {
            event.reply("Es wurde kein laufendes Gewinnspiel mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }

        boolean success = service.end(guild.getId(), id);
        if (!success) {
            event.reply("Das Gewinnspiel konnte nicht beendet werden.").setEphemeral(true).queue();
            return;
        }

        Giveaway giveaway = service.getGiveaway(guild.getId(), id).orElseThrow();
        event.reply("Gewinnspiel `" + id + "` wurde beendet.").setEphemeral(true).queue();
        LOGGER.info("Giveaway {} manually ended by {} in guild {}", id, event.getUser().getId(), guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "GIVEAWAY_END", "id=" + id);
        coordinator.announceEnd(event.getJDA(), giveaway);
    }

    private void handleReroll(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("gewinnspiel-id").getAsString();
        boolean success = service.reroll(guild.getId(), id);
        if (!success) {
            event.reply("Es konnte keine Neuauslosung durchgeführt werden. Ist das Gewinnspiel beendet und hat Teilnehmer?").setEphemeral(true).queue();
            return;
        }

        Giveaway giveaway = service.getGiveaway(guild.getId(), id).orElseThrow();
        event.reply("Für Gewinnspiel `" + id + "` wurden neue Gewinner ausgelost.").setEphemeral(true).queue();
        LOGGER.info("Giveaway {} rerolled by {} in guild {}", id, event.getUser().getId(), guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "GIVEAWAY_REROLL", "id=" + id);
        coordinator.announceReroll(event.getJDA(), giveaway);
    }

    private void handleCancel(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("gewinnspiel-id").getAsString();
        Optional<Giveaway> giveawayOpt = service.getGiveaway(guild.getId(), id);
        if (giveawayOpt.isEmpty() || !giveawayOpt.get().isRunning()) {
            event.reply("Es wurde kein laufendes Gewinnspiel mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }

        boolean success = service.cancel(guild.getId(), id);
        if (!success) {
            event.reply("Das Gewinnspiel konnte nicht abgebrochen werden.").setEphemeral(true).queue();
            return;
        }

        Giveaway giveaway = service.getGiveaway(guild.getId(), id).orElseThrow();
        event.reply("Gewinnspiel `" + id + "` wurde abgebrochen.").setEphemeral(true).queue();
        LOGGER.info("Giveaway {} cancelled by {} in guild {}", id, event.getUser().getId(), guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "GIVEAWAY_CANCEL", "id=" + id);
        coordinator.announceCancel(event.getJDA(), giveaway);
    }

    private void handleList(SlashCommandInteractionEvent event, Guild guild) {
        event.replyEmbeds(GiveawayEmbedFactory.buildListEmbed(guild.getName(), service.getRunningGiveaways(guild.getId())))
                .setEphemeral(true).queue();
    }
}
