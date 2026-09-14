package com.k1ngtle.vsia.signality.radar.network;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;

public final class RadarNetworkService {
    public static final String DEFAULT_NETWORK =
            "default";

    private static final Map<ServerLevel, Map<String, RadarNetwork>> NETWORKS =
            new ConcurrentHashMap<>();

    private static final Map<UUID, String> EMITTER_BINDINGS =
            new ConcurrentHashMap<>();

    private RadarNetworkService() {
    }

    public static void receive(
            IRadarEmitter emitter,
            RadarContact contact
    ) {
        if (emitter == null
                || contact == null
                || emitter.level() == null) {
            return;
        }

        ServerLevel level =
                emitter.level();

        long gameTime =
                level.getGameTime();

        String networkId =
                networkFor(
                        emitter.id()
                );

        RadarNetwork network =
                network(
                        level,
                        networkId
                );

        RadarMeasurement measurement =
                RadarMeasurementModel.fromContact(
                        emitter,
                        contact,
                        gameTime
                );

        if (measurement == null) {
            network.recordDroppedReport();
            return;
        }

        RadarNetworkConfig config =
                network.config();

        long linkSeed =
                linkSeed(
                        measurement,
                        networkId
                );

        if (unitInterval(
                linkSeed
        )
                < config.packetLossProbability()) {
            network.recordDroppedReport();
            return;
        }

        int jitter =
                config.jitterTicks() == 0
                        ? 0
                        : boundedInt(
                        mix64(
                                linkSeed
                                        ^ 0x4f1bbcdcBEEFL
                        ),
                        config.jitterTicks()
                                + 1
                );

        long deliveryTick =
                gameTime
                        + config.baseLatencyTicks()
                        + jitter;

        network.accept(
                new RadarSensorReport(
                        networkId,
                        deliveryTick,
                        measurement
                )
        );
    }

    public static void tick(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        Map<String, RadarNetwork> networks =
                NETWORKS.get(
                        level
                );

        if (networks == null
                || networks.isEmpty()) {
            return;
        }

        long now =
                level.getGameTime();

        for (RadarNetwork network :
                networks.values()) {
            network.tick(
                    now
            );
        }
    }

    public static RadarNetwork network(
            ServerLevel level,
            String networkId
    ) {
        String normalized =
                normalizeNetworkId(
                        networkId
                );

        Map<String, RadarNetwork> networks =
                NETWORKS.computeIfAbsent(
                        level,
                        ignored ->
                                new ConcurrentHashMap<>()
                );

        return networks.computeIfAbsent(
                normalized,
                ignored ->
                        new RadarNetwork(
                                normalized,
                                RadarNetworkConfig.realisticDefault()
                        )
        );
    }

    public static RadarNetwork configureNetwork(
            ServerLevel level,
            String networkId,
            RadarNetworkConfig config
    ) {
        String normalized =
                normalizeNetworkId(
                        networkId
                );

        Map<String, RadarNetwork> networks =
                NETWORKS.computeIfAbsent(
                        level,
                        ignored ->
                                new ConcurrentHashMap<>()
                );

        RadarNetwork replacement =
                new RadarNetwork(
                        normalized,
                        config
                );

        RadarNetwork previous =
                networks.put(
                        normalized,
                        replacement
                );

        if (previous != null) {
            previous.clear();
        }

        return replacement;
    }

    public static List<RadarNetworkTrack> tracks(
            ServerLevel level,
            String networkId
    ) {
        if (level == null) {
            return List.of();
        }

        RadarNetwork network =
                network(
                        level,
                        networkId
                );

        return network.tracks(
                level.getGameTime()
        );
    }

    public static RadarNetworkStats stats(
            ServerLevel level,
            String networkId
    ) {
        return network(
                level,
                networkId
        )
                .stats();
    }

    public static Set<String> networkIds(
            ServerLevel level
    ) {
        if (level == null) {
            return Set.of(
                    DEFAULT_NETWORK
            );
        }

        Map<String, RadarNetwork> networks =
                NETWORKS.get(
                        level
                );

        if (networks == null
                || networks.isEmpty()) {
            return Set.of(
                    DEFAULT_NETWORK
            );
        }

        return Set.copyOf(
                networks.keySet()
        );
    }

