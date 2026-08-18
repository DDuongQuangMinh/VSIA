package com.k1ngtle.vsia.signality.engineering.channel;

import com.k1ngtle.vsia.signality.api.signal.SignalPacket;
import net.minecraft.server.level.ServerLevel;

public record ScheduledRfTransmission(
        ActiveRfTransmission metadata,
        SignalPacket packet,
        ServerLevel level
) {
    public ScheduledRfTransmission {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata");
        }

        if (packet == null) {
            throw new IllegalArgumentException("packet");
        }

        if (level == null) {
            throw new IllegalArgumentException("level");
        }
    }
}
