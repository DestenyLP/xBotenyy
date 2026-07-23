package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleEntry;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleMessage;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleService;
import de.destenylp.xBotenyy.discordbot.reactionroles.ReactionRoleType;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.util.PermissionGuard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReactionRoleCommand extends AbstractGuildCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactionRoleCommand.class);

    private final ReactionRoleService service;

    public ReactionRoleCommand(ReactionRoleService service) {
        this.service = service;
    }

    @Override
    public CommandData getCommandData() {
        return Commands.slash("reactionrole", "Verwalte Reaction Roles")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER))
                .addSubcommands(
                        new SubcommandData("add", "Fügt eine Rollenzuweisung zu einer Nachricht hinzu (Nachricht z.B. mit /message create erstellen)")
                                .addOption(OptionType.STRING, "message-id", "ID der Nachricht, die zur Reaction-Role Nachricht werden soll", true)
                                .addOption(OptionType.ROLE, "role", "Rolle die vergeben werden soll", true)
                                .addOptions(new OptionData(OptionType.STRING, "type", "Reaction oder Button", true)
                                        .addChoice("Reaction", "REACTION")
                                        .addChoice("Button", "BUTTON"))
                                .addOption(OptionType.STRING, "emoji", "Emoji für die Zuweisung", true)
                                .addOption(OptionType.CHANNEL, "channel", "Kanal der Nachricht (Standard: aktueller Kanal)", false)
                                .addOption(OptionType.STRING, "button-label", "Beschriftung des Buttons (nur bei Button)", false)
                                .addOptions(new OptionData(OptionType.STRING, "button-style", "Button-Stil (nur bei Button)", false)
                                        .addChoice("Primary", "primary")
                                        .addChoice("Secondary", "secondary")
                                        .addChoice("Success", "success")
                                        .addChoice("Danger", "danger")),
                        new SubcommandData("remove", "Entfernt eine Rollenzuweisung")
                                .addOption(OptionType.STRING, "message-id", "ID der Reaction-Role Nachricht", true)
                                .addOption(OptionType.STRING, "identifier", "Emoji oder Rollen-ID der Zuweisung", true),
                        new SubcommandData("list", "Zeigt alle Rollenzuweisungen einer Nachricht")
                                .addOption(OptionType.STRING, "message-id", "ID der Reaction-Role Nachricht", true)
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!PermissionGuard.requireManageServer(event)) {
            return;
        }

        switch (subcommand) {
            case "add" -> handleAdd(event);
            case "remove" -> handleRemove(event);
            case "list" -> handleList(event);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleAdd(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("message-id").getAsString();
        Role role = event.getOption("role").getAsRole();
        String typeRaw = event.getOption("type").getAsString();
        String emoji = event.getOption("emoji").getAsString();
        OptionMapping channelOption = event.getOption("channel");
        OptionMapping labelOption = event.getOption("button-label");
        OptionMapping styleOption = event.getOption("button-style");

        Optional<ReactionRoleType> typeOpt = service.parseType(typeRaw);
        if (typeOpt.isEmpty()) {
            event.reply("Ungültiger Typ.").setEphemeral(true).queue();
            return;
        }
        ReactionRoleType type = typeOpt.get();

        if (type == ReactionRoleType.BUTTON && labelOption == null) {
            event.reply("Für einen Button muss eine Beschriftung (button-label) angegeben werden.").setEphemeral(true).queue();
            return;
        }

        Emoji parsedEmoji;
        try {
            parsedEmoji = Emoji.fromFormatted(emoji);
        } catch (RuntimeException e) {
            event.reply("Das Emoji konnte nicht gelesen werden.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = channelOption != null
                ? channelOption.getAsChannel().asTextChannel()
                : event.getChannel().asTextChannel();

        event.deferReply(true).queue();

        String label = labelOption != null ? labelOption.getAsString() : null;
        String styleRaw = styleOption != null ? styleOption.getAsString() : null;

        channel.retrieveMessageById(messageId).queue(message -> {
            ReactionRoleMessage rrMessage = service.getOrCreateMessage(event.getGuild().getId(), channel.getId(), messageId);
            if (type == ReactionRoleType.REACTION) {
                addReactionRole(event, message, rrMessage, role, parsedEmoji);
            } else {
                addButtonRole(event, message, rrMessage, role, parsedEmoji, label, styleRaw);
            }
        }, failure -> {
            LOGGER.error("Failed to retrieve message {}: {}", messageId, failure.getMessage());
            event.getHook().sendMessage("Die Nachricht konnte nicht gefunden werden (falscher Kanal oder falsche ID?).").queue();
        });
    }

    private void addReactionRole(SlashCommandInteractionEvent event, Message message, ReactionRoleMessage rrMessage,
                                  Role role, Emoji emoji) {
        message.addReaction(emoji).queue(success -> {
            service.recordReactionEntry(event.getGuild().getId(), rrMessage.getMessageId(), role.getId(), emoji.getFormatted());
            event.getHook().sendMessage("Reaction-Role hinzugefügt: " + emoji.getFormatted() + " -> " + role.getAsMention()).queue();
            LOGGER.info("Added reaction role mapping {} -> {} on message {}", emoji.getFormatted(), role.getId(), rrMessage.getMessageId());
            AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "REACTIONROLE_ADD",
                    "message=" + rrMessage.getMessageId() + " role=" + role.getId() + " emoji=" + emoji.getFormatted());
        }, failure -> {
            LOGGER.error("Failed to add reaction to message {}: {}", rrMessage.getMessageId(), failure.getMessage());
            event.getHook().sendMessage("Die Reaction konnte nicht hinzugefügt werden.").queue();
        });
    }

    private void addButtonRole(SlashCommandInteractionEvent event, Message message, ReactionRoleMessage rrMessage,
                                Role role, Emoji emoji, String label, String styleRaw) {
        ButtonStyle style = service.parseButtonStyle(styleRaw);
        String componentId = service.buildButtonComponentId(rrMessage.getMessageId(), role.getId());

        List<ReactionRoleEntry> currentButtons = service.buttonEntriesOf(rrMessage);
        if (!service.canAddButton(currentButtons)) {
            event.getHook().sendMessage("Es können maximal " + ReactionRoleService.getMaxButtonsPerMessage() + " Buttons pro Nachricht vergeben werden.").queue();
            return;
        }

        List<ReactionRoleEntry> preview = new ArrayList<>(currentButtons);
        preview.add(ReactionRoleEntry.builder()
                .componentId(componentId)
                .roleId(role.getId())
                .type(ReactionRoleType.BUTTON)
                .emoji(emoji.getFormatted())
                .buttonLabel(label)
                .buttonStyle(style.name())
                .build());

        Guild guild = event.getGuild();
        List<ActionRow> rows = service.buildButtonRows(preview, roleId -> resolveRoleName(guild, roleId));

        message.editMessageComponents(rows).queue(success -> {
            service.recordButtonEntry(guild.getId(), rrMessage.getMessageId(), componentId, role.getId(), emoji.getFormatted(), label, style);
            event.getHook().sendMessage("Button-Role hinzugefügt: " + label + " -> " + role.getAsMention()).queue();
            LOGGER.info("Added button role mapping {} -> {} on message {}", componentId, role.getId(), rrMessage.getMessageId());
            AuditLog.record(guild.getId(), event.getUser().getId(), "REACTIONROLE_ADD",
                    "message=" + rrMessage.getMessageId() + " role=" + role.getId() + " type=BUTTON");
        }, failure -> {
            LOGGER.error("Failed to edit components on message {}: {}", rrMessage.getMessageId(), failure.getMessage());
            event.getHook().sendMessage("Der Button konnte nicht hinzugefügt werden.").queue();
        });
    }

    private String resolveRoleName(Guild guild, String roleId) {
        Role role = guild.getRoleById(roleId);
        return role != null ? role.getName() : "Rolle";
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("message-id").getAsString();
        String identifier = event.getOption("identifier").getAsString();

        Optional<ReactionRoleMessage> rrMessageOpt = service.findMessage(event.getGuild().getId(), messageId);
        if (rrMessageOpt.isEmpty()) {
            event.reply("Es wurde keine Reaction-Role Nachricht mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }

        Optional<ReactionRoleEntry> entryOpt = service.findByIdentifier(rrMessageOpt.get(), identifier);
        if (entryOpt.isEmpty()) {
            event.reply("Für diese Nachricht wurde keine passende Zuweisung gefunden.").setEphemeral(true).queue();
            return;
        }

        ReactionRoleEntry entry = entryOpt.get();
        event.deferReply(true).queue();

        TextChannel channel = event.getJDA().getChannelById(TextChannel.class, rrMessageOpt.get().getChannelId());
        service.removeEntry(event.getGuild().getId(), messageId, identifier);
        AuditLog.record(event.getGuild().getId(), event.getUser().getId(), "REACTIONROLE_REMOVE",
                "message=" + messageId + " identifier=" + identifier);

        if (channel == null) {
            event.getHook().sendMessage("Zuweisung entfernt, der Kanal konnte jedoch nicht aktualisiert werden.").queue();
            return;
        }

        if (entry.getType() == ReactionRoleType.REACTION) {
            channel.retrieveMessageById(messageId).queue(message ->
                    message.clearReactions(Emoji.fromFormatted(entry.getEmoji())).queue(
                            success -> event.getHook().sendMessage("Zuweisung entfernt.").queue(),
                            failure -> event.getHook().sendMessage("Zuweisung entfernt, die Reaction konnte jedoch nicht gelöscht werden.").queue()
                    ), failure -> event.getHook().sendMessage("Zuweisung entfernt.").queue());
        } else {
            List<ReactionRoleEntry> remainingButtons = service.buttonEntriesOf(rrMessageOpt.get());
            Guild guild = event.getGuild();
            List<ActionRow> rows = service.buildButtonRows(remainingButtons, roleId -> resolveRoleName(guild, roleId));

            channel.retrieveMessageById(messageId).queue(message ->
                    message.editMessageComponents(rows).queue(
                            success -> event.getHook().sendMessage("Zuweisung entfernt.").queue(),
                            failure -> event.getHook().sendMessage("Zuweisung entfernt, der Button konnte jedoch nicht gelöscht werden.").queue()),
                    failure -> event.getHook().sendMessage("Zuweisung entfernt.").queue());
        }
    }

    private void handleList(SlashCommandInteractionEvent event) {
        String messageId = event.getOption("message-id").getAsString();
        Optional<ReactionRoleMessage> rrMessageOpt = service.findMessage(event.getGuild().getId(), messageId);

        if (rrMessageOpt.isEmpty()) {
            event.reply("Es wurde keine Reaction-Role Nachricht mit dieser ID gefunden.").setEphemeral(true).queue();
            return;
        }

        List<ReactionRoleEntry> entries = rrMessageOpt.get().getEntries();
        if (entries.isEmpty()) {
            event.reply("Für diese Nachricht sind noch keine Rollenzuweisungen vorhanden.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Rollenzuweisungen für Nachricht " + messageId);
        eb.setColor(DiscordColors.brand());

        for (ReactionRoleEntry entry : entries) {
            String typeLabel = entry.getType() == ReactionRoleType.REACTION ? "Reaction" : "Button";
            String value = entry.getEmoji() != null ? entry.getEmoji() : "-";
            eb.addField("<@&" + entry.getRoleId() + "> (" + typeLabel + ")", value, true);
        }

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
}