    public static String bindEmitter(
            ServerLevel level,
            UUID emitterId,
            String networkId
    ) {
        String normalized =
                normalizeNetworkId(
                        networkId
                );

        network(
                level,
                normalized
        );

        EMITTER_BINDINGS.put(
                emitterId,
                normalized
        );

        return normalized;
    }

    public static void unbindEmitter(
            UUID emitterId
    ) {
        if (emitterId != null) {
            EMITTER_BINDINGS.remove(
                    emitterId
            );
        }
    }

    public static String networkFor(
            UUID emitterId
    ) {
        if (emitterId == null) {
            return DEFAULT_NETWORK;
        }

        return EMITTER_BINDINGS.getOrDefault(
                emitterId,
                DEFAULT_NETWORK
        );
    }

    public static void clearNetwork(
            ServerLevel level,
            String networkId
    ) {
        if (level == null) {
            return;
        }

        Map<String, RadarNetwork> networks =
                NETWORKS.get(
                        level
                );

        if (networks == null) {
            return;
        }

        RadarNetwork network =
                networks.remove(
                        normalizeNetworkId(
                                networkId
                        )
                );

        if (network != null) {
            network.clear();
        }
    }

    public static void clearLevel(
            ServerLevel level
    ) {
        if (level == null) {
            return;
        }

        Map<String, RadarNetwork> networks =
                NETWORKS.remove(
                        level
                );

        if (networks != null) {
            for (RadarNetwork network :
                    networks.values()) {
                network.clear();
            }
        }
    }

    public static void clear() {
        List<RadarNetwork> all =
                new ArrayList<>();

        for (Map<String, RadarNetwork> networks :
                NETWORKS.values()) {
            all.addAll(
                    networks.values()
            );
        }

        for (RadarNetwork network :
                all) {
            network.clear();
        }

        NETWORKS.clear();
        EMITTER_BINDINGS.clear();
        RadarWaveformRegistry.clearAll();
    }

    public static String normalizeNetworkId(
            String value
    ) {
        if (value == null
                || value.isBlank()) {
            return DEFAULT_NETWORK;
        }

        String lower =
                value.trim()
                        .toLowerCase();

        StringBuilder out =
                new StringBuilder();

        for (int i = 0;
             i < lower.length()
                     && out.length() < 32;
             i++) {
            char ch =
                    lower.charAt(
                            i
                    );

            if (Character.isLetterOrDigit(
                    ch
            )
                    || ch == '-'
                    || ch == '_'
                    || ch == '.') {
                out.append(
                        ch
                );
            }
        }

        return out.length() == 0
                ? DEFAULT_NETWORK
                : out.toString();
    }

    private static long linkSeed(
            RadarMeasurement measurement,
            String networkId
    ) {
        long value =
                measurement.emitterId()
                        .getMostSignificantBits()
                        ^ measurement.emitterId()
                        .getLeastSignificantBits();

        value =
                mix64(
                        value
                                ^ measurement.sourceTargetId()
                                .getMostSignificantBits()
                );

        value =
                mix64(
                        value
                                ^ measurement.sourceTargetId()
                                .getLeastSignificantBits()
                );

        value =
                mix64(
                        value
                                ^ measurement.measurementTick()
                );

        return mix64(
                value
                        ^ networkId.hashCode()
        );
    }

    private static int boundedInt(
            long value,
            int bound
    ) {
        if (bound <= 1) {
            return 0;
        }

        long positive =
                value
                        & Long.MAX_VALUE;

        return (int) (
                positive
                        % bound
        );
    }

    private static double unitInterval(
            long value
    ) {
        long bits =
                (
                        value >>> 11
                )
                        & (
                        (1L << 53)
                                - 1L
                );

        return bits
                * 0x1.0p-53;
    }

    private static long mix64(
            long z
    ) {
        z =
                (
                        z
                                ^ (
                                z >>> 30
                        )
                )
                        * 0xbf58476d1ce4e5b9L;

        z =
                (
                        z
                                ^ (
                                z >>> 27
                        )
                )
                        * 0x94d049bb133111ebL;

        return z
                ^ (
                z >>> 31
        );
    }
}
