package de.destenylp.xBotenyy.discordbot.reports;

import java.util.List;
import java.util.function.Supplier;

public class ReportSettings {
    private String channelId;
    private String notifyRoleId;
    private final Supplier<List<Report>> reportsSupplier;

    public ReportSettings() {
        this(List::of);
    }

    ReportSettings(Supplier<List<Report>> reportsSupplier) {
        this.reportsSupplier = reportsSupplier;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getNotifyRoleId() {
        return notifyRoleId;
    }

    public void setNotifyRoleId(String notifyRoleId) {
        this.notifyRoleId = notifyRoleId;
    }

    public List<Report> getReports() {
        return reportsSupplier.get();
    }
}
