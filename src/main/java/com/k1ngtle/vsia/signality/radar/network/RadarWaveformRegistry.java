package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RadarWaveformRegistry {
    private static final Map<UUID, RadarWaveformProfile> PROFILES =
            new ConcurrentHashMap<>();

    private RadarWaveformRegistry() {
    }

    public static void set(
            UUID emitterId,
            RadarWaveformProfile profile
    ) {
        if (emitterId == null
                || profile == null) {
            return;
        }

        PROFILES.put(
                emitterId,
                profile
        );
    }

    public static void clear(
            UUID emitterId
    ) {
        if (emitterId != null) {
            PROFILES.remove(
                    emitterId
            );
        }
    }

    public static void clearAll() {
        PROFILES.clear();
    }

    public static RadarWaveformProfile forEmitter(
            IRadarEmitter emitter
    ) {
        RadarWaveformProfile configured =
                PROFILES.get(
                        emitter.id()
                );

        if (configured != null) {
            return configured;
        }

        return RadarWaveformProfile.realisticDefault(
                emitter.profile()
                        .wavelengthMeters()
        );
    }
}
