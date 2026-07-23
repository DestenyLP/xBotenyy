package de.destenylp.xBotenyy.common.core;

import java.time.Duration;

public interface PrunableResource extends Prunable {
    String getName();

    static PrunableResource of(String name, Prunable prunable) {
        return new PrunableResource() {
            @Override
            public int pruneOldEntries(Duration retention) {
                return prunable.pruneOldEntries(retention);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }
}
