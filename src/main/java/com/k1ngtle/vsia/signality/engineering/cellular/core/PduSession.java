package com.k1ngtle.vsia.signality.engineering.cellular.core;

import java.util.UUID;

public record PduSession(
        int sessionId,
        UUID ueId,
        String dnn,
        String ipAddress,
        int fiveQi,
        boolean active
) {
}
