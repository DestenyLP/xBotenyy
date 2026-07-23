package de.destenylp.xBotenyy.common.commands;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandRegistry<C> {
    private final Map<String, Command<C>> commandsByName = new ConcurrentHashMap<>();
    private final Map<String, Command<C>> lookupByAlias = new ConcurrentHashMap<>();

    public void register(Command<C> command) {
        String name = normalize(command.getName());
        if (commandsByName.containsKey(name) || lookupByAlias.containsKey(name)) {
            throw new IllegalStateException("Befehlsname '" + name + "' ist bereits registriert.");
        }
        commandsByName.put(name, command);
        lookupByAlias.put(name, command);

        for (String alias : command.getAliases()) {
            String normalizedAlias = normalize(alias);
            if (lookupByAlias.containsKey(normalizedAlias)) {
                throw new IllegalStateException("Alias '" + normalizedAlias + "' fuer Befehl '" + name
                        + "' ist bereits vergeben.");
            }
            lookupByAlias.put(normalizedAlias, command);
        }
    }

    public Optional<Command<C>> find(String nameOrAlias) {
        if (nameOrAlias == null || nameOrAlias.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(lookupByAlias.get(normalize(nameOrAlias)));
    }

    public Collection<Command<C>> all() {
        return List.copyOf(commandsByName.values());
    }

    public int size() {
        return commandsByName.size();
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
