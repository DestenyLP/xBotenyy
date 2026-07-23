package de.destenylp.xBotenyy.common.core;

import java.time.Duration;

public interface Prunable {
    int pruneOldEntries(Duration retention);
}
