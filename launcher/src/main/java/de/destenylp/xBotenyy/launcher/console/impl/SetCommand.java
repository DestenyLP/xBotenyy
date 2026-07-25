package de.destenylp.xBotenyy.launcher.console.impl;

import de.destenylp.xBotenyy.launcher.LauncherSettings;
import de.destenylp.xBotenyy.launcher.console.CommandContext;
import de.destenylp.xBotenyy.launcher.console.ConsoleCommand;

import java.util.List;
import java.util.Locale;

public final class SetCommand implements ConsoleCommand {

    private static final List<String> MAX_RESTARTS_KEYS = List.of("maxrestarts", "max-restarts", "maxrestart", "attempts");
    private static final List<String> RESTART_DELAY_KEYS = List.of("restartdelay", "restart-delay", "delay");

    private static int parseInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " muss eine Ganzzahl sein, war: " + value);
        }
    }

    private static long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " muss eine Ganzzahl sein, war: " + value);
        }
    }

    @Override
    public String name() {
        return "set";
    }

    @Override
    public List<String> aliases() {
        return List.of("config", "settings");
    }

    @Override
    public String usage() {
        return "set [maxrestarts <n> | restartdelay <sekunden>]";
    }

    @Override
    public String description() {
        return "Zeigt oder aendert die Neustart-Einstellungen (maxrestarts, restartdelay) zur Laufzeit.";
    }

    @Override
    public void execute(String[] args, CommandContext context) {
        LauncherSettings settings = context.settings();

        if (args.length == 0) {
            printCurrent(context, settings);
            return;
        }
        if (args.length != 2) {
            throw new IllegalArgumentException("Verwendung: " + usage());
        }

        String key = args[0].toLowerCase(Locale.ROOT);
        String rawValue = args[1].trim();

        if (MAX_RESTARTS_KEYS.contains(key)) {
            int newValue = parseInt(rawValue, "maxrestarts");
            int previous = settings.setMaxRestartAttempts(newValue);
            context.print("maxrestarts: " + previous + " -> " + newValue
                    + " (gilt ab dem naechsten Absturz/Neustart eines Bots)");
            return;
        }
        if (RESTART_DELAY_KEYS.contains(key)) {
            long newValue = parseLong(rawValue, "restartdelay");
            long previous = settings.setRestartDelaySeconds(newValue);
            context.print("restartdelay: " + previous + "s -> " + newValue + "s"
                    + " (gilt ab dem naechsten Absturz/Neustart eines Bots)");
            return;
        }

        throw new IllegalArgumentException("Unbekannte Einstellung '" + args[0]
                + "'. Gueltige Werte: maxrestarts, restartdelay.");
    }

    private void printCurrent(CommandContext context, LauncherSettings settings) {
        context.print("Aktuelle Einstellungen:");
        context.print("  maxrestarts  = " + settings.getMaxRestartAttempts()
                + "  (max. automatische Neustartversuche pro Absturz)");
        context.print("  restartdelay = " + settings.getRestartDelaySeconds() + "s"
                + "  (Wartezeit vor einem automatischen Neustart)");
        context.print("Aendern mit: set maxrestarts <n>  |  set restartdelay <sekunden>");
    }
}
