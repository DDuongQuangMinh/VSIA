package com.k1ngtle.vsia.signality.engineering.vm;

public record ProtocolVmTimer(
        String id,
        String entrypoint,
        long dueTick
) {
    public ProtocolVmTimer {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id");
        }

        if (entrypoint == null
                || entrypoint.isBlank()) {
            throw new IllegalArgumentException(
                    "entrypoint"
            );
        }
    }
}
