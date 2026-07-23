package de.destenylp.xBotenyy.discordbot.listeners;

import de.destenylp.xBotenyy.discordbot.reports.Report;
import de.destenylp.xBotenyy.discordbot.reports.ReportCategory;
import de.destenylp.xBotenyy.discordbot.reports.ReportEmbedFactory;
import de.destenylp.xBotenyy.discordbot.reports.ReportService;
import de.destenylp.xBotenyy.discordbot.reports.ReportSettings;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import de.destenylp.xBotenyy.common.util.AuditLog;
import de.destenylp.xBotenyy.discordbot.observability.BotMetrics;
import net.dv8tion.jda.api.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class ReportListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportListener.class);

    private final ReportService service;
    private final int shortFieldMaxLength;
    private final int longFieldMaxLength;

    public ReportListener(ReportService service, int shortFieldMaxLength, int longFieldMaxLength) {
        this.service = service;
        this.shortFieldMaxLength = shortFieldMaxLength;
        this.longFieldMaxLength = longFieldMaxLength;
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        try {
            handleStringSelectInteraction(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Report-Select-Interaktion {} in Guild {}: ",
                    event.getComponentId(), event.getGuild() != null ? event.getGuild().getId() : "unknown", e);
            replyGenericError(event);
        }
    }

    private void handleStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!"report:category".equals(event.getComponentId())) {
            return;
        }

        String key = event.getValues().isEmpty() ? null : event.getValues().get(0);
        Optional<ReportCategory> categoryOpt = ReportCategory.fromKey(key);
        if (categoryOpt.isEmpty()) {
            event.reply("Ungültige Kategorie.").setEphemeral(true).queue();
            return;
        }

        ReportCategory category = categoryOpt.get();

        TextInput subject = TextInput.create("subject", TextInputStyle.SHORT)
                .setPlaceholder("Kurze Zusammenfassung deines Anliegens")
                .setMaxLength(shortFieldMaxLength)
                .build();

        TextInput target = TextInput.create("target", TextInputStyle.SHORT)
                .setPlaceholder("z.B. Name des Mitglieds, Kanal oder Bereich")
                .setRequired(false)
                .setMaxLength(shortFieldMaxLength)
                .build();

        TextInput description = TextInput.create("description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Beschreibe dein Anliegen so genau wie möglich...")
                .setMaxLength(longFieldMaxLength)
                .build();

        TextInput evidence = TextInput.create("evidence", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Links, Zeitpunkte, Screenshots-Beschreibung, Zeugen...")
                .setRequired(false)
                .setMaxLength(longFieldMaxLength)
                .build();

        Modal modal = Modal.create("report:modal:" + category.getKey(), "Neuer Report: " + category.getLabel())
                .addComponents(
                        Label.of("Betreff", subject),
                        Label.of("Betroffene Person / Bereich (optional)", target),
                        Label.of("Beschreibung", description),
                        Label.of("Beweise / Kontext (optional)", evidence))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        try {
            handleModalInteraction(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Report-Modal-Interaktion {} in Guild {}: ",
                    event.getModalId(), event.getGuild() != null ? event.getGuild().getId() : "unknown", e);
            replyGenericError(event);
        }
    }

    private void handleModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (modalId.startsWith("report:modal:")) {
            handleReportSubmission(event, modalId.substring("report:modal:".length()));
        } else if (modalId.startsWith("report:resolvemodal:")) {
            handleResolveSubmission(event, modalId.substring("report:resolvemodal:".length()));
        } else if (modalId.startsWith("report:rejectmodal:")) {
            handleRejectSubmission(event, modalId.substring("report:rejectmodal:".length()));
        }
    }

    private void handleReportSubmission(ModalInteractionEvent event, String categoryKey) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            event.reply("Dieser Vorgang ist nur auf einem Server möglich.").setEphemeral(true).queue();
            return;
        }

        Optional<ReportCategory> categoryOpt = ReportCategory.fromKey(categoryKey);
        if (categoryOpt.isEmpty()) {
            event.reply("Ungültige Kategorie.").setEphemeral(true).queue();
            return;
        }

        ReportSettings settings = service.getSettings(guild.getId()).orElse(null);
        if (settings == null || settings.getChannelId() == null) {
            event.reply("Das Report-System wurde auf diesem Server noch nicht eingerichtet.").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getJDA().getChannelById(TextChannel.class, settings.getChannelId());
        if (channel == null) {
            event.reply("Der konfigurierte Report-Kanal existiert nicht mehr. Bitte informiere das Team.").setEphemeral(true).queue();
            return;
        }

        String subject = getValue(event, "subject");
        String target = getValue(event, "target");
        String description = getValue(event, "description");
        String evidence = getValue(event, "evidence");

        Report report = service.createReport(guild.getId(), member.getId(), member.getUser().getName(),
                categoryOpt.get(), subject, target, description, evidence);

        event.deferReply(true).queue();

        MessageCreateAction action = channel.sendMessageEmbeds(ReportEmbedFactory.buildChannelEmbed(report))
                .setComponents(ReportEmbedFactory.buildChannelComponents(report));

        if (settings.getNotifyRoleId() != null) {
            action = action.setContent("<@&" + settings.getNotifyRoleId() + ">");
        }

        action.queue(message -> {
            service.attachMessage(guild.getId(), report.getId(), channel.getId(), message.getId());
            event.getHook().sendMessage("Dein Report wurde erfolgreich eingereicht! Du kannst den Status jederzeit mit `/reports` einsehen.\nID: `" + report.getId() + "`")
                    .setEphemeral(true).queue();
            LOGGER.info("Report {} created by {} in guild {}", report.getId(), member.getId(), guild.getId());
            BotMetrics.incrementReportsCreated();
        }, failure -> {
            LOGGER.error("Failed to post report {} to channel {}: {}", report.getId(), channel.getId(), failure.getMessage());
            event.getHook().sendMessage("Dein Report wurde gespeichert, konnte aber nicht an das Team gesendet werden. Bitte informiere einen Admin.")
                    .setEphemeral(true).queue();
        });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        try {
            handleReportButtonInteraction(event);
        } catch (Exception e) {
            LOGGER.error("Unerwarteter Fehler bei Report-Button-Interaktion {} in Guild {}: ",
                    event.getComponentId(), event.getGuild() != null ? event.getGuild().getId() : "unknown", e);
            replyGenericError(event);
        }
    }

    private void handleReportButtonInteraction(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith("report:")) {
            return;
        }

        String[] parts = componentId.split(":");
        if (parts.length != 3) {
            return;
        }

        String action = parts[1];
        String reportId = parts[2];

        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        ReportSettings settings = service.getSettings(guild.getId()).orElse(null);
        Optional<Report> reportOpt = service.getReport(guild.getId(), reportId);
        if (reportOpt.isEmpty()) {
            event.reply("Dieser Report existiert nicht mehr.").setEphemeral(true).queue();
            return;
        }

        if (!ReportService.isModerator(member, settings)) {
            event.reply("Du bist nicht berechtigt, Reports zu bearbeiten.").setEphemeral(true).queue();
            return;
        }

        if (reportOpt.get().getStatus().isClosed()) {
            event.reply("Dieser Report wurde bereits abgeschlossen.").setEphemeral(true).queue();
            return;
        }

        switch (action) {
            case "claim" -> handleClaim(event, guild, member, reportId);
            case "resolve" -> handleResolvePrompt(event, reportId);
            case "reject" -> handleRejectPrompt(event, reportId);
            default -> {
            }
        }
    }

    private void handleClaim(ButtonInteractionEvent event, Guild guild, Member member, String reportId) {
        boolean success = service.claim(guild.getId(), reportId, member.getId(), member.getUser().getName());
        if (!success) {
            event.reply("Dieser Report konnte nicht übernommen werden.").setEphemeral(true).queue();
            return;
        }

        Report report = service.getReport(guild.getId(), reportId).orElseThrow();
        event.editMessageEmbeds(ReportEmbedFactory.buildChannelEmbed(report))
                .setComponents(ReportEmbedFactory.buildChannelComponents(report))
                .queue();
        LOGGER.info("Report {} claimed by {} in guild {}", reportId, member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "REPORT_CLAIM", "reportId=" + reportId);
    }

    private void handleResolvePrompt(ButtonInteractionEvent event, String reportId) {
        TextInput note = TextInput.create("note", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Was wurde unternommen? Diese Notiz erhält das Mitglied per DM.")
                .setRequired(false)
                .setMaxLength(longFieldMaxLength)
                .build();

        Modal modal = Modal.create("report:resolvemodal:" + reportId, "Report abschließen")
                .addComponents(Label.of("Abschlussnotiz (optional)", note))
                .build();

        event.replyModal(modal).queue();
    }

    private void handleRejectPrompt(ButtonInteractionEvent event, String reportId) {
        TextInput reason = TextInput.create("reason", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Warum wird dieser Report abgelehnt? Das Mitglied erhält diese Begründung per DM.")
                .setMaxLength(longFieldMaxLength)
                .build();

        Modal modal = Modal.create("report:rejectmodal:" + reportId, "Report ablehnen")
                .addComponents(Label.of("Begründung", reason))
                .build();

        event.replyModal(modal).queue();
    }

    private void handleResolveSubmission(ModalInteractionEvent event, String reportId) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        String note = getValue(event, "note");
        boolean success = service.resolve(guild.getId(), reportId, member.getId(), member.getUser().getName(), note);
        if (!success) {
            event.reply("Dieser Report konnte nicht abgeschlossen werden.").setEphemeral(true).queue();
            return;
        }

        Report report = service.getReport(guild.getId(), reportId).orElseThrow();
        event.reply("Report `" + reportId + "` wurde als **erledigt** markiert.").setEphemeral(true).queue();
        updateChannelMessage(event, report);
        notifyMember(event, report, ReportEmbedFactory.buildResolutionDmEmbed(report));
        LOGGER.info("Report {} resolved by {} in guild {}", reportId, member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "REPORT_RESOLVE", "reportId=" + reportId);
    }

    private void handleRejectSubmission(ModalInteractionEvent event, String reportId) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        if (guild == null || member == null) {
            return;
        }

        String reason = getValue(event, "reason");
        boolean success = service.reject(guild.getId(), reportId, member.getId(), member.getUser().getName(), reason);
        if (!success) {
            event.reply("Dieser Report konnte nicht abgelehnt werden.").setEphemeral(true).queue();
            return;
        }

        Report report = service.getReport(guild.getId(), reportId).orElseThrow();
        event.reply("Report `" + reportId + "` wurde **abgelehnt**.").setEphemeral(true).queue();
        updateChannelMessage(event, report);
        notifyMember(event, report, ReportEmbedFactory.buildRejectionDmEmbed(report));
        LOGGER.info("Report {} rejected by {} in guild {}", reportId, member.getId(), guild.getId());
        AuditLog.record(guild.getId(), member.getId(), "REPORT_REJECT", "reportId=" + reportId);
    }

    private void updateChannelMessage(ModalInteractionEvent event, Report report) {
        if (report.getReportChannelId() == null || report.getReportMessageId() == null) {
            return;
        }

        TextChannel channel = event.getJDA().getChannelById(TextChannel.class, report.getReportChannelId());
        if (channel == null) {
            return;
        }

        channel.retrieveMessageById(report.getReportMessageId()).queue(
                message -> message.editMessageEmbeds(ReportEmbedFactory.buildChannelEmbed(report))
                        .setComponents(ReportEmbedFactory.buildChannelComponents(report))
                        .queue(),
                failure -> LOGGER.error("Failed to update report message {}: {}", report.getReportMessageId(), failure.getMessage()));
    }

    private void notifyMember(ModalInteractionEvent event, Report report, MessageEmbed embed) {
        event.getJDA().retrieveUserById(report.getReporterId()).queue(
                user -> user.openPrivateChannel().queue(
                        privateChannel -> privateChannel.sendMessageEmbeds(embed).queue(
                                success -> {
                                },
                                failure -> LOGGER.warn("Could not DM user {} about report {}: {}", report.getReporterId(), report.getId(), failure.getMessage())),
                        failure -> LOGGER.warn("Could not open DM channel with user {}: {}", report.getReporterId(), failure.getMessage())),
                failure -> LOGGER.warn("Could not retrieve user {}: {}", report.getReporterId(), failure.getMessage()));
    }

    private String getValue(ModalInteractionEvent event, String id) {
        ModalMapping mapping = event.getValue(id);
        return mapping != null ? mapping.getAsString() : null;
    }

    private void replyGenericError(IReplyCallback event) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").queue();
        } else {
            event.reply("Es ist ein unerwarteter Fehler aufgetreten. Bitte versuche es erneut.").setEphemeral(true).queue();
        }
    }
}
