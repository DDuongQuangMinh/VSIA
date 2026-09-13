package com.k1ngtle.vsia.signality.internet.satellite;

import com.k1ngtle.vsia.signality.internet.satellite.device.TemporarySatelliteTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class SatelliteNetworkManager {
    private static final Map<ServerLevel, Map<BlockPos, TemporarySatelliteTerminalBlockEntity>>
            TERMINALS =
            new IdentityHashMap<>();

    private static final Map<ServerLevel, List<ScheduledSatellitePacket>>
            PENDING =
            new IdentityHashMap<>();

    private SatelliteNetworkManager() {
    }

    public static synchronized void register(
            TemporarySatelliteTerminalBlockEntity terminal
    ) {
        if (terminal == null
                || !(terminal.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        TERMINALS
                .computeIfAbsent(
                        level,
                        ignored ->
                                new LinkedHashMap<>()
                )
                .put(
                        terminal.getBlockPos()
                                .immutable(),
                        terminal
                );
    }

    public static synchronized void unregister(
            TemporarySatelliteTerminalBlockEntity terminal
    ) {
        if (terminal == null
                || !(terminal.getLevel()
                instanceof ServerLevel level)) {
            return;
        }

        Map<BlockPos, TemporarySatelliteTerminalBlockEntity> byPos =
                TERMINALS.get(
                        level
                );

        if (byPos == null) {
            return;
        }

        byPos.remove(
                terminal.getBlockPos()
        );

        if (byPos.isEmpty()) {
            TERMINALS.remove(
                    level
            );
        }
    }

    public static synchronized List<TemporarySatelliteTerminalBlockEntity> terminals(
            ServerLevel level
    ) {
        Map<BlockPos, TemporarySatelliteTerminalBlockEntity> byPos =
                TERMINALS.get(
                        level
                );

        if (byPos == null) {
            return List.of();
        }

        return List.copyOf(
                byPos.values()
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

        TemporarySatelliteTerminalBlockEntity best =
                null;

        double bestDistance =
                Double.POSITIVE_INFINITY;

        for (TemporarySatelliteTerminalBlockEntity candidate
                : terminals(level)) {
            if (candidate == source
                    || candidate.isRemoved()) {
                continue;
            }

            double distance =
                    source.getBlockPos()
                            .distSqr(
                                    candidate
                                            .getBlockPos()
                            );

            if (distance < bestDistance) {
                bestDistance =
                        distance;

                best =
                        candidate;
            }
        }

        return best;
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
            return SatelliteLinkAssessment
                    .unavailable();
        }

        Vec3 sourceGround =
                SatelliteOrbitModel
                        .groundPositionEcef(
                                source.getBlockPos()
                        );

        Vec3 targetGround =
                SatelliteOrbitModel
                        .groundPositionEcef(
                                target.getBlockPos()
                        );

        double seconds =
                SatelliteOrbitModel
                        .simulationSeconds(
                                level
                        );

        SatelliteLinkAssessment best =
                SatelliteLinkAssessment
                        .unavailable();

        for (SatelliteOrbitState satellite
                : SatelliteOrbitModel
                .constellation(seconds)) {

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
                    || assessment
                    .bottleneckSnrDb()
                    > best
                    .bottleneckSnrDb()) {
                best =
                        assessment;
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

    public static boolean sendPacket(
            TemporarySatelliteTerminalBlockEntity source,
            TemporarySatelliteTerminalBlockEntity target,
            String message
    ) {
        if (source == null
                || target == null
                || !(source.getLevel()
                instanceof ServerLevel level)
                || source.getLevel()
                != target.getLevel()) {
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
                    "No common satellite visible"
            );

            return false;
        }

        long seed =
                source.getBlockPos()
                        .asLong()
                        ^ Long.rotateLeft(
                        target.getBlockPos()
                                .asLong(),
                        17
                )
                        ^ level.getGameTime();

        boolean survivesLink =
                new Random(seed)
                        .nextDouble()
                        <= assessment
                        .packetSuccessProbability();

        SatellitePacket packet =
                source.createPacket(
                        target.terminalId(),
                        message
                );

        long delayTicks =
                Math.max(
                        1L,
                        (long) Math.ceil(
                                assessment
                                        .propagationDelayMs()
                                        / 50.0
                        )
                );

        ScheduledSatellitePacket scheduled =
                new ScheduledSatellitePacket(
                        source.terminalId(),
                        target.terminalId(),
                        packet,
                        assessment,
                        level.getGameTime()
                                + delayTicks,
                        survivesLink
                );

        synchronized (SatelliteNetworkManager.class) {
            PENDING
                    .computeIfAbsent(
                            level,
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(
                            scheduled
                    );
        }

        source.setStatus(
                "SATCOM datagram seq="
                        + packet.sequenceNumber()
                        + " queued via "
                        + assessment.satelliteName()
                        + " ("
                        + String.format(
                                java.util.Locale.ROOT,
                                "%.2f ms",
                                assessment.propagationDelayMs()
                        )
                        + ")"
        );

        return true;
    }

    public static void tick(
            ServerLevel level
    ) {
        List<ScheduledSatellitePacket> due =
                new ArrayList<>();

        synchronized (SatelliteNetworkManager.class) {
            List<ScheduledSatellitePacket> queue =
                    PENDING.get(
                            level
                    );

            if (queue == null
                    || queue.isEmpty()) {
                return;
            }

            Iterator<ScheduledSatellitePacket> iterator =
                    queue.iterator();

            while (iterator.hasNext()) {
                ScheduledSatellitePacket scheduled =
                        iterator.next();

                if (scheduled.dueTick()
                        <= level.getGameTime()) {
                    due.add(
                            scheduled
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

        for (ScheduledSatellitePacket scheduled
                : due) {
            TemporarySatelliteTerminalBlockEntity source =
                    terminalById(
                            level,
                            scheduled.sourceTerminalId()
                    );

            TemporarySatelliteTerminalBlockEntity target =
                    terminalById(
                            level,
                            scheduled.targetTerminalId()
                    );

            if (target == null) {
                if (source != null) {
                    source.setStatus(
                            "SATCOM destination unavailable before arrival"
                    );
                }

                continue;
            }

            if (!scheduled.survivesLink()) {
                if (source != null) {
                    source.setStatus(
                            "SATCOM datagram lost at link layer"
                    );
                }

                continue;
            }

            target.receivePacket(
                    scheduled.packet(),
                    scheduled.assessment()
            );

            if (source != null) {
                source.setStatus(
                        "SATCOM datagram seq="
                                + scheduled.packet()
                                .sequenceNumber()
                                + " delivered via "
                                + scheduled.assessment()
                                .satelliteName()
                );
            }
        }
    }

    private static TemporarySatelliteTerminalBlockEntity terminalById(
            ServerLevel level,
            UUID terminalId
    ) {
        for (TemporarySatelliteTerminalBlockEntity terminal
                : terminals(level)) {
            if (!terminal.isRemoved()
                    && terminal.terminalId()
                    .equals(
                            terminalId
                    )) {
                return terminal;
            }
        }

        return null;
    }

    public static synchronized int pendingCount(
            ServerLevel level
    ) {
        List<ScheduledSatellitePacket> queue =
                PENDING.get(
                        level
                );

        return queue == null
                ? 0
                : queue.size();
    }

    public static synchronized void clear() {
        TERMINALS.clear();
        PENDING.clear();
    }

    private record ScheduledSatellitePacket(
            UUID sourceTerminalId,
            UUID targetTerminalId,
            SatellitePacket packet,
            SatelliteLinkAssessment assessment,
            long dueTick,
            boolean survivesLink
    ) {
    }
}
