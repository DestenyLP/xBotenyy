package de.destenylp.xBotenyy.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuditLog {
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private AuditLog() {
    }

    public static void record(String scopeId, String actorId, String action, String detail) {
        AUDIT.info("scope={} actor={} action={} detail={}", scopeId, actorId, action, detail);
    }
}
