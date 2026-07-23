package de.destenylp.xBotenyy.discordbot.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public final class PermissionGuard {
    private PermissionGuard() {
    }

    public static boolean requireManageServer(SlashCommandInteractionEvent event) {
        return require(event, Permission.MANAGE_SERVER, "Server verwalten");
    }

    public static boolean requireAdministrator(SlashCommandInteractionEvent event) {
        return require(event, Permission.ADMINISTRATOR, "Administrator");
    }

    private static boolean require(SlashCommandInteractionEvent event, Permission permission, String label) {
        Member member = event.getMember();
        if (member == null || !member.hasPermission(permission)) {
            event.reply("Dir fehlt die Berechtigung `" + label + "` fuer diesen Befehl.").setEphemeral(true).queue();
            return false;
        }
        return true;
    }
}
