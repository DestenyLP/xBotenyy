package de.destenylp.xBotenyy.twitchbot.commands.impl;

import de.destenylp.xBotenyy.common.automod.AutomodSettings;
import de.destenylp.xBotenyy.common.commands.CommandPermission;
import de.destenylp.xBotenyy.twitchbot.commands.AbstractTwitchCommand;
import de.destenylp.xBotenyy.twitchbot.commands.TwitchCommandContext;

public class AutomodStatusCommand extends AbstractTwitchCommand {
    public AutomodStatusCommand() {
        super("automod", "Zeigt den aktuellen AutoMod-Status.", java.util.List.of("mod"),
                CommandPermission.MODERATOR, 5);
    }

    @Override
    public void execute(TwitchCommandContext context) {
        AutomodSettings settings = context.services().automodEngine().getSettings();
        boolean aiActive = context.services().automodEngine().isAiAvailable() && settings.getAiFilter().enabled();

        StringBuilder builder = new StringBuilder();
        builder.append("AutoMod ist ").append(settings.isEnabled() ? "aktiviert" : "deaktiviert").append(". ");
        builder.append("Aktive Filter: ");
        java.util.List<String> active = new java.util.ArrayList<>();
        if (settings.getWordFilter().isEnabled()) active.add("Woerter");
        if (settings.getInviteFilter().enabled()) active.add("Invites");
        if (settings.getCapsFilter().enabled()) active.add("Caps");
        if (settings.getSpamFilter().enabled()) active.add("Spam");
        if (settings.getDuplicateFilter().enabled()) active.add("Duplikate");
        if (settings.getLinkFilter().enabled()) active.add("Links");
        if (settings.getMentionFilter().enabled()) active.add("Mentions");
        if (aiActive) active.add("KI-Moderation");
        builder.append(active.isEmpty() ? "keine" : String.join(", ", active)).append(".");

        context.reply(builder.toString());
    }
}
