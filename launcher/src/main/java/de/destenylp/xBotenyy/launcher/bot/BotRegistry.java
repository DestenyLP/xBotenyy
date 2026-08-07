package de.destenylp.xBotenyy.launcher.bot;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class BotRegistry {
    private final Map<BotId, ManagedBot> bots = new EnumMap<>(BotId.class);

    public void register(ManagedBot bot) {
        bots.put(bot.getId(), bot);
    }

    public Optional<ManagedBot> find(BotId id) {
        return Optional.ofNullable(bots.get(id));
    }

    public Optional<ManagedBot> find(String token) {
        return BotId.parse(token).flatMap(this::find);
    }

    public Collection<ManagedBot> all() {
        return bots.values();
    }

    public boolean isEmpty() {
        return bots.isEmpty();
    }
}

