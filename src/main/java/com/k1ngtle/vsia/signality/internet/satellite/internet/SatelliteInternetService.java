package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteNetworkManager;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Random;

public final class SatelliteInternetService {
    public static final double LOCAL_TERMINAL_RANGE_BLOCKS =
            16.0;

    private SatelliteInternetService() {
    }

    public static PathResult resolvePath(
            ServerPlayer player
    ) {
        if (player == null) {
            return PathResult.error(
                    "No player is attached to this request."
            );
        }

        ServerLevel level =
                player.serverLevel();

        TemporarySatelliteTerminalBlockEntity userTerminal =
                nearestTerminal(
                        level,
                        player.position(),
                        LOCAL_TERMINAL_RANGE_BLOCKS
                );

        if (userTerminal == null) {
            return PathResult.error(
                    "No satellite user terminal is within "
                            + (int) LOCAL_TERMINAL_RANGE_BLOCKS
                            + " blocks."
            );
        }

        TemporarySatelliteTerminalBlockEntity bestGateway =
                null;

        SatelliteLinkAssessment bestLink =
                null;

        for (TemporarySatelliteTerminalBlockEntity candidate
                : SatelliteNetworkManager.terminals(
                level
        )) {
            if (candidate == null
                    || candidate == userTerminal
                    || candidate.isRemoved()
                    || !candidate.internetGatewayEnabled()
                    || candidate.band()
                    != userTerminal.band()) {
                continue;
            }

            SatelliteLinkAssessment assessment =
                    SatelliteNetworkManager.assess(
                            userTerminal,
                            candidate
                    );

            if (!assessment.visible()) {
                continue;
            }

            if (bestLink == null
                    || assessment
                    .bottleneckSnrDb()
                    > bestLink
                    .bottleneckSnrDb()) {
                bestGateway =
                        candidate;

                bestLink =
                        assessment;
            }
        }

        if (bestGateway == null
                || bestLink == null) {
            return PathResult.error(
                    "No Internet gateway terminal on "
                            + userTerminal.band().name()
                            + " has a common visible satellite."
            );
        }

        return PathResult.success(
                new SatelliteInternetPath(
                        userTerminal,
                        bestGateway,
                        bestLink
                )
        );
    }

    public static PhoneBrowserServerService.ServerPage fetchWebsite(
            ServerPlayer player,
            String rawUrl
    ) {
        PathResult resolved =
                resolvePath(
                        player
                );

        if (!resolved.success()) {
            return new PhoneBrowserServerService.ServerPage(
                    rawUrl == null
                            ? ""
                            : rawUrl,
                    503,
                    "Satellite Unavailable",
                    "text/plain; charset=utf-8",
                    resolved.error(),
                    "",
                    "SATELLITE",
                    "Browser -> local terminal -> no usable satellite Internet path"
            );
        }

        SatelliteInternetPath path =
                resolved.path();

        SatelliteLinkAssessment link =
                path.link();

        double oneWaySuccess =
                clamp01(
                        link.packetSuccessProbability()
                );

        double requestResponseSuccess =
                oneWaySuccess
                        * oneWaySuccess;

        if (!passesLink(
                player,
                rawUrl,
                requestResponseSuccess
        )) {
            return new PhoneBrowserServerService.ServerPage(
                    rawUrl == null
                            ? ""
                            : rawUrl,
                    504,
                    "Satellite Link Loss",
                    "text/plain; charset=utf-8",
                    String.format(
                            Locale.ROOT,
                            "The SATCOM path through %s dropped the request/response exchange. Link probability %.3f.",
                            link.satelliteName(),
                            requestResponseSuccess
                    ),
                    "",
                    "SATELLITE",
                    routeSummary(
                            path
                    )
            );
        }

        PhoneBrowserServerService.ServerPage backbone =
                PhoneBrowserServerService.fetch(
                        player,
                        rawUrl,
                        "WIFI"
                );

        path.userTerminal()
                .setStatus(
                        "Satellite Internet active via "
                                + link.satelliteName()
                );

        path.gatewayTerminal()
                .setStatus(
                        "Internet gateway relaying "
                                + path.userTerminal()
                                .getBlockPos()
                                .toShortString()
                );

        return new PhoneBrowserServerService.ServerPage(
                backbone.url(),
                backbone.statusCode(),
                backbone.reason(),
                backbone.contentType(),
                backbone.body(),
                backbone.styleSheet(),
                "SATELLITE",
                routeSummary(
                        path
                )
        );
    }

    public static String routeSummary(
            SatelliteInternetPath path
    ) {
        if (path == null
                || !path.valid()) {
            return "Satellite route unavailable";
        }

        SatelliteLinkAssessment link =
                path.link();

        return String.format(
                Locale.ROOT,
                "Browser -> VSAT %s -> %s uplink -> %s -> downlink -> Internet Gateway %s -> ISP1 DNS / W1.28 data center | SNR %.1f dB | one-way %.2f ms",
                path.userTerminal()
                        .getBlockPos()
                        .toShortString(),
                path.userTerminal()
                        .band()
                        .name(),
                link.satelliteName(),
                path.gatewayTerminal()
                        .getBlockPos()
                        .toShortString(),
                link.bottleneckSnrDb(),
                link.propagationDelayMs()
        );
    }

    private static TemporarySatelliteTerminalBlockEntity nearestTerminal(
            ServerLevel level,
            Vec3 playerPosition,
            double maximumDistance
    ) {
        TemporarySatelliteTerminalBlockEntity best =
                null;

        double maximumDistanceSqr =
                maximumDistance
                        * maximumDistance;

        double bestDistanceSqr =
                maximumDistanceSqr;

        for (TemporarySatelliteTerminalBlockEntity terminal
                : SatelliteNetworkManager.terminals(
                level
        )) {
            if (terminal == null
                    || terminal.isRemoved()) {
                continue;
            }

            double distance =
                    Vec3.atCenterOf(
                            terminal.getBlockPos()
                    )
                            .distanceToSqr(
                                    playerPosition
                            );

            if (distance <= bestDistanceSqr) {
                best =
                        terminal;

                bestDistanceSqr =
                        distance;
            }
        }

        return best;
    }

    private static boolean passesLink(
            ServerPlayer player,
            String rawUrl,
            double probability
    ) {
        if (probability >= 0.999999) {
            return true;
        }

        long epoch =
                player.serverLevel()
                        .getGameTime()
                        / 20L;

        long seed =
                player.getUUID()
                        .getMostSignificantBits()
                        ^ Long.rotateLeft(
                        player.getUUID()
                                .getLeastSignificantBits(),
                        19
                )
                        ^ Long.rotateLeft(
                        epoch,
                        7
                )
                        ^ (
                        rawUrl == null
                                ? 0
                                : rawUrl.hashCode()
                );

        return new Random(
                seed
        )
                .nextDouble()
                <= probability;
    }

    private static double clamp01(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }

    public record PathResult(
            boolean success,
            SatelliteInternetPath path,
            String error
    ) {
        public static PathResult success(
                SatelliteInternetPath path
        ) {
            return new PathResult(
                    true,
                    path,
                    ""
            );
        }

        public static PathResult error(
                String error
        ) {
            return new PathResult(
                    false,
                    null,
                    error == null
                            ? "Satellite path unavailable"
                            : error
            );
        }
    }
}
