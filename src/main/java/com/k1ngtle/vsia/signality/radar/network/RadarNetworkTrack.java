package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.radar.iff.IffResult;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record RadarNetworkTrack(
        UUID trackId,
        RadarTrackState state,
        Vec3 position,
        Vec3 velocity,
        long createdTick,
        long lastMeasurementTick,
        int hits,
        int sensorCount,
        Set<UUID> contributingSensors,
        double bestSnrLinear,
        double positionUncertaintyMeters,
        double quality,
        IffResult iff
) {
    public RadarNetworkTrack {
        contributingSensors =
                Set.copyOf(
                        contributingSensors
                );
    }

    public double speedMps() {
        return velocity.length();
    }

    public double ageSeconds(
            long nowTick
    ) {
        return Math.max(
                0L,
                nowTick - createdTick
        ) / 20.0;
    }

    public double staleSeconds(
            long nowTick
    ) {
        return Math.max(
                0L,
                nowTick - lastMeasurementTick
        ) / 20.0;
    }
}
