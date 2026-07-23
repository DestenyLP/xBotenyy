package de.destenylp.xBotenyy.discordbot.reactionroles;

import de.destenylp.xBotenyy.discordbot.core.GuildService;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ReactionRoleService implements GuildService {
    private static volatile int maxButtonsPerMessage = 25;

    public static void configureMaxButtonsPerMessage(int value) {
        maxButtonsPerMessage = Math.max(1, Math.min(25, value));
    }

    public static int getMaxButtonsPerMessage() {
        return maxButtonsPerMessage;
    }

    private final ReactionRoleRepository manager;

    public ReactionRoleService(ReactionRoleRepository manager) {
        this.manager = manager;
    }

    @Override
    public String getServiceName() {
        return "Reaction Roles";
    }

    public ReactionRoleMessage createMessage(String guildId, String channelId, String messageId) {
        return manager.createMessage(guildId, channelId, messageId);
    }

    public ReactionRoleMessage getOrCreateMessage(String guildId, String channelId, String messageId) {
        return manager.getOrCreateMessage(guildId, channelId, messageId);
    }

    public Optional<ReactionRoleMessage> findMessage(String guildId, String messageId) {
        return manager.getMessage(guildId, messageId);
    }

    public Optional<ReactionRoleType> parseType(String raw) {
        try {
            return Optional.of(ReactionRoleType.valueOf(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public ButtonStyle parseButtonStyle(String style) {
        if (style == null) {
            return ButtonStyle.PRIMARY;
        }
        return switch (style.toLowerCase()) {
            case "secondary" -> ButtonStyle.SECONDARY;
            case "success" -> ButtonStyle.SUCCESS;
            case "danger" -> ButtonStyle.DANGER;
            default -> ButtonStyle.PRIMARY;
        };
    }

    public List<ReactionRoleEntry> buttonEntriesOf(ReactionRoleMessage message) {
        return message.getEntries().stream()
                .filter(entry -> entry.getType() == ReactionRoleType.BUTTON)
                .toList();
    }

    public boolean canAddButton(List<ReactionRoleEntry> currentButtonEntries) {
        return currentButtonEntries.size() < maxButtonsPerMessage;
    }

    public String buildButtonComponentId(String messageId, String roleId) {
        return "reactionrole:" + messageId + ":" + roleId;
    }

    public ReactionRoleEntry recordReactionEntry(String guildId, String messageId, String roleId, String emojiFormatted) {
        ReactionRoleEntry entry = ReactionRoleEntry.builder()
                .roleId(roleId)
                .type(ReactionRoleType.REACTION)
                .emoji(emojiFormatted)
                .build();
        manager.addEntry(guildId, messageId, entry);
        return entry;
    }

    public ReactionRoleEntry recordButtonEntry(String guildId, String messageId, String componentId, String roleId,
                                                String emojiFormatted, String label, ButtonStyle style) {
        ReactionRoleEntry entry = ReactionRoleEntry.builder()
                .componentId(componentId)
                .roleId(roleId)
                .type(ReactionRoleType.BUTTON)
                .emoji(emojiFormatted)
                .buttonLabel(label)
                .buttonStyle(style.name())
                .build();
        manager.addEntry(guildId, messageId, entry);
        return entry;
    }

    public Optional<ReactionRoleEntry> findByIdentifier(ReactionRoleMessage message, String identifier) {
        return message.getEntries().stream()
                .filter(entry -> entry.getRoleId().equals(identifier) || (entry.getEmoji() != null && entry.getEmoji().equals(identifier)))
                .findFirst();
    }

    public boolean removeEntry(String guildId, String messageId, String identifier) {
        return manager.removeEntry(guildId, messageId, identifier);
    }

    public List<ActionRow> buildButtonRows(List<ReactionRoleEntry> entries, Function<String, String> roleNameResolver) {
        List<Button> buttons = new ArrayList<>();
        for (ReactionRoleEntry entry : entries) {
            String label = entry.getButtonLabel() != null ? entry.getButtonLabel() : roleNameResolver.apply(entry.getRoleId());
            ButtonStyle style = parseButtonStyle(entry.getButtonStyle());
            Button button = Button.of(style, entry.getComponentId(), label);
            if (entry.getEmoji() != null) {
                try {
                    button = button.withEmoji(Emoji.fromFormatted(entry.getEmoji()));
                } catch (RuntimeException ignored) {
                }
            }
            buttons.add(button);
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) {
            rows.add(ActionRow.of(buttons.subList(i, Math.min(i + 5, buttons.size()))));
        }
        return rows;
    }
}
