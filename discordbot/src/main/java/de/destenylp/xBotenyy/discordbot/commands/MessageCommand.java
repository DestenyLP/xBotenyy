package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.messaging.MessageDispatcher;
import de.destenylp.xBotenyy.discordbot.messaging.MessageRenderer;
import de.destenylp.xBotenyy.discordbot.messaging.MessageTemplate;
import de.destenylp.xBotenyy.discordbot.messaging.RenderedMessage;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderCatalog;
import de.destenylp.xBotenyy.discordbot.placeholders.PlaceholderContext;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.discordbot.util.ImageUrlValidator;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MessageCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCommand.class);

    @Override
    public CommandData getCommandData() {
        return Commands.slash("message", "Erstellt Nachrichten mit Platzhaltern, z.B. als Basis für Reaction Roles")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommands(
                        new SubcommandData("create", "Sendet eine neue Nachricht in einen Kanal")
                                .addOption(OptionType.STRING, "content", "Nachrichtentext (Platzhalter: siehe /message placeholders, \\n für Zeilenumbruch)", true)
                                .addOption(OptionType.BOOLEAN, "embed", "Als Embed senden?", true)
                                .addOption(OptionType.STRING, "title", "Titel (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "color", "Hex Farbe für das Embed, z.B. #5865F2", false)
                                .addOption(OptionType.STRING, "image", "Bild-URL (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "footer", "Footer-Text (nur bei Embed)", false)
                                .addOption(OptionType.CHANNEL, "channel", "Zielkanal (Standard: aktueller Kanal)", false),
                        new SubcommandData("edit", "Bearbeitet eine vom Bot gesendete Nachricht")
                                .addOption(OptionType.STRING, "message-id", "ID der zu bearbeitenden Nachricht", true)
                                .addOption(OptionType.STRING, "content", "Neuer Nachrichtentext (Platzhalter: siehe /message placeholders, \\n für Zeilenumbruch)", true)
                                .addOption(OptionType.BOOLEAN, "embed", "Als Embed anzeigen?", true)
                                .addOption(OptionType.STRING, "title", "Titel (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "color", "Hex Farbe für das Embed, z.B. #5865F2", false)
                                .addOption(OptionType.STRING, "image", "Bild-URL (nur bei Embed)", false)
                                .addOption(OptionType.STRING, "footer", "Footer-Text (nur bei Embed)", false)
                                .addOption(OptionType.CHANNEL, "channel", "Kanal der Nachricht (Standard: aktueller Kanal)", false),
                        new SubcommandData("placeholders", "Zeigt alle verfügbaren Platzhalter")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (("create".equals(subcommand) || "edit".equals(subcommand)) && !PermissionGuard.requireAdministrator(event)) {
            return;
        }

        switch (subcommand) {
            case "create" -> handleCreate(event);
            case "edit" -> handleEdit(event);
            case "placeholders" -> event.replyEmbeds(PlaceholderCatalog.buildOverviewEmbed()).setEphemeral(true).queue();
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleCreate(SlashCommandInteractionEvent event) {
        String content = event.getOption("content").getAsString();
        boolean embed = event.getOption("embed").getAsBoolean();
        OptionMapping titleOption = event.getOption("title");
        OptionMapping colorOption = event.getOption("color");
        OptionMapping imageOption = event.getOption("image");
        OptionMapping footerOption = event.getOption("footer");
        OptionMapping channelOption = event.getOption("channel");

        String color = colorOption != null ? colorOption.getAsString() : null;
        if (color != null && DiscordColors.parse(color).isEmpty()) {
            event.reply("Die angegebene Farbe ist ungültig. Bitte nutze das Format #RRGGBB.").setEphemeral(true).queue();
            return;
        }

        String image = imageOption != null ? imageOption.getAsString() : null;
        if (!ImageUrlValidator.isValid(image)) {
            event.reply("Die Bild-URL ist ungültig. Erlaubt sind http(s)-Links auf png/jpg/jpeg/gif/webp.").setEphemeral(true).queue();
            return;
        }

        TextChannel targetChannel = channelOption != null
                ? channelOption.getAsChannel().asTextChannel()
                : event.getChannel().asTextChannel();

        MessageTemplate template = MessageTemplate.builder()
                .embed(embed)
                .title(titleOption != null ? titleOption.getAsString() : null)
                .content(content)
                .color(color)
                .imageUrl(image)
                .footer(footerOption != null ? footerOption.getAsString() : null)
                .build();

        Guild guild = event.getGuild();
        PlaceholderContext context = PlaceholderContext.of(event.getMember(), guild, targetChannel);

        Optional<String> validationError = MessageRenderer.validate(template, context);
        if (validationError.isPresent()) {
            event.reply(validationError.get()).setEphemeral(true).queue();
            return;
        }

        RenderedMessage rendered = MessageRenderer.render(template, context);

        Optional<MessageCreateAction> actionOpt = MessageDispatcher.prepare(targetChannel, rendered);
        if (actionOpt.isEmpty()) {
            event.reply("Die Nachricht enthält keinen Inhalt.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        actionOpt.get().queue(
                message -> onCreated(event, targetChannel, message),
                failure -> onCreateFailed(event, failure));
    }

    private void onCreated(SlashCommandInteractionEvent event, TextChannel channel, Message message) {
        event.getHook().sendMessage("Nachricht gesendet! Nachrichten-ID: `" + message.getId() + "`\n" +
                "Diese ID kannst du z.B. mit `/reactionrole add message-id:" + message.getId() + "` weiterverwenden.").queue();
        LOGGER.info("Message {} created in channel {}", message.getId(), channel.getId());
    }

    private void onCreateFailed(SlashCommandInteractionEvent event, Throwable failure) {
        LOGGER.error("Failed to send message: {}", failure.getMessage());
        event.getHook().sendMessage("Die Nachricht konnte nicht gesendet werden.").queue();
    }

    private void handleEdit(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("message-id").getAsString();
        String content = event.getOption("content").getAsString();
        boolean embed = event.getOption("embed").getAsBoolean();
        OptionMapping titleOption = event.getOption("title");
        OptionMapping colorOption = event.getOption("color");
        OptionMapping imageOption = event.getOption("image");
        OptionMapping footerOption = event.getOption("footer");
        OptionMapping channelOption = event.getOption("channel");

        String color = colorOption != null ? colorOption.getAsString() : null;
        if (color != null && DiscordColors.parse(color).isEmpty()) {
            event.reply("Die angegebene Farbe ist ungültig. Bitte nutze das Format #RRGGBB.").setEphemeral(true).queue();
            return;
        }

        String image = imageOption != null ? imageOption.getAsString() : null;
        if (!ImageUrlValidator.isValid(image)) {
            event.reply("Die Bild-URL ist ungültig. Erlaubt sind http(s)-Links auf png/jpg/jpeg/gif/webp.").setEphemeral(true).queue();
            return;
        }

        TextChannel targetChannel = channelOption != null
                ? channelOption.getAsChannel().asTextChannel()
                : event.getChannel().asTextChannel();

        MessageTemplate template = MessageTemplate.builder()
                .embed(embed)
                .title(titleOption != null ? titleOption.getAsString() : null)
                .content(content)
                .color(color)
                .imageUrl(image)
                .footer(footerOption != null ? footerOption.getAsString() : null)
                .build();

        Guild guild = event.getGuild();
        PlaceholderContext context = PlaceholderContext.of(event.getMember(), guild, targetChannel);

        Optional<String> validationError = MessageRenderer.validate(template, context);
        if (validationError.isPresent()) {
            event.reply(validationError.get()).setEphemeral(true).queue();
            return;
        }

        RenderedMessage rendered = MessageRenderer.render(template, context);

        Optional<MessageEditAction> actionOpt = MessageDispatcher.prepareEdit(targetChannel, messageId, rendered);
        if (actionOpt.isEmpty()) {
            event.reply("Die Nachricht enthält keinen Inhalt.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        targetChannel.retrieveMessageById(messageId).queue(
                existing -> {
                    if (!existing.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
                        event.getHook().sendMessage("Diese Nachricht wurde nicht von mir gesendet und kann daher nicht bearbeitet werden.").queue();
                        return;
                    }

                    actionOpt.get().queue(
                            message -> onEdited(event, targetChannel, message),
                            failure -> onEditFailed(event, messageId, failure));
                },
                failure -> onRetrieveForEditFailed(event, messageId, failure));
    }

    private void onEdited(SlashCommandInteractionEvent event, TextChannel channel, Message message) {
        event.getHook().sendMessage("Nachricht bearbeitet! Nachrichten-ID: `" + message.getId() + "`").queue();
        LOGGER.info("Message {} edited in channel {}", message.getId(), channel.getId());
    }

    private void onEditFailed(SlashCommandInteractionEvent event, String messageId, Throwable failure) {
        LOGGER.error("Failed to edit message {}: {}", messageId, failure.getMessage());
        event.getHook().sendMessage("Die Nachricht konnte nicht bearbeitet werden.").queue();
    }

    private void onRetrieveForEditFailed(SlashCommandInteractionEvent event, String messageId, Throwable failure) {
        LOGGER.error("Failed to retrieve message {} for edit: {}", messageId, failure.getMessage());
        event.getHook().sendMessage("Die Nachricht konnte nicht gefunden werden (falscher Kanal oder falsche ID?).").queue();
    }
}
