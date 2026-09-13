package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsAnswer;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import com.k1ngtle.vsia.signality.internet.satellite.SatelliteLinkAssessment;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackDirectory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;
import java.util.Random;

public final class LongHaulBrowserService {
    private LongHaulBrowserService() {
    }

    public static PhoneBrowserServerService.ServerPage fetch(
            ServerPlayer player,
            String rawUrl,
            String accessTransport
    ) {
        RoutePlan plan =
                plan(
                        player,
                        rawUrl
                );

        String normalizedAccess =
                normalizeAccessTransport(
                        accessTransport
                );

        if (!plan.targetResolved()) {
            return PhoneBrowserServerService.fetch(
                    player,
                    rawUrl,
                    normalizedAccess
            );
        }

        if (!plan.physicalDestinationAvailable()) {
            return errorPage(
                    rawUrl,
                    503,
                    "Physical Destination Unavailable",
                    "The website resolves to "
                            + plan.rackIp()
                            + ", but no loaded physical Server Rack with that IP is registered. VS:IA cannot apply the 5 km routing policy without a physical destination.",
                    normalizedAccess,
                    plan.summary()
            );
        }

        if (!plan.satelliteRequired()) {
            PhoneBrowserServerService.ServerPage page =
                    PhoneBrowserServerService.fetch(
                            player,
                            rawUrl,
                            normalizedAccess
                    );

            return withRoute(
                    page,
                    normalizedAccess,
                    accessPrefix(
                            normalizedAccess
                    )
                            + " -> terrestrial/ground network -> ISP1 DNS/HTTP -> Server Rack "
                            + plan.rackPosText()
                            + " | distance "
                            + LongHaulRoutePolicy.describeDistance(
                            plan.distanceBlocks()
                    )
                            + " | SATELLITE NOT REQUIRED (<5 km)"
            );
        }

        if (!plan.backhaulAvailable()) {
            return errorPage(
                    rawUrl,
                    503,
                    "Satellite Backhaul Required",
                    "The physical destination is "
                            + LongHaulRoutePolicy.describeDistance(
                            plan.distanceBlocks()
                    )
                            + " away. VS:IA requires satellite backhaul at 5 km or more. "
                            + plan.backhaulError(),
                    normalizedAccess,
                    plan.summary()
            );
        }

        SatelliteInternetPath path =
                plan.satellitePath();

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
            return errorPage(
                    rawUrl,
                    504,
                    "Satellite Link Loss",
                    String.format(
                            Locale.ROOT,
                            "The required SATCOM request/response exchange through %s was lost. Combined exchange probability %.3f.",
                            link.satelliteName(),
                            requestResponseSuccess
                    ),
                    normalizedAccess,
                    satelliteRouteSummary(
                            normalizedAccess,
                            plan
                    )
            );
        }

        PhoneBrowserServerService.ServerPage page =
                PhoneBrowserServerService.fetch(
                        player,
                        rawUrl,
                        normalizedAccess
                );

        path.sourceTerminal()
                .setStatus(
                        "Long-haul network uplink via "
                                + link.satelliteName()
                );

        path.destinationTerminal()
                .setStatus(
                        "Long-haul network downlink via "
                                + link.satelliteName()
                );

