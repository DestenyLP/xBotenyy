package de.destenylp.xBotenyy.discordbot.moderation;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
public final class ModerationPermissionGuard {
    private ModerationPermissionGuard() {
    }
    public static boolean requireModerator(SlashCommandInteractionEvent event, ModerationRoleSettings settings) {
        return require(event, settings, false);
    }
    public static boolean requireAdmin(SlashCommandInteractionEvent event, ModerationRoleSettings settings) {
        return require(event, settings, true);
    }
    private static boolean require(SlashCommandInteractionEvent event, ModerationRoleSettings settings, boolean adminOnly) {
        Member member = event.getMember();
        if (member == null) {
            event.reply("Dieser Befehl kann nur auf einem Server genutzt werden.").setEphemeral(true).queue();
            return false;
        }
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        if (!adminOnly && member.hasPermission(Permission.MODERATE_MEMBERS)) {
            return true;
        }
        boolean hasAdminRole = member.getRoles().stream()
                .anyMatch(role -> settings.adminRoleIds().contains(role.getId()));
        if (hasAdminRole) {
            return true;
        }
        if (!adminOnly) {
            boolean hasModeratorRole = member.getRoles().stream()
                    .anyMatch(role -> settings.moderatorRoleIds().contains(role.getId()));
            if (hasModeratorRole) {
                return true;
            }
        }
        event.reply("Dir fehlt die Berechtigung fuer diesen Befehl. Frage einen Admin nach der passenden Moderator-Rolle "
                + "(`/modroles`).").setEphemeral(true).queue();
        return false;
    }
}
