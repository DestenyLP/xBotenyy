package de.destenylp.xBotenyy.discordbot.commands;

import de.destenylp.xBotenyy.common.moderation.TwitchRoleSyncStatus;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.core.AbstractGuildCommand;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationPermissionGuard;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationRoleSettings;
import de.destenylp.xBotenyy.discordbot.moderation.ModerationRoleSettingsRepository;
import de.destenylp.xBotenyy.discordbot.util.DiscordColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class ModerationRolesCommand extends AbstractGuildCommand {
    private final ModerationRoleSettingsRepository repository;

    public ModerationRolesCommand(ModerationRoleSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    public CommandData getCommandData() {
        OptionData roleOption = new OptionData(OptionType.ROLE, "role", "Rolle", true);
        return Commands.slash("modroles", "Konfiguriert Straf- und Moderator-Rollen")
                .addSubcommands(
                        new SubcommandData("warn-role", "Rolle fuer verwarnte Mitglieder").addOptions(roleOption),
                        new SubcommandData("mute-role", "Rolle fuer Mitglieder im Timeout").addOptions(roleOption),
                        new SubcommandData("ban-role", "Rolle fuer gebannte/markierte Mitglieder").addOptions(roleOption),
                        new SubcommandData("add-moderator", "Fuegt eine Moderator-Rolle hinzu").addOptions(roleOption),
                        new SubcommandData("remove-moderator", "Entfernt eine Moderator-Rolle").addOptions(roleOption),
                        new SubcommandData("add-admin", "Fuegt eine Admin-Rolle hinzu").addOptions(roleOption),
                        new SubcommandData("remove-admin", "Entfernt eine Admin-Rolle").addOptions(roleOption),
                        new SubcommandData("sync-role", "Verknuepft einen Twitch-Status mit einer Discord-Rolle")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "status", "Twitch-Status", true)
                                                .addChoice("Subscriber", "SUBSCRIBER")
                                                .addChoice("VIP", "VIP")
                                                .addChoice("Moderator", "MODERATOR")
                                                .addChoice("Broadcaster", "BROADCASTER"),
                                        new OptionData(OptionType.ROLE, "role", "Discord-Rolle (leer = Zuordnung entfernen)", false)),
                        new SubcommandData("status", "Zeigt die aktuelle Konfiguration")
                );
    }

    @Override
    protected void executeInGuild(SlashCommandInteractionEvent event, Guild guild, String subcommand) {
        if (!ModerationPermissionGuard.requireAdmin(event, repository.getOrEmpty(guild.getId()))) {
            return;
        }

        switch (subcommand) {
            case "warn-role" -> {
                Role role = event.getOption("role").getAsRole();
                repository.setWarnRole(guild.getId(), role.getId());
                event.reply("Warn-Rolle gesetzt: " + role.getAsMention()).setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_WARN", "role=" + role.getId());
            }
            case "mute-role" -> {
                Role role = event.getOption("role").getAsRole();
                repository.setMuteRole(guild.getId(), role.getId());
                event.reply("Mute-Rolle gesetzt: " + role.getAsMention()).setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_MUTE", "role=" + role.getId());
            }
            case "ban-role" -> {
                Role role = event.getOption("role").getAsRole();
                repository.setBanRole(guild.getId(), role.getId());
                event.reply("Ban-Rolle gesetzt: " + role.getAsMention()).setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_BAN", "role=" + role.getId());
            }
            case "add-moderator" -> {
                Role role = event.getOption("role").getAsRole();
                repository.addModeratorRole(guild.getId(), role.getId());
                event.reply(role.getAsMention() + " darf jetzt `/mod`-Befehle nutzen.").setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_ADD_MOD", "role=" + role.getId());
            }
            case "remove-moderator" -> {
                Role role = event.getOption("role").getAsRole();
                repository.removeModeratorRole(guild.getId(), role.getId());
                event.reply(role.getAsMention() + " wurde als Moderator-Rolle entfernt.").setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_REMOVE_MOD", "role=" + role.getId());
            }
            case "add-admin" -> {
                Role role = event.getOption("role").getAsRole();
                repository.addAdminRole(guild.getId(), role.getId());
                event.reply(role.getAsMention() + " darf jetzt auch `/modroles` nutzen.").setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_ADD_ADMIN", "role=" + role.getId());
            }
            case "remove-admin" -> {
                Role role = event.getOption("role").getAsRole();
                repository.removeAdminRole(guild.getId(), role.getId());
                event.reply(role.getAsMention() + " wurde als Admin-Rolle entfernt.").setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_REMOVE_ADMIN", "role=" + role.getId());
            }
            case "sync-role" -> {
                TwitchRoleSyncStatus status = TwitchRoleSyncStatus.valueOf(event.getOption("status").getAsString());
                Role role = event.getOption("role") != null ? event.getOption("role").getAsRole() : null;
                repository.setSyncRole(guild.getId(), status, role != null ? role.getId() : null);
                String reply = role != null
                        ? "Twitch-Status **" + status.name() + "** wird jetzt mit " + role.getAsMention() + " synchronisiert."
                        : "Sync-Zuordnung fuer **" + status.name() + "** entfernt.";
                event.reply(reply).setEphemeral(true).queue();
                AuditLog.record(guild.getId(), event.getUser().getId(), "MODROLES_SYNC_ROLE",
                        "status=" + status + " role=" + (role != null ? role.getId() : "none"));
            }
            case "status" -> handleStatus(event, guild);
            default -> replyUnknownSubcommand(event);
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event, Guild guild) {
        ModerationRoleSettings settings = repository.getOrEmpty(guild.getId());
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(DiscordColors.brand());
        eb.setTitle("\uD83D\uDEE1\uFE0F Moderations-Rollen – " + guild.getName());
        eb.addField("Warn-Rolle", mention(settings.warnRoleId()), true);
        eb.addField("Mute-Rolle", mention(settings.muteRoleId()), true);
        eb.addField("Ban-Rolle", mention(settings.banRoleId()), true);
        eb.addField("Moderator-Rollen", mentionList(settings.moderatorRoleIds()), false);
        eb.addField("Admin-Rollen", mentionList(settings.adminRoleIds()), false);
        eb.addField("Sync: Subscriber", mention(settings.syncSubscriberRoleId()), true);
        eb.addField("Sync: VIP", mention(settings.syncVipRoleId()), true);
        eb.addField("Sync: Moderator", mention(settings.syncModeratorRoleId()), true);
        eb.addField("Sync: Broadcaster", mention(settings.syncBroadcasterRoleId()), true);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private String mention(String roleId) {
        return roleId == null || roleId.isBlank() ? "nicht gesetzt" : "<@&" + roleId + ">";
    }

    private String mentionList(java.util.List<String> roleIds) {
        return roleIds.isEmpty() ? "keine" : roleIds.stream().map(id -> "<@&" + id + ">")
                .reduce((a, b) -> a + ", " + b).orElse("keine");
    }
}