        return withRoute(
                page,
                normalizedAccess,
                satelliteRouteSummary(
                        normalizedAccess,
                        plan
                )
        );
    }

    public static RoutePlan plan(
            ServerPlayer player,
            String rawUrl
    ) {
        if (player == null) {
            return RoutePlan.unresolved(
                    "No player endpoint is attached to this request."
            );
        }

        BrowserRequest request =
                new BrowserRequest(
                        rawUrl
                );

        if (request.authority()
                .isBlank()) {
            return RoutePlan.unresolved(
                    "Invalid website address."
            );
        }

        ServerLevel level =
                player.serverLevel();

        TargetResolution target =
                resolveTarget(
                        level,
                        request
                );

        if (!target.resolved()) {
            return RoutePlan.unresolved(
                    target.error()
            );
        }

        ServerRackBlockEntity rack =
                ServerRackDirectory.byIp(
                        level,
                        target.rackIp()
                );

        if (rack == null) {
            return RoutePlan.physicalUnavailable(
                    target.rackIp(),
                    target.host(),
                    "No loaded Server Rack is registered at the resolved IP."
            );
        }

        Vec3 source =
                player.position();

        Vec3 destination =
                Vec3.atCenterOf(
                        rack.getBlockPos()
                );

        double distanceBlocks =
                LongHaulRoutePolicy.distanceBlocks(
                        source,
                        destination
                );

        boolean satelliteRequired =
                LongHaulRoutePolicy.requiresSatellite(
                        source,
                        destination
                );

        if (!satelliteRequired) {
            return RoutePlan.terrestrial(
                    target.rackIp(),
                    target.host(),
                    rack,
                    distanceBlocks
            );
        }

        SatelliteBackhaulService.PathResult backhaul =
                SatelliteBackhaulService.resolve(
                        level,
                        source,
                        destination
                );

        if (!backhaul.success()) {
            return RoutePlan.satelliteUnavailable(
                    target.rackIp(),
                    target.host(),
                    rack,
                    distanceBlocks,
                    backhaul.error()
            );
        }

        return RoutePlan.satellite(
                target.rackIp(),
                target.host(),
                rack,
                distanceBlocks,
                backhaul.path()
        );
    }

    private static TargetResolution resolveTarget(
            ServerLevel level,
            BrowserRequest request
    ) {
        if (request.isPureRackAddress()) {
            BrowserRequest.DirectRackTarget target =
                    request.directRackTarget();

            if (target == null) {
                return TargetResolution.error(
                        "Direct rack address is missing the website host."
                );
            }

            return TargetResolution.success(
                    target.rackIp(),
                    target.host()
            );
        }

        if (request.usesDirectRack()) {
            if (request.host()
                    .isBlank()) {
                return TargetResolution.error(
                        "Direct rack address is missing the website host."
                );
            }

            return TargetResolution.success(
                    request.directRackIp(),
                    request.host()
            );
        }

        String host =
                request.host();

        if (host.isBlank()) {
            return TargetResolution.error(
                    "Invalid website hostname."
            );
        }

        Optional<InternetDnsAnswer> answer =
                InternetRegistrySavedData
                        .get(level)
                        .resolveFirst(
                                host,
                                "A",
                                System.currentTimeMillis()
                        );

        if (answer.isEmpty()) {
            return TargetResolution.error(
                    "ISP1 DNS has no A record for "
                            + host
                            + "."
            );
        }

        return TargetResolution.success(
                answer.get()
                        .value(),
                host
        );
    }

    private static String satelliteRouteSummary(
            String accessTransport,
            RoutePlan plan
    ) {
        SatelliteInternetPath path =
                plan.satellitePath();

        SatelliteLinkAssessment link =
                path.link();

        return String.format(
                Locale.ROOT,
                "%s -> local ground network -> SAT terminal %s -> %s %s uplink -> %s -> downlink -> SAT terminal %s -> destination ground network -> ISP1 DNS/HTTP -> Server Rack %s | endpoint distance %.3f km | SNR %.1f dB | SAT delay %.2f ms | P %.3f | SATELLITE REQUIRED (>=5 km)",
                accessPrefix(
                        accessTransport
                ),
                path.sourceTerminal()
                        .getBlockPos()
                        .toShortString(),
                path.sourceTerminal()
                        .band()
                        .name(),
                "SATCOM",
                link.satelliteName(),
                path.destinationTerminal()
                        .getBlockPos()
                        .toShortString(),
                plan.rackPosText(),
                plan.distanceBlocks()
                        / LongHaulRoutePolicy.BLOCKS_PER_KILOMETER,
                link.bottleneckSnrDb(),
                link.propagationDelayMs(),
                link.packetSuccessProbability()
        );
    }

    private static String accessPrefix(
            String accessTransport
    ) {
        if ("CELLULAR".equalsIgnoreCase(
                accessTransport
        )) {
            return "Browser -> Cellular -> UE -> gNB -> 5G Core/UPF";
        }

        return "Browser -> Wi-Fi -> 802.11 -> AP/Router";
    }

    private static String normalizeAccessTransport(
            String transport
    ) {
        return "CELLULAR".equalsIgnoreCase(
                transport
        )
                ? "CELLULAR"
                : "WIFI";
    }

    private static PhoneBrowserServerService.ServerPage withRoute(
            PhoneBrowserServerService.ServerPage page,
            String transport,
            String routeSummary
    ) {
        return new PhoneBrowserServerService.ServerPage(
                page.url(),
                page.statusCode(),
                page.reason(),
                page.contentType(),
                page.body(),
                page.styleSheet(),
                transport,
                routeSummary
        );
    }

    private static PhoneBrowserServerService.ServerPage errorPage(
            String rawUrl,
            int statusCode,
            String reason,
            String body,
            String transport,
            String routeSummary
    ) {
        return new PhoneBrowserServerService.ServerPage(
                rawUrl == null
                        ? ""
                        : new BrowserRequest(
                        rawUrl
                ).displayUrl(),
                statusCode,
                reason,
                "text/plain; charset=utf-8",
                body,
                "",
                transport,
                routeSummary
        );
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

    private record TargetResolution(
            boolean resolved,
            String rackIp,
            String host,
            String error
    ) {
        private static TargetResolution success(
                String rackIp,
                String host
        ) {
            return new TargetResolution(
                    true,
                    rackIp == null
                            ? ""
                            : rackIp,
                    host == null
                            ? ""
                            : host,
                    ""
            );
        }

        private static TargetResolution error(
                String error
        ) {
            return new TargetResolution(
                    false,
                    "",
                    "",
                    error == null
                            ? "Target could not be resolved."
                            : error
            );
        }
    }

    public record RoutePlan(
            boolean targetResolved,
            boolean physicalDestinationAvailable,
            boolean satelliteRequired,
            boolean backhaulAvailable,
            String rackIp,
            String host,
            ServerRackBlockEntity rack,
            double distanceBlocks,
            SatelliteInternetPath satellitePath,
            String backhaulError,
            String diagnostic
    ) {
        public static RoutePlan unresolved(
                String diagnostic
        ) {
            return new RoutePlan(
                    false,
                    false,
                    false,
                    false,
                    "",
                    "",
                    null,
                    Double.NaN,
                    null,
                    "",
                    diagnostic == null
                            ? "Target unresolved"
                            : diagnostic
            );
        }

        public static RoutePlan physicalUnavailable(
                String rackIp,
                String host,
                String diagnostic
        ) {
            return new RoutePlan(
                    true,
                    false,
                    false,
                    false,
                    rackIp,
                    host,
                    null,
                    Double.NaN,
                    null,
                    "",
                    diagnostic
            );
        }

        public static RoutePlan terrestrial(
                String rackIp,
                String host,
                ServerRackBlockEntity rack,
                double distanceBlocks
        ) {
            return new RoutePlan(
                    true,
                    true,
                    false,
                    true,
                    rackIp,
                    host,
                    rack,
                    distanceBlocks,
                    null,
                    "",
                    "Terrestrial route selected because distance is below 5 km."
            );
        }

        public static RoutePlan satelliteUnavailable(
                String rackIp,
                String host,
                ServerRackBlockEntity rack,
                double distanceBlocks,
                String error
        ) {
            return new RoutePlan(
                    true,
                    true,
                    true,
                    false,
                    rackIp,
                    host,
                    rack,
                    distanceBlocks,
                    null,
                    error == null
                            ? "Satellite backhaul unavailable"
                            : error,
                    "Satellite backhaul is mandatory because distance is at least 5 km."
            );
        }

        public static RoutePlan satellite(
                String rackIp,
                String host,
                ServerRackBlockEntity rack,
                double distanceBlocks,
                SatelliteInternetPath satellitePath
        ) {
            return new RoutePlan(
                    true,
                    true,
                    true,
                    true,
                    rackIp,
                    host,
                    rack,
                    distanceBlocks,
                    satellitePath,
                    "",
                    "Satellite backhaul selected because distance is at least 5 km."
            );
        }

        public String rackPosText() {
            return rack == null
                    ? "unknown"
                    : rack.getBlockPos()
                    .toShortString();
        }

        public String summary() {
            if (!targetResolved) {
                return diagnostic;
            }

            if (!physicalDestinationAvailable) {
                return host
                        + " -> "
                        + rackIp
                        + " | physical rack unavailable";
            }

            String distance =
                    LongHaulRoutePolicy.describeDistance(
                            distanceBlocks
                    );

            if (!satelliteRequired) {
                return host
                        + " -> "
                        + rackIp
                        + " @ "
                        + rackPosText()
                        + " | "
                        + distance
                        + " | TERRESTRIAL (<5 km)";
            }

            if (!backhaulAvailable) {
                return host
                        + " -> "
                        + rackIp
                        + " @ "
                        + rackPosText()
                        + " | "
                        + distance
                        + " | SATELLITE REQUIRED (>=5 km) | "
                        + backhaulError;
            }

            return host
                    + " -> "
                    + rackIp
                    + " @ "
                    + rackPosText()
                    + " | "
                    + distance
                    + " | "
                    + SatelliteBackhaulService
                    .routeSummary(
                            satellitePath
                    );
        }
    }
}
