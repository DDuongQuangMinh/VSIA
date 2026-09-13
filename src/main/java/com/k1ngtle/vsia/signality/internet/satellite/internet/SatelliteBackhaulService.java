package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteNetworkManager;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SatelliteBackhaulService {
    public static final double TERMINAL_ACCESS_RANGE_BLOCKS =
            64.0;

    private SatelliteBackhaulService() {
    }

    public static PathResult resolve(
            ServerLevel level,
            Vec3 sourceEndpoint,
            Vec3 destinationEndpoint
    ) {
        if (level == null
                || sourceEndpoint == null
                || destinationEndpoint == null) {
            return PathResult.error(
                    "Invalid satellite backhaul endpoints."
            );
        }

        double endpointDistance =
                LongHaulRoutePolicy.distanceBlocks(
                        sourceEndpoint,
                        destinationEndpoint
                );

        if (endpointDistance
                < LongHaulRoutePolicy
                .SATELLITE_REQUIRED_DISTANCE_BLOCKS) {
            return PathResult.notRequired(
                    endpointDistance
            );
        }

        List<TerminalCandidate> sourceTerminals =
                nearbyTerminals(
                        level,
                        sourceEndpoint
                );

        if (sourceTerminals.isEmpty()) {
            return PathResult.error(
                    "Satellite backhaul is mandatory at 5 km or more, but no satellite terminal is within "
                            + (int) TERMINAL_ACCESS_RANGE_BLOCKS
                            + " blocks of the source endpoint."
            );
        }

        List<TerminalCandidate> destinationTerminals =
                nearbyTerminals(
                        level,
                        destinationEndpoint
                );

        if (destinationTerminals.isEmpty()) {
            return PathResult.error(
                    "Satellite backhaul is mandatory at 5 km or more, but no satellite terminal is within "
                            + (int) TERMINAL_ACCESS_RANGE_BLOCKS
                            + " blocks of the destination endpoint."
            );
        }

        SatelliteInternetPath bestPath =
                null;

        for (TerminalCandidate source
                : sourceTerminals) {
            for (TerminalCandidate destination
                    : destinationTerminals) {
                if (source.terminal()
                        == destination.terminal()) {
                    continue;
                }

                if (source.terminal()
                        .band()
                        != destination.terminal()
                        .band()) {
                    continue;
                }

                SatelliteLinkAssessment link =
                        SatelliteNetworkManager.assess(
                                source.terminal(),
                                destination.terminal()
                        );

                if (!link.visible()) {
                    continue;
                }

                SatelliteInternetPath candidate =
                        new SatelliteInternetPath(
                                source.terminal(),
                                destination.terminal(),
                                link,
                                endpointDistance,
                                source.distanceBlocks(),
                                destination.distanceBlocks()
                        );

                if (bestPath == null
                        || better(
                        candidate,
                        bestPath
                )) {
                    bestPath =
                            candidate;
                }
            }
        }

        if (bestPath == null) {
            return PathResult.error(
                    "Satellite backhaul is mandatory at 5 km or more, but no same-band source/destination terminal pair currently has a common visible satellite."
            );
        }

        return PathResult.success(
                bestPath
        );
    }

    public static String routeSummary(
            SatelliteInternetPath path
    ) {
        if (path == null
                || !path.valid()) {
            return "Satellite backhaul unavailable";
        }

        SatelliteLinkAssessment link =
                path.link();

        return String.format(
                Locale.ROOT,
                "SATCOM backhaul | endpoint distance %.3f km | source terminal %s | %s | %s | destination terminal %s | SNR %.1f dB | one-way %.2f ms | P %.3f",
                path.endpointDistanceBlocks()
                        / LongHaulRoutePolicy.BLOCKS_PER_KILOMETER,
                path.sourceTerminal()
                        .getBlockPos()
                        .toShortString(),
                path.sourceTerminal()
                        .band()
                        .name(),
                link.satelliteName(),
                path.destinationTerminal()
                        .getBlockPos()
                        .toShortString(),
                link.bottleneckSnrDb(),
                link.propagationDelayMs(),
                link.packetSuccessProbability()
        );
    }

    private static boolean better(
            SatelliteInternetPath candidate,
            SatelliteInternetPath current
    ) {
        int snrCompare =
                Double.compare(
                        candidate.link()
                                .bottleneckSnrDb(),
                        current.link()
                                .bottleneckSnrDb()
                );

        if (snrCompare != 0) {
            return snrCompare > 0;
        }

        double candidateAccess =
                candidate.sourceAccessDistanceBlocks()
                        + candidate.destinationAccessDistanceBlocks();

        double currentAccess =
                current.sourceAccessDistanceBlocks()
                        + current.destinationAccessDistanceBlocks();

        return candidateAccess
                < currentAccess;
    }

    private static List<TerminalCandidate> nearbyTerminals(
            ServerLevel level,
            Vec3 endpoint
    ) {
        List<TerminalCandidate> result =
                new ArrayList<>();

        double maximumDistanceSqr =
                TERMINAL_ACCESS_RANGE_BLOCKS
                        * TERMINAL_ACCESS_RANGE_BLOCKS;

        for (TemporarySatelliteTerminalBlockEntity terminal
                : SatelliteNetworkManager.terminals(
                level
        )) {
            if (terminal == null
                    || terminal.isRemoved()) {
                continue;
            }

            double distanceSqr =
                    Vec3.atCenterOf(
                            terminal.getBlockPos()
                    )
                            .distanceToSqr(
                                    endpoint
                            );

            if (distanceSqr
                    > maximumDistanceSqr) {
                continue;
            }

            result.add(
                    new TerminalCandidate(
                            terminal,
                            Math.sqrt(
                                    distanceSqr
                            )
                    )
            );
        }

        result.sort(
                Comparator.comparingDouble(
                        TerminalCandidate::distanceBlocks
                )
        );

        return List.copyOf(
                result
        );
    }

    private record TerminalCandidate(
            TemporarySatelliteTerminalBlockEntity terminal,
            double distanceBlocks
    ) {
    }

    public record PathResult(
            boolean success,
            boolean satelliteRequired,
            SatelliteInternetPath path,
            double endpointDistanceBlocks,
            String error
    ) {
        public static PathResult success(
                SatelliteInternetPath path
        ) {
            return new PathResult(
                    true,
                    true,
                    path,
                    path.endpointDistanceBlocks(),
                    ""
            );
        }

        public static PathResult notRequired(
                double endpointDistanceBlocks
        ) {
            return new PathResult(
                    true,
                    false,
                    null,
                    endpointDistanceBlocks,
                    ""
            );
        }

        public static PathResult error(
                String error
        ) {
            return new PathResult(
                    false,
                    true,
                    null,
                    Double.NaN,
                    error == null
                            ? "Satellite backhaul unavailable"
                            : error
            );
        }
    }
}
