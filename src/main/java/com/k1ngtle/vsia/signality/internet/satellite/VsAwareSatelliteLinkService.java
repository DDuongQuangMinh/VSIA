package com.k1ngtle.vsia.signality.internet.satellite;

import com.k1ngtle.vsia.signality.integration.vs.VsNetworkPosition;
import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class VsAwareSatelliteLinkService {
    private static final Map<ServerLevel, List<PendingPacket>>
            PENDING =
            new IdentityHashMap<>();

    private VsAwareSatelliteLinkService() {
    }

    public static Vec3 terminalWorldPosition(
            TemporarySatelliteTerminalBlockEntity terminal
    ) {
        if (terminal == null
                || !(terminal.getLevel()
                instanceof ServerLevel level)) {
            return terminal == null
                    ? Vec3.ZERO
                    : Vec3.atCenterOf(
                    terminal.getBlockPos()
            );
        }

        return VsNetworkPosition.blockCenterWorld(
                level,
                terminal.getBlockPos()
        );
    }

    public static TemporarySatelliteTerminalBlockEntity nearestOther(
            TemporarySatelliteTerminalBlockEntity source
    ) {
        if (source == null
                || !(source.getLevel()
                instanceof ServerLevel level)) {
            return null;
        }

        Vec3 sourceWorld =
                terminalWorldPosition(
                        source
                );

        TemporarySatelliteTerminalBlockEntity best =
                null;

        double bestDistanceSqr =
                Double.POSITIVE_INFINITY;

        for (TemporarySatelliteTerminalBlockEntity candidate
                : SatelliteNetworkManager.terminals(
                level
        )) {
            if (candidate == null
                    || candidate == source
                    || candidate.isRemoved()) {
                continue;
            }

            double distanceSqr =
                    sourceWorld.distanceToSqr(
                            terminalWorldPosition(
                                    candidate
                            )
                    );

            if (distanceSqr
                    < bestDistanceSqr) {
                bestDistanceSqr =
                        distanceSqr;

                best =
                        candidate;
            }
        }

        return best;
    }

    public static SatelliteLinkAssessment assessSelf(
            TemporarySatelliteTerminalBlockEntity terminal
    ) {
        return assess(
                terminal,
                terminal
        );
    }

    public static SatelliteLinkAssessment assess(
            TemporarySatelliteTerminalBlockEntity source,
            TemporarySatelliteTerminalBlockEntity target
    ) {
        if (source == null
                || target == null
                || source.getLevel()
                != target.getLevel()
                || !(source.getLevel()
                instanceof ServerLevel level)
                || source.band()
                != target.band()) {
            return SatelliteLinkAssessment.unavailable();
        }

        Vec3 sourceGround =
                groundPositionEcef(
                        terminalWorldPosition(
                                source
                        )
                );

        Vec3 targetGround =
                groundPositionEcef(
                        terminalWorldPosition(
                                target
                        )
                );

        double seconds =
                SatelliteOrbitModel.simulationSeconds(
                        level
                );

        SatelliteLinkAssessment best =
                SatelliteLinkAssessment.unavailable();

        for (SatelliteOrbitState satellite
                : SatelliteOrbitModel.constellation(
                seconds
        )) {
            SatelliteLinkAssessment assessment =
                    SatelliteLinkMath.assess(
                            sourceGround,
                            targetGround,
                            satellite,
                            source.band(),
                            Math.max(
                                    source.minimumElevationDeg(),
                                    target.minimumElevationDeg()
                            )
                    );

            if (!assessment.visible()) {
                continue;
            }

            if (!best.visible()
                    || assessment.bottleneckSnrDb()
                    > best.bottleneckSnrDb()) {
                best =
                        assessment;
            }
        }

        return best;
    }

    public static boolean sendPacket(
            TemporarySatelliteTerminalBlockEntity source,
            TemporarySatelliteTerminalBlockEntity target,
            String message
    ) {
        if (source == null
                || target == null
                || source.getLevel()
                != target.getLevel()
                || !(source.getLevel()
                instanceof ServerLevel level)) {
            return false;
        }

        SatelliteLinkAssessment assessment =
                assess(
                        source,
                        target
                );

        source.setLastAssessment(
                assessment
        );

        if (!assessment.visible()) {
            source.setStatus(
                    "No common VS-aware satellite link visible"
            );

            return false;
        }

        SatellitePacket packet =
                source.createPacket(
                        target.terminalId(),
                        message
                );

        long seed =
                source.terminalId()
                        .getMostSignificantBits()
                        ^ Long.rotateLeft(
                        target.terminalId()
                                .getLeastSignificantBits(),
                        17
                )
                        ^ Long.rotateLeft(
                        packet.sequenceNumber(),
                        7
                )
                        ^ level.getGameTime();

        boolean survives =
                new Random(
                        seed
                )
                        .nextDouble()
                        <= clamp01(
                        assessment.packetSuccessProbability()
                );

        long delayTicks =
                Math.max(
                        1L,
                        (long) Math.ceil(
                                assessment.propagationDelayMs()
                                        / 50.0
                        )
                );

        synchronized (VsAwareSatelliteLinkService.class) {
            PENDING
                    .computeIfAbsent(
                            level,
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(
                            new PendingPacket(
                                    source.terminalId(),
                                    target.terminalId(),
                                    packet,
                                    assessment,
                                    level.getGameTime()
                                            + delayTicks,
                                    survives
                            )
                    );
        }

        source.setStatus(
                "VS-aware SATCOM datagram seq="
                        + packet.sequenceNumber()
                        + " queued via "
                        + assessment.satelliteName()
        );

        return true;
    }

    public static void tick(
            ServerLevel level
    ) {
        List<PendingPacket> due =
                new ArrayList<>();

        synchronized (VsAwareSatelliteLinkService.class) {
            List<PendingPacket> queue =
                    PENDING.get(
                            level
                    );

            if (queue == null
                    || queue.isEmpty()) {
                return;
            }

            Iterator<PendingPacket> iterator =
                    queue.iterator();

            while (iterator.hasNext()) {
                PendingPacket pending =
                        iterator.next();

                if (pending.dueTick()
                        <= level.getGameTime()) {
                    due.add(
                            pending
                    );

                    iterator.remove();
                }
            }

            if (queue.isEmpty()) {
                PENDING.remove(
                        level
                );
            }
        }

        for (PendingPacket pending
                : due) {
            TemporarySatelliteTerminalBlockEntity source =
                    terminalById(
                            level,
                            pending.sourceTerminalId()
                    );

            TemporarySatelliteTerminalBlockEntity target =
                    terminalById(
                            level,
                            pending.targetTerminalId()
                    );

            if (target == null) {
                if (source != null) {
                    source.setStatus(
                            "VS-aware SATCOM destination unavailable"
                    );
                }

                continue;
            }

            if (!pending.survives()) {
                if (source != null) {
                    source.setStatus(
                            "VS-aware SATCOM datagram lost at link layer"
                    );
                }

                continue;
            }

            target.receivePacket(
                    pending.packet(),
                    pending.assessment()
            );

            if (source != null) {
                source.setStatus(
                        "VS-aware SATCOM datagram seq="
                                + pending.packet()
                                .sequenceNumber()
                                + " delivered via "
                                + pending.assessment()
                                .satelliteName()
                );
            }
        }
    }

    public static synchronized void clear() {
        PENDING.clear();
    }

    private static TemporarySatelliteTerminalBlockEntity terminalById(
            ServerLevel level,
            UUID terminalId
    ) {
        for (TemporarySatelliteTerminalBlockEntity terminal
                : SatelliteNetworkManager.terminals(
                level
        )) {
            if (terminal != null
                    && !terminal.isRemoved()
                    && terminal.terminalId()
                    .equals(
                            terminalId
                    )) {
                return terminal;
            }
        }

        return null;
    }

    private static Vec3 groundPositionEcef(
            Vec3 worldPosition
    ) {
        double altitude =
                Math.max(
                        -64.0,
                        worldPosition.y
                );

        double latitude =
                worldPosition.z
                        / SatelliteOrbitModel
                        .EARTH_RADIUS_METERS;

        latitude =
                Math.max(
                        -Math.toRadians(
                                85.0
                        ),
                        Math.min(
                                Math.toRadians(
                                        85.0
                                ),
                                latitude
                        )
                );

        double cosLat =
                Math.cos(
                        latitude
                );

        double longitude =
                worldPosition.x
                        / Math.max(
                        1.0,
                        SatelliteOrbitModel
                                .EARTH_RADIUS_METERS
                                * cosLat
                );

        double radius =
                SatelliteOrbitModel
                        .EARTH_RADIUS_METERS
                        + altitude;

        return new Vec3(
                radius
                        * cosLat
                        * Math.cos(
                        longitude
                ),
                radius
                        * cosLat
                        * Math.sin(
                        longitude
                ),
                radius
                        * Math.sin(
                        latitude
                )
        );
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

    private record PendingPacket(
            UUID sourceTerminalId,
            UUID targetTerminalId,
            SatellitePacket packet,
            SatelliteLinkAssessment assessment,
            long dueTick,
            boolean survives
    ) {
    }
}
