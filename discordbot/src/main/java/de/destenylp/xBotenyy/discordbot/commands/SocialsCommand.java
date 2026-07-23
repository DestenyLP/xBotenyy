package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.socials.SocialAccount;
import de.destenylp.xBotenyy.discordbot.socials.SocialMessageFactory;
import de.destenylp.xBotenyy.discordbot.socials.SocialService;
import de.destenylp.xBotenyy.discordbot.socials.SocialValidators;
import de.destenylp.xBotenyy.discordbot.socials.SocialsPollStatus;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchApiClient;
import de.destenylp.xBotenyy.discordbot.socials.twitch.TwitchStream;
import de.destenylp.xBotenyy.discordbot.socials.youtube.YoutubeFeedClient;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.FieldEdit;
import de.destenylp.xBotenyy.discordbot.util.ImageUrlValidator;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SocialsCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocialsCommand.class);

    private final SocialService service;
    private final String defaultYoutubeMessage;
    private final String defaultTwitchMessage;
    private final YoutubeFeedClient youtubeFeedClient;
    private final TwitchApiClient twitchApiClient;

    public SocialsCommand(SocialService service, String defaultYoutubeMessage, String defaultTwitchMessage,
                           YoutubeFeedClient youtubeFeedClient, TwitchApiClient twitchApiClient) {
        this.service = service;
        this.defaultYoutubeMessage = defaultYoutubeMessage;
        this.defaultTwitchMessage = defaultTwitchMessage;
        this.youtubeFeedClient = youtubeFeedClient;
        this.twitchApiClient = twitchApiClient;
    }

    @Override
    public CommandData getCommandData() {
        OptionData platformOption = new OptionData(OptionType.STRING, "platform", "Plattform", true)
                .addChoice("YouTube", "youtube")
                .addChoice("Twitch", "twitch");

        return Commands.slash("socials", "Verwalte YouTube- und Twitch-Benachrichtigungen")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("add", "Fuegt einen neuen Account hinzu, der ueberwacht werden soll")
                                .addOption(OptionType.STRING, "name", "Anzeigename des Accounts, z.B. sapphire", true)
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Kanal fuer Ankuendigungen", true))
                                .addOption(OptionType.STRING, "youtube", "YouTube Kanal-ID (beginnt mit UC...)", false)
                                .addOption(OptionType.STRING, "twitch", "Twitch Benutzername", false),
                        new SubcommandData("edit", "Bearbeitet einen bestehenden Account")
                                .addOption(OptionType.STRING, "id", "ID des Accounts", true)
                                .addOption(OptionType.STRING, "name", "Neuer Anzeigename", false)
                                .addOptions(new OptionData(OptionType.CHANNEL, "channel", "Neuer Ankuendigungskanal", false))
                                .addOption(OptionType.BOOLEAN, "enabled", "Ueberwachung aktivieren/deaktivieren", false)
                                .addOption(OptionType.STRING, "youtube", "Neue YouTube Kanal-ID ('none' zum Entfernen)", false)
                                .addOption(OptionType.STRING, "twitch", "Neuer Twitch Benutzername ('none' zum Entfernen)", false),
                        new SubcommandData("message", "Passt die Ankuendigungsnachricht fuer eine Plattform an")
                                .addOption(OptionType.STRING, "id", "ID des Accounts", true)
                                .addOptions(platformOption)
                                .addOption(OptionType.STRING, "content", "Nachrichtentext (Platzhalter siehe /socials placeholders)", false)
                                .addOption(OptionType.BOOLEAN, "embed", "Als Embed senden?", false)
                                .addOption(OptionType.STRING, "title", "Titel ('none' zum Entfernen, nur bei Embed)", false)
                                .addOption(OptionType.STRING, "color", "Hex Farbe ('none' zum Entfernen, nur bei Embed)", false)
                                .addOption(OptionType.STRING, "image", "Bild-URL ('none' zum Entfernen, nur bei Embed)", false)
                                .addOption(OptionType.STRING, "footer", "Footer-Text ('none' zum Entfernen, nur bei Embed)", false),
                        new SubcommandData("remove", "Entfernt einen Account")
                                .addOption(OptionType.STRING, "id", "ID des Accounts", true),
                        new SubcommandData("list", "Zeigt alle konfigurierten Accounts"),
                        new SubcommandData("status", "Zeigt den Status der Ueberwachung (letzter Check, Fehler, Accounts)"),
                        new SubcommandData("test", "Sendet eine Testvorschau der Ankuendigung")
                                .addOption(OptionType.STRING, "id", "ID des Accounts", true)
                                .addOptions(new OptionData(OptionType.STRING, "platform", "Plattform", true)
                                        .addChoice("YouTube", "youtube")
                                        .addChoice("Twitch", "twitch")),
                        new SubcommandData("placeholders", "Zeigt alle verfuegbaren Platzhalter")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        switch (subcommand) {
            case "add" -> handleAdd(event, guild);
            case "edit" -> handleEdit(event, guild);
            case "message" -> handleMessage(event, guild);
            case "remove" -> handleRemove(event, guild);
            case "list" -> handleList(event, guild);
            case "status" -> handleStatus(event, guild);
            case "test" -> handleTest(event, guild);
            case "placeholders" -> handlePlaceholders(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleAdd(SlashCommandInteractionEvent event, Guild guild) {
        if (service.isAtCapacity(guild.getId())) {
            event.reply("Es koennen maximal " + service.getMaxAccountsPerGuild()
                    + " Accounts pro Server ueberwacht werden.").setEphemeral(true).queue();
            return;
        }

        String name = event.getOption("name").getAsString();
        TextChannel channel = event.getOption("channel").getAsChannel().asTextChannel();
        OptionMapping youtubeOption = event.getOption("youtube");
        OptionMapping twitchOption = event.getOption("twitch");

        if (youtubeOption == null && twitchOption == null) {
            event.reply("Bitte gib mindestens einen YouTube-Kanal oder Twitch-Benutzernamen an.").setEphemeral(true).queue();
            return;
        }

        String youtubeChannelId = youtubeOption != null ? youtubeOption.getAsString() : null;
        if (youtubeChannelId != null && !SocialValidators.isValidYoutubeChannelId(youtubeChannelId)) {
            event.reply("Die YouTube Kanal-ID ist ungueltig. Sie beginnt mit `UC` und hat 24 Zeichen.").setEphemeral(true).queue();
            return;
        }

        String twitchLogin = twitchOption != null ? twitchOption.getAsString() : null;
        if (twitchLogin != null && !SocialValidators.isValidTwitchLogin(twitchLogin)) {
            event.reply("Der Twitch Benutzername ist ungueltig.").setEphemeral(true).queue();
            return;
        }

        SocialAccount draft = SocialAccount.builder()
                .name(name)
                .channelId(channel.getId())
                .youtubeChannelId(youtubeChannelId)
                .youtubeMessage(defaultYoutubeMessage)
                .twitchLogin(twitchLogin != null ? twitchLogin.toLowerCase() : null)
                .twitchMessage(defaultTwitchMessage)
                .build();

        SocialAccount created = service.addAccount(guild.getId(), draft);
        event.reply("Account `" + created.getName() + "` wurde hinzugefuegt! ID: `" + created.getId() + "`\n" +
                "Passe die Nachricht bei Bedarf mit `/socials message` an.").setEphemeral(true).queue();
        LOGGER.info("Added social account {} for guild {}", created.getId(), guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "SOCIALS_ACCOUNT_ADD", "accountId=" + created.getId());
    }

    private void handleEdit(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("id").getAsString();
        Optional<SocialAccount> accountOpt = service.getAccount(guild.getId(), id);
        if (accountOpt.isEmpty()) {
            event.reply("Es wurde kein Account mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }
        SocialAccount account = accountOpt.get();

        OptionMapping nameOption = event.getOption("name");
        OptionMapping channelOption = event.getOption("channel");
        OptionMapping enabledOption = event.getOption("enabled");
        OptionMapping youtubeOption = event.getOption("youtube");
        OptionMapping twitchOption = event.getOption("twitch");

        if (youtubeOption != null && !isClearValue(youtubeOption.getAsString())
                && !SocialValidators.isValidYoutubeChannelId(youtubeOption.getAsString())) {
            event.reply("Die YouTube Kanal-ID ist ungueltig. Sie beginnt mit `UC` und hat 24 Zeichen.").setEphemeral(true).queue();
            return;
        }

        if (twitchOption != null && !isClearValue(twitchOption.getAsString())
                && !SocialValidators.isValidTwitchLogin(twitchOption.getAsString())) {
            event.reply("Der Twitch Benutzername ist ungueltig.").setEphemeral(true).queue();
            return;
        }

        boolean finalHasYoutube = youtubeOption != null ? !isClearValue(youtubeOption.getAsString()) : account.hasYoutube();
        boolean finalHasTwitch = twitchOption != null ? !isClearValue(twitchOption.getAsString()) : account.hasTwitch();
        if (!finalHasYoutube && !finalHasTwitch) {
            event.reply("Ein Account benoetigt mindestens eine Plattform. Nutze `/socials remove`, um ihn stattdessen zu entfernen.")
                    .setEphemeral(true).queue();
            return;
        }

        if (nameOption != null) {
            account.setName(nameOption.getAsString());
        }
        if (channelOption != null) {
            account.setChannelId(channelOption.getAsChannel().asTextChannel().getId());
        }
        if (enabledOption != null) {
            account.setEnabled(enabledOption.getAsBoolean());
        }
        if (youtubeOption != null) {
            if (isClearValue(youtubeOption.getAsString())) {
                account.clearYoutube();
            } else {
                account.setYoutubeChannelId(youtubeOption.getAsString());
            }
        }
        if (twitchOption != null) {
            if (isClearValue(twitchOption.getAsString())) {
                account.clearTwitch();
            } else {
                account.setTwitchLogin(twitchOption.getAsString().toLowerCase());
            }
        }

        service.saveAccount(guild.getId(), account);
        event.reply("Account `" + id + "` wurde aktualisiert.").setEphemeral(true).queue();
        LOGGER.info("Edited social account {} for guild {}", id, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "SOCIALS_ACCOUNT_EDIT", "accountId=" + id);
    }

    private void handleMessage(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("id").getAsString();
        String platform = event.getOption("platform").getAsString();

        Optional<SocialAccount> accountOpt = service.getAccount(guild.getId(), id);
        if (accountOpt.isEmpty()) {
            event.reply("Es wurde kein Account mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }
        SocialAccount account = accountOpt.get();

        if (platform.equals("youtube") && !account.hasYoutube()) {
            event.reply("Fuer diesen Account ist kein YouTube-Kanal konfiguriert.").setEphemeral(true).queue();
            return;
        }
        if (platform.equals("twitch") && !account.hasTwitch()) {
            event.reply("Fuer diesen Account ist kein Twitch-Account konfiguriert.").setEphemeral(true).queue();
            return;
        }

        OptionMapping contentOption = event.getOption("content");
        OptionMapping embedOption = event.getOption("embed");
        OptionMapping titleOption = event.getOption("title");
        OptionMapping colorOption = event.getOption("color");
        OptionMapping imageOption = event.getOption("image");
        OptionMapping footerOption = event.getOption("footer");

        if (colorOption != null && !isClearValue(colorOption.getAsString())
                && DiscordColors.parse(colorOption.getAsString()).isEmpty()) {
            event.reply("Die angegebene Farbe ist ungueltig. Bitte nutze das Format #RRGGBB.").setEphemeral(true).queue();
            return;
        }

        if (imageOption != null && !isClearValue(imageOption.getAsString())
                && !ImageUrlValidator.isValid(imageOption.getAsString())) {
            event.reply("Die Bild-URL ist ungueltig. Erlaubt sind http(s)-Links auf png/jpg/jpeg/gif/webp.").setEphemeral(true).queue();
            return;
        }

        var template = platform.equals("youtube") ? account.getYoutubeTemplate() : account.getTwitchTemplate();

        if (contentOption != null) {
            template.setContent(contentOption.getAsString());
        }
        if (embedOption != null) {
            template.setEmbed(embedOption.getAsBoolean());
        }
        fieldEditFrom(titleOption).applyTo(template::setTitle);
        fieldEditFrom(colorOption).applyTo(template::setColor);
        fieldEditFrom(imageOption).applyTo(template::setImageUrl);
        fieldEditFrom(footerOption).applyTo(template::setFooter);

        service.saveAccount(guild.getId(), account);
        event.reply("Nachricht fuer `" + platform + "` bei Account `" + id + "` wurde aktualisiert.").setEphemeral(true).queue();
        LOGGER.info("Edited {} message template for social account {} in guild {}", platform, id, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "SOCIALS_MESSAGE_EDIT", "accountId=" + id + " platform=" + platform);
    }

    private FieldEdit<String> fieldEditFrom(OptionMapping option) {
        if (option == null) {
            return FieldEdit.notProvided();
        }
        String value = option.getAsString();
        return isClearValue(value) ? FieldEdit.clear() : FieldEdit.value(value);
    }

    private boolean isClearValue(String value) {
        return value != null && (value.equalsIgnoreCase("none") || value.equals("-"));
    }

    private void handleRemove(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("id").getAsString();

        boolean removed = service.removeAccount(guild.getId(), id);
        if (removed) {
            event.reply("Account `" + id + "` wurde entfernt.").setEphemeral(true).queue();
            LOGGER.info("Removed social account {} for guild {}", id, guild.getId());
            AuditLog.record(guild.getId(), event.getUser().getId(), "SOCIALS_ACCOUNT_REMOVE", "accountId=" + id);
        } else {
            event.reply("Es wurde kein Account mit dieser ID gefunden.").setEphemeral(true).queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event, Guild guild) {
        List<SocialAccount> accounts = service.getAccounts(guild.getId());
        if (accounts.isEmpty()) {
            event.reply("Es wurden noch keine Accounts hinzugefuegt. Nutze `/socials add`, um einen zu erstellen.")
                    .setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Ueberwachte Social-Accounts");
        eb.setColor(DiscordColors.brand());

        for (SocialAccount account : accounts) {
            String value = "Kanal: <#" + account.getChannelId() + ">\n" +
                    "Aktiviert: " + (account.isEnabled() ? "Ja" : "Nein") + "\n" +
                    "YouTube: " + (account.hasYoutube() ? "`" + account.getYoutubeChannelId() + "`" : "Nicht konfiguriert") + "\n" +
                    "Twitch: " + (account.hasTwitch() ? "`" + account.getTwitchLogin() + "`" : "Nicht konfiguriert");
            eb.addField(account.getName() + " (ID: " + account.getId() + ")", value, false);
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleTest(SlashCommandInteractionEvent event, Guild guild) {
        String id = event.getOption("id").getAsString();
        String platform = event.getOption("platform").getAsString();

        Optional<SocialAccount> accountOpt = service.getAccount(guild.getId(), id);
        if (accountOpt.isEmpty()) {
            event.reply("Es wurde kein Account mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }
        SocialAccount account = accountOpt.get();

        if (platform.equals("youtube")) {
            if (!account.hasYoutube()) {
                event.reply("Fuer diesen Account ist kein YouTube-Kanal konfiguriert.").setEphemeral(true).queue();
                return;
            }
            event.deferReply(true).queue();
            String youtubeChannelId = account.getYoutubeChannelId();
            CompletableFuture.supplyAsync(() -> youtubeFeedClient.fetchLatestVideo(youtubeChannelId))
                    .thenAccept(videoOpt -> {
                        if (videoOpt.isEmpty()) {
                            event.getHook().sendMessage("Es konnten keine echten Video-Daten fuer diesen Kanal abgerufen werden "
                                    + "(kein Video gefunden oder Fehler beim Abruf). Nutze `/socials status` fuer Details.").queue();
                            return;
                        }
                        RenderedMessage built = SocialMessageFactory.buildYoutubeMessage(account, videoOpt.get(), guild);
                        sendPreview(event, platform, id, built);
                    })
                    .exceptionally(ex -> {
                        LOGGER.warn("Fehler beim Abrufen echter YouTube-Daten fuer Test-Vorschau: {}", ex.getMessage());
                        event.getHook().sendMessage("Es ist ein Fehler beim Abrufen der echten Video-Daten aufgetreten.").queue();
                        return null;
                    });
        } else {
            if (!account.hasTwitch()) {
                event.reply("Fuer diesen Account ist kein Twitch-Account konfiguriert.").setEphemeral(true).queue();
                return;
            }
            if (twitchApiClient == null) {
                event.reply("Twitch ist auf diesem Bot nicht konfiguriert (keine Zugangsdaten hinterlegt).")
                        .setEphemeral(true).queue();
                return;
            }
            event.deferReply(true).queue();
            String twitchLogin = account.getTwitchLogin();
            CompletableFuture.supplyAsync(() -> twitchApiClient.fetchLiveStreams(List.of(twitchLogin)))
                    .thenAccept(streams -> {
                        TwitchStream stream = streams.get(twitchLogin.toLowerCase());
                        if (stream == null) {
                            event.getHook().sendMessage("`" + twitchLogin + "` ist gerade nicht live, es liegen daher keine "
                                    + "echten Stream-Daten vor. Starte einen Stream und versuche es erneut, um eine "
                                    + "Vorschau mit echten Daten zu erhalten.").queue();
                            return;
                        }
                        RenderedMessage built = SocialMessageFactory.buildTwitchMessage(account, stream, guild);
                        sendPreview(event, platform, id, built);
                    })
                    .exceptionally(ex -> {
                        LOGGER.warn("Fehler beim Abrufen echter Twitch-Daten fuer Test-Vorschau: {}", ex.getMessage());
                        event.getHook().sendMessage("Es ist ein Fehler beim Abrufen der echten Stream-Daten aufgetreten.").queue();
                        return null;
                    });
        }
    }

    private void sendPreview(SlashCommandInteractionEvent event, String platform, String id, RenderedMessage built) {
        String prefix = "**Vorschau** (`" + platform + "`, Account `" + id + "`, echte Daten):\n";
        String body = built.content() != null ? built.content() : "";
        WebhookMessageCreateAction<?> action = event.getHook().sendMessage(prefix + body);
        if (built.embed() != null) {
            action = action.addEmbeds(built.embed());
        }
        action.queue();
    }

    private void handleStatus(SlashCommandInteractionEvent event, Guild guild) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Socials Status");
        eb.setColor(DiscordColors.brand());

        eb.addField("Twitch Zugangsdaten", SocialsPollStatus.isTwitchConfigured() ? "Konfiguriert" : "Nicht konfiguriert", true);
        eb.addField("YouTube Intervall", SocialsPollStatus.getYoutubeIntervalMinutes() + " min", true);
        eb.addField("Twitch Intervall", SocialsPollStatus.getTwitchIntervalMinutes() + " min", true);
        eb.addField("Letzter YouTube-Check", formatRelative(SocialsPollStatus.getLastYoutubePoll()), true);
        eb.addField("Letzter Twitch-Check", formatRelative(SocialsPollStatus.getLastTwitchPoll()), true);

        String youtubeError = SocialsPollStatus.getLastYoutubeError();
        String twitchError = SocialsPollStatus.getLastTwitchError();
        if (youtubeError != null) {
            eb.addField("Letzter YouTube-Fehler", youtubeError, false);
        }
        if (twitchError != null) {
            eb.addField("Letzter Twitch-Fehler", twitchError, false);
        }

        List<SocialAccount> accounts = service.getAccounts(guild.getId());
        if (accounts.isEmpty()) {
            eb.addField("Accounts", "Es wurden noch keine Accounts hinzugefuegt.", false);
        } else {
            for (SocialAccount account : accounts) {
                StringBuilder value = new StringBuilder();
                value.append("Aktiviert: ").append(account.isEnabled() ? "Ja" : "Nein").append("\n");
                if (account.hasYoutube()) {
                    value.append("YouTube: `").append(account.getYoutubeChannelId()).append("`, letztes Video: ")
                            .append(account.getLastYoutubeVideoId() != null
                                    ? "`" + account.getLastYoutubeVideoId() + "`" : "noch keine Baseline")
                            .append("\n");
                }
                if (account.hasTwitch()) {
                    value.append("Twitch: `").append(account.getTwitchLogin()).append("`, aktuell live: ")
                            .append(account.isTwitchCurrentlyLive() ? "Ja" : "Nein");
                }
                eb.addField(account.getName() + " (ID: " + account.getId() + ")", value.toString(), false);
            }
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private String formatRelative(Instant instant) {
        if (instant == null) {
            return "Noch kein Durchlauf";
        }
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) {
            return "vor " + seconds + "s";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return "vor " + minutes + "min";
        }
        long hours = minutes / 60;
        return "vor " + hours + "h";
    }

    private void handlePlaceholders(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Verfuegbare Platzhalter fuer /socials message");
        eb.setColor(DiscordColors.brand());
        eb.setDescription("Nutze `\\n` an beliebiger Stelle fuer einen Zeilenumbruch.");
        eb.addField("{account}", "Anzeigename des Accounts", true);
        eb.addField("{video.title}", "Titel des Videos (nur YouTube)", true);
        eb.addField("{video.url}", "Link zum Video (nur YouTube)", true);
        eb.addField("{video.thumbnail}", "Vorschaubild-URL (nur YouTube)", true);
        eb.addField("{stream.title}", "Titel des Livestreams (nur Twitch)", true);
        eb.addField("{stream.game}", "Aktuell gespielte Kategorie (nur Twitch)", true);
        eb.addField("{stream.url}", "Link zum Stream (nur Twitch)", true);
        eb.addField("{twitch.login}", "Twitch Benutzername (nur Twitch)", true);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
