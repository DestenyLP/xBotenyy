package de.destenylp.xBotenyy.discordbot.tickets;

import java.util.List;
import java.util.function.Supplier;

public class TicketSettings {
    private String categoryChannelId;
    private String supportRoleId;
    private String logChannelId;
    private String transcriptChannelId;
    private String panelChannelId;
    private String panelMessageId;
    private int maxOpenTicketsPerMember = 1;
    private int autoCloseInactivityHours = 0;
    private final Supplier<List<Ticket>> ticketsSupplier;

    public TicketSettings() {
        this(List::of);
    }

    TicketSettings(Supplier<List<Ticket>> ticketsSupplier) {
        this.ticketsSupplier = ticketsSupplier;
    }

    public String getCategoryChannelId() {
        return categoryChannelId;
    }

    public void setCategoryChannelId(String categoryChannelId) {
        this.categoryChannelId = categoryChannelId;
    }

    public String getSupportRoleId() {
        return supportRoleId;
    }

    public void setSupportRoleId(String supportRoleId) {
        this.supportRoleId = supportRoleId;
    }

    public String getLogChannelId() {
        return logChannelId;
    }

    public void setLogChannelId(String logChannelId) {
        this.logChannelId = logChannelId;
    }

    public String getTranscriptChannelId() {
        return transcriptChannelId;
    }

    public void setTranscriptChannelId(String transcriptChannelId) {
        this.transcriptChannelId = transcriptChannelId;
    }

    public String getPanelChannelId() {
        return panelChannelId;
    }

    public String getPanelMessageId() {
        return panelMessageId;
    }

    public void setPanel(String panelChannelId, String panelMessageId) {
        this.panelChannelId = panelChannelId;
        this.panelMessageId = panelMessageId;
    }

    public int getMaxOpenTicketsPerMember() {
        return maxOpenTicketsPerMember;
    }

    public void setMaxOpenTicketsPerMember(int maxOpenTicketsPerMember) {
        this.maxOpenTicketsPerMember = Math.max(1, maxOpenTicketsPerMember);
    }

    public int getAutoCloseInactivityHours() {
        return autoCloseInactivityHours;
    }

    public void setAutoCloseInactivityHours(int autoCloseInactivityHours) {
        this.autoCloseInactivityHours = Math.max(0, autoCloseInactivityHours);
    }

    public boolean isConfigured() {
        return categoryChannelId != null && supportRoleId != null;
    }

    public List<Ticket> getTickets() {
        return ticketsSupplier.get();
    }
}
