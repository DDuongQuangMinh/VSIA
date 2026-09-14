package com.k1ngtle.vsia.signality.radar.network;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public final class RadarNetworkApi {
    private RadarNetworkApi() {
    }

    public static RadarNetwork configure(
            ServerLevel level,
            String networkId,
            RadarNetworkConfig config
    ) {
        return RadarNetworkService.configureNetwork(
                level,
                networkId,
                config
        );
    }

    public static List<RadarNetworkTrack> tracks(
            ServerLevel level,
            String networkId
    ) {
        return RadarNetworkService.tracks(
                level,
                networkId
        );
    }

    public static RadarNetworkStats stats(
            ServerLevel level,
            String networkId
    ) {
        return RadarNetworkService.stats(
                level,
                networkId
        );
    }

    public static Set<String> networks(
            ServerLevel level
    ) {
        return RadarNetworkService.networkIds(
                level
        );
    }

    public static String bindEmitter(
            ServerLevel level,
            UUID emitterId,
            String networkId
    ) {
        return RadarNetworkService.bindEmitter(
                level,
                emitterId,
                networkId
        );
    }

    public static void unbindEmitter(
            UUID emitterId
    ) {
        RadarNetworkService.unbindEmitter(
                emitterId
        );
    }

    public static String networkFor(
            UUID emitterId
    ) {
        return RadarNetworkService.networkFor(
                emitterId
        );
    }

    public static void setWaveform(
            UUID emitterId,
            RadarWaveformProfile profile
    ) {
        RadarWaveformRegistry.set(
                emitterId,
                profile
        );
    }

    public static void clearWaveform(
            UUID emitterId
    ) {
        RadarWaveformRegistry.clear(
                emitterId
        );
    }
}
