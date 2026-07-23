package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderCatalog;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.FieldEdit;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.ImageUrlValidator;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeMessageFactory;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeService;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeSettings;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeVariant;
import de.destenylp.xBotenyy.discordbot.welcome.WelcomeVariantEdit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class WelcomeCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WelcomeCommand.class);

    private final WelcomeService service;
    private final int previewMaxLength;

    public WelcomeCommand(WelcomeService service, int previewMaxLength) {
        this.service = service;
        this.previewMaxLength = previewMaxLength;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("welcome", "Verwalte die Willkommensnachrichten")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("settings", "Kanal & allgemeine Einstellungen konfigurieren (ohne Optionen: zeigt aktuellen Stand)")
                                .addOption(OptionType.CHANNEL, "channel", "Kanal für Willkommensnachrichten", false)
                                .addOption(OptionType.BOOLEAN, "enabled", "Willkommensnachrichten aktivieren/deaktivieren", false)
                                .addOption(OptionType.BOOLEAN, "dm", "Zusätzlich per Direktnachricht an das Mitglied senden?", false),
                        new SubcommandData("add", "Fügt eine neue Willkommensnachrichten-Variante hinzu")
                                .addOption(OptionType.STRING, "content", "Nachrichtentext (Platzhalter: siehe /welcome placeholders, \\n für Zeilenumbruch)", true)
                                .addOption(OptionType.BOOLEAN, "embed", "Als Embed senden?", true)
                                .addOption(OptionType.STRING, "title", "Titel (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "color", "Hex Farbe für das Embed, z.B. #5865F2", false)
                                .addOption(OptionType.STRING, "image", "Bild-URL (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "footer", "Footer-Text (nur bei Embed)", false)
                                .addOption(OptionType.BOOLEAN, "ping", "Mitglied zusätzlich außerhalb erwähnen? (Standard: an)", false),
                        new SubcommandData("edit", "Bearbeitet eine bestehende Variante")
                                .addOption(OptionType.STRING, "id", "ID der Variante", true)
                                .addOption(OptionType.STRING, "content", "Neuer Nachrichtentext", false)
                                .addOption(OptionType.BOOLEAN, "embed", "Als Embed senden?", false)
                                .addOption(OptionType.STRING, "title", "Neuer Titel ('none' zum Entfernen)", false)
                                .addOption(OptionType.STRING, "color", "Neue Hex Farbe ('none' zum Entfernen)", false)
                                .addOption(OptionType.STRING, "image", "Neue Bild-URL ('none' zum Entfernen)", false)
                                .addOption(OptionType.STRING, "footer", "Neuer Footer ('none' zum Entfernen)", false)
                                .addOption(OptionType.BOOLEAN, "ping", "Mitglied zusätzlich außerhalb erwähnen?", false),
                        new SubcommandData("remove", "Entfernt eine Willkommensnachrichten-Variante")
                                .addOption(OptionType.STRING, "id", "ID der Variante", true),
                        new SubcommandData("list", "Zeigt alle Willkommensnachrichten-Varianten"),
                        new SubcommandData("test", "Sendet eine Testvorschau der Willkommensnachricht")
                                .addOption(OptionType.STRING, "id", "ID der Variante (leer = zufällig)", false),
                        new SubcommandData("placeholders", "Zeigt alle verfügbaren Platzhalter")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        switch (subcommand) {
            case "settings" -> handleSettings(event);
            case "add" -> handleAdd(event);
            case "edit" -> handleEdit(event);
            case "remove" -> handleRemove(event);
            case "list" -> handleList(event);
            case "test" -> handleTest(event);
            case "placeholders" -> handlePlaceholders(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleSettings(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        OptionMapping channelOption = event.getOption("channel");
        OptionMapping enabledOption = event.getOption("enabled");
        OptionMapping dmOption = event.getOption("dm");

        if (channelOption == null && enabledOption == null && dmOption == null) {
            WelcomeSettings settings = service.getSettings(guild.getId()).orElse(null);
            event.reply(describeSettings(settings)).setEphemeral(true).queue();
            return;
        }

        if (channelOption != null) {
            TextChannel channel = channelOption.getAsChannel().asTextChannel();
            service.updateChannel(guild.getId(), channel.getId());
        }
        if (enabledOption != null) {
            service.updateEnabled(guild.getId(), enabledOption.getAsBoolean());
        }
        if (dmOption != null) {
            service.updateDm(guild.getId(), dmOption.getAsBoolean());
        }

        WelcomeSettings updated = service.getSettings(guild.getId()).orElse(null);
        event.reply("Einstellungen aktualisiert!\n" + describeSettings(updated)).setEphemeral(true).queue();
        LOGGER.info("Welcome settings updated for guild {}", guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "WELCOME_SETTINGS_UPDATE",
                "channel=" + (updated != null ? updated.getChannelId() : null)
                        + " enabled=" + (updated != null && updated.isEnabled())
                        + " dm=" + (updated != null && updated.isDmEnabled()));
    }

    private String describeSettings(WelcomeSettings settings) {
        if (settings == null) {
            return "Noch nicht konfiguriert. Nutze `/welcome settings channel:#kanal enabled:true`.";
        }
        return "Kanal: " + (settings.getChannelId() != null ? "<#" + settings.getChannelId() + ">" : "Nicht gesetzt") + "\n" +
                "Aktiviert: " + (settings.isEnabled() ? "Ja" : "Nein") + "\n" +
                "Zusätzlich per DM: " + (settings.isDmEnabled() ? "Ja" : "Nein") + "\n" +
                "Anzahl Varianten: " + settings.getVariants().size();
    }

    private void handleAdd(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        String content = event.getOption("content").getAsString();
        boolean embed = event.getOption("embed").getAsBoolean();
        OptionMapping titleOption = event.getOption("title");
        OptionMapping colorOption = event.getOption("color");
        OptionMapping imageOption = event.getOption("image");
        OptionMapping footerOption = event.getOption("footer");
        OptionMapping pingOption = event.getOption("ping");

        String title = titleOption != null ? titleOption.getAsString() : null;
        String color = colorOption != null ? colorOption.getAsString() : null;
        String image = imageOption != null ? imageOption.getAsString() : null;
        String footer = footerOption != null ? footerOption.getAsString() : null;
        boolean ping = pingOption == null || pingOption.getAsBoolean();

        if (color != null && DiscordColors.parse(color).isEmpty()) {
            event.reply("Die angegebene Farbe ist ungültig. Bitte nutze das Format #RRGGBB.").setEphemeral(true).queue();
            return;
        }

        if (!ImageUrlValidator.isValid(image)) {
            event.reply("Die Bild-URL ist ungültig. Erlaubt sind http(s)-Links auf png/jpg/jpeg/gif/webp.").setEphemeral(true).queue();
            return;
        }

        WelcomeVariant draft = WelcomeVariant.builder()
                .embed(embed)
                .title(title)
                .content(content)
                .color(color)
                .imageUrl(image)
                .footer(footer)
                .ping(ping)
                .build();

        WelcomeVariant variant = service.addVariant(guild.getId(), draft);
        event.reply("Willkommensnachricht-Variante erstellt! ID: `" + variant.getId() + "`\n" +
                "Teste sie mit `/welcome test id:" + variant.getId() + "`.").setEphemeral(true).queue();
        LOGGER.info("Added welcome variant {} for guild {}", variant.getId(), guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "WELCOME_VARIANT_ADD", "variantId=" + variant.getId());
    }

    private void handleEdit(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        String id = event.getOption("id").getAsString();

        Optional<WelcomeVariant> variantOpt = service.getVariant(guild.getId(), id);
        if (variantOpt.isEmpty()) {
            event.reply("Es wurde keine Variante mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }

        OptionMapping contentOption = event.getOption("content");
        OptionMapping embedOption = event.getOption("embed");
        OptionMapping titleOption = event.getOption("title");
        OptionMapping colorOption = event.getOption("color");
        OptionMapping imageOption = event.getOption("image");
        OptionMapping footerOption = event.getOption("footer");
        OptionMapping pingOption = event.getOption("ping");

        if (colorOption != null && !WelcomeService.isClearValue(colorOption.getAsString())
                && DiscordColors.parse(colorOption.getAsString()).isEmpty()) {
            event.reply("Die angegebene Farbe ist ungültig. Bitte nutze das Format #RRGGBB.").setEphemeral(true).queue();
            return;
        }

        if (imageOption != null && !WelcomeService.isClearValue(imageOption.getAsString())
                && !ImageUrlValidator.isValid(imageOption.getAsString())) {
            event.reply("Die Bild-URL ist ungültig. Erlaubt sind http(s)-Links auf png/jpg/jpeg/gif/webp.").setEphemeral(true).queue();
            return;
        }

        WelcomeVariantEdit edit = WelcomeVariantEdit.builder()
                .content(contentOption != null ? contentOption.getAsString() : null)
                .embed(embedOption != null ? embedOption.getAsBoolean() : null)
                .title(fieldEditFrom(titleOption))
                .color(fieldEditFrom(colorOption))
                .imageUrl(fieldEditFrom(imageOption))
                .footer(fieldEditFrom(footerOption))
                .ping(pingOption != null ? pingOption.getAsBoolean() : null)
                .build();

        service.editVariant(guild.getId(), id, edit);
        event.reply("Variante `" + id + "` wurde aktualisiert.").setEphemeral(true).queue();
        LOGGER.info("Edited welcome variant {} for guild {}", id, guild.getId());
        AuditLog.record(guild.getId(), event.getUser().getId(), "WELCOME_VARIANT_EDIT", "variantId=" + id);
    }

    private FieldEdit<String> fieldEditFrom(OptionMapping option) {
        if (option == null) {
            return FieldEdit.notProvided();
        }
        String value = option.getAsString();
        return WelcomeService.isClearValue(value) ? FieldEdit.clear() : FieldEdit.value(value);
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        String id = event.getOption("id").getAsString();

        boolean removed = service.removeVariant(guild.getId(), id);
        if (removed) {
            event.reply("Variante `" + id + "` wurde entfernt.").setEphemeral(true).queue();
            LOGGER.info("Removed welcome variant {} for guild {}", id, guild.getId());
            AuditLog.record(guild.getId(), event.getUser().getId(), "WELCOME_VARIANT_REMOVE", "variantId=" + id);
        } else {
            event.reply("Es wurde keine Variante mit dieser ID gefunden.").setEphemeral(true).queue();
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        List<WelcomeVariant> variants = service.getVariants(guild.getId());

        if (variants.isEmpty()) {
            event.reply("Es wurden noch keine Willkommensnachrichten-Varianten erstellt. Nutze `/welcome add` um eine zu erstellen.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Willkommensnachrichten-Varianten");
        eb.setColor(DiscordColors.brand());

        for (WelcomeVariant variant : variants) {
            String preview = variant.getContent() != null && variant.getContent().length() > previewMaxLength
                    ? variant.getContent().substring(0, previewMaxLength) + "..."
                    : variant.getContent();
            String value = "Typ: " + (variant.isEmbed() ? "Embed" : "Text") +
                    (variant.getTitle() != null ? "\nTitel: " + variant.getTitle() : "") +
                    "\nInhalt: " + preview +
                    "\nPing: " + (variant.isPing() ? "Ja" : "Nein");
            eb.addField("ID: " + variant.getId(), value, false);
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleTest(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        OptionMapping idOption = event.getOption("id");

        List<WelcomeVariant> variants = service.getVariants(guild.getId());
        if (variants.isEmpty()) {
            event.reply("Es wurden noch keine Willkommensnachrichten-Varianten erstellt.").setEphemeral(true).queue();
            return;
        }

        WelcomeVariant variant;
        if (idOption != null) {
            Optional<WelcomeVariant> variantOpt = service.getVariant(guild.getId(), idOption.getAsString());
            if (variantOpt.isEmpty()) {
                event.reply("Es wurde keine Variante mit dieser ID gefunden.").setEphemeral(true).queue();
                return;
            }
            variant = variantOpt.get();
        } else {
            variant = variants.get(ThreadLocalRandom.current().nextInt(variants.size()));
        }

        Member member = event.getMember();

        TextChannel welcomeChannel = service.getSettings(guild.getId())
                .map(WelcomeSettings::getChannelId)
                .map(channelId -> event.getJDA().getChannelById(TextChannel.class, channelId))
                .orElse(null);
        if (welcomeChannel == null) {
            welcomeChannel = event.getChannel().asTextChannel();
        }

        RenderedMessage built = WelcomeMessageFactory.build(variant, member, guild, welcomeChannel);

        event.deferReply(true).queue();

        String prefix = "**Vorschau** (Variante `" + variant.getId() + "`):\n";
        String body = built.content() != null ? built.content() : "";
        WebhookMessageCreateAction<?> action = event.getHook().sendMessage(prefix + body);
        if (built.embed() != null) {
            action = action.addEmbeds(built.embed());
        }
        action.queue();
    }

    private void handlePlaceholders(SlashCommandInteractionEvent event) {
        event.replyEmbeds(PlaceholderCatalog.buildOverviewEmbed()).setEphemeral(true).queue();
    }
}
