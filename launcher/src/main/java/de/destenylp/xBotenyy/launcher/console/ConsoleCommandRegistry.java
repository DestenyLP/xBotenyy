package de.destenylp.xBotenyy.launcher.console;

import java.util.*;

public final class ConsoleCommandRegistry {

    private final Map<String, ConsoleCommand> byName = new LinkedHashMap<>();
    private final List<ConsoleCommand> ordered = new ArrayList<>();

    private static String normalize(String token) {
        return token.trim().toLowerCase(Locale.ROOT);
    }

    public void register(ConsoleCommand command) {
        String primary = normalize(command.name());
        if (byName.containsKey(primary)) {
            throw new IllegalStateException("Command '" + primary + "' is already registered.");
        }
        byName.put(primary, command);
        for (String alias : command.aliases()) {
            byName.putIfAbsent(normalize(alias), command);
        }
        ordered.add(command);
    }

    public Optional<ConsoleCommand> find(String token) {
        return Optional.ofNullable(byName.get(normalize(token)));
    }

    public List<ConsoleCommand> all() {
        return Collections.unmodifiableList(ordered);
    }
}
