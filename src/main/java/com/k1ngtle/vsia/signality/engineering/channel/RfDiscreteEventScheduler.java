package com.k1ngtle.vsia.signality.engineering.channel;

import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.engineering.math.RfMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RfDiscreteEventScheduler {
    private static final Map<UUID, ScheduledRfTransmission> PENDING =
            new LinkedHashMap<>();

    private static boolean processing;

    private RfDiscreteEventScheduler() {
    }

    public static synchronized void schedule(
            ScheduledRfTransmission transmission
    ) {
        PENDING.put(
                transmission.metadata()
                        .transmissionId(),
                transmission
        );
    }

    public static void tick(
            ServerLevel level
    ) {
        List<ScheduledRfTransmission> due =
                collectDue(
                        level
                );

        if (due.isEmpty()) {
            return;
        }

        for (ScheduledRfTransmission transmission : due) {
            RfTransmissionRegistry.register(
                    transmission.metadata()
            );
        }

        synchronized (RfDiscreteEventScheduler.class) {
            processing =
                    true;
        }

        try {
            for (ScheduledRfTransmission transmission : due) {
                SignalBus.broadcast(
                        transmission.packet(),
                        level
                );
            }
        } finally {
            synchronized (RfDiscreteEventScheduler.class) {
                processing =
                        false;
            }
        }

        removeDelivered(
                due
        );
    }

    public static synchronized boolean processing() {
        return processing;
    }

    public static synchronized int pendingCount() {
        return PENDING.size();
    }

    public static synchronized void clear() {
        PENDING.clear();
        processing =
                false;
    }

    public static RfMediumState sense(
            ServerLevel level,
            Vec3 receiverPosition,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double busyThresholdDbm
    ) {
        return sense(
                level,
                receiverPosition,
                receiverCenterFrequencyHz,
                receiverBandwidthHz,
                busyThresholdDbm,
                RfAntennaState.isotropic()
        );
    }

    public static RfMediumState sense(
            ServerLevel level,
            Vec3 receiverPosition,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double busyThresholdDbm,
            RfAntennaState receiverAntenna
    ) {
        return senseAtTick(
                level,
                level.getGameTime(),
                receiverPosition,
                receiverCenterFrequencyHz,
                receiverBandwidthHz,
                busyThresholdDbm,
                receiverAntenna
        );
    }

    public static RfMediumState senseNextDeliveryTick(
            ServerLevel level,
            Vec3 receiverPosition,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double busyThresholdDbm,
            RfAntennaState receiverAntenna
    ) {
        long deliveryTick =
                level.getGameTime()
                        + Math.max(
                        1L,
                        RfChannelSettings
                                .MIN_EVENT_LATENCY_TICKS
                );

        return senseAtTick(
                level,
                deliveryTick,
                receiverPosition,
                receiverCenterFrequencyHz,
                receiverBandwidthHz,
                busyThresholdDbm,
                receiverAntenna
        );
    }

    private static RfMediumState senseAtTick(
            ServerLevel level,
            long tick,
            Vec3 receiverPosition,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double busyThresholdDbm,
            RfAntennaState receiverAntenna
    ) {
        String dimensionId =
                level.dimension()
                        .location()
                        .toString();

        List<ActiveRfTransmission> candidates =
                new ArrayList<>();

        Set<UUID> seen =
                new HashSet<>();

        Collection<ActiveRfTransmission> active =
                RfTransmissionRegistry.activeInDimension(
                        dimensionId,
                        tick
                );

        for (ActiveRfTransmission transmission : active) {
            if (seen.add(
                    transmission.transmissionId()
            )) {
                candidates.add(
                        transmission
                );
            }
        }

        synchronized (RfDiscreteEventScheduler.class) {
            for (ScheduledRfTransmission scheduled : PENDING.values()) {
                ActiveRfTransmission transmission =
                        scheduled.metadata();

                if (!transmission.dimensionId()
                        .equals(
                                dimensionId
                        )) {
                    continue;
                }

                if (!intervalContainsOrTouches(
                        transmission,
                        tick
                )) {
                    continue;
                }

                if (seen.add(
                        transmission.transmissionId()
                )) {
                    candidates.add(
                            transmission
                    );
                }
            }
        }

        double energyWatts =
                0.0;

        int overlapping =
                0;

        for (ActiveRfTransmission transmission : candidates) {
            double overlap =
                    SpectralOverlap.fractionOfReceiverBandwidth(
                            receiverCenterFrequencyHz,
                            receiverBandwidthHz,
                            transmission.centerFrequencyHz(),
                            transmission.bandwidthHz()
                    );

            if (overlap <= 0.0) {
                continue;
            }

            double receivedWatts =
                    estimateReceivedPowerWatts(
                            level,
                            transmission,
                            receiverPosition,
                            receiverAntenna
                    );

            energyWatts +=
                    receivedWatts
                            * overlap;

            overlapping++;
        }

        double energyDbm =
                energyWatts <= 0.0
                        ? Double.NEGATIVE_INFINITY
                        : RfMath.wattsToDbm(
                        energyWatts
                );

        return new RfMediumState(
                energyWatts,
                energyDbm,
                overlapping,
                energyDbm
                        >= busyThresholdDbm
        );
    }

    private static synchronized List<ScheduledRfTransmission> collectDue(
            ServerLevel level
    ) {
        long tick =
                level.getGameTime();

        String dimensionId =
                level.dimension()
                        .location()
                        .toString();

        List<ScheduledRfTransmission> due =
                new ArrayList<>();

        for (ScheduledRfTransmission transmission : PENDING.values()) {
            ActiveRfTransmission metadata =
                    transmission.metadata();

            if (!metadata.dimensionId()
                    .equals(
                            dimensionId
                    )) {
                continue;
            }

            if (metadata.startTick()
                    <= tick) {
                due.add(
                        transmission
                );
            }
        }

        return due;
    }

    private static synchronized void removeDelivered(
            List<ScheduledRfTransmission> delivered
    ) {
        for (ScheduledRfTransmission transmission : delivered) {
            PENDING.remove(
                    transmission.metadata()
                            .transmissionId()
            );
        }
    }

    private static boolean intervalContainsOrTouches(
            ActiveRfTransmission transmission,
            long tick
    ) {
        return tick >= transmission.startTick()
                && tick <= transmission.endTick();
    }

    private static double estimateReceivedPowerWatts(
            ServerLevel level,
            ActiveRfTransmission transmission,
            Vec3 receiverPosition,
            RfAntennaState receiverAntenna
    ) {
        double distance =
                Math.max(
                        0.01,
                        transmission
                                .transmitterPosition()
                                .distanceTo(
                                        receiverPosition
                                )
                );

        double pathLossDb =
                RfMath.freeSpacePathLossDb(
                        distance,
                        transmission.centerFrequencyHz()
                );

        double txPowerDbm =
                RfMath.wattsToDbm(
                        Math.max(
                                transmission.transmitPowerWatts(),
                                1.0E-30
                        )
                );

        Vec3 txToRx =
                receiverPosition.subtract(
                        transmission.transmitterPosition()
                );

        Vec3 directionTxToRx =
                txToRx.lengthSqr() < 1.0E-18
                        ? new Vec3(
                        0.0,
                        0.0,
                        1.0
                )
                        : txToRx.normalize();

        Vec3 directionRxToTx =
                directionTxToRx.scale(
                        -1.0
                );

        double antennaGainDbi =
                transmission.antennaState() == null
                        || (
                        transmission.antennaState().pattern()
                                == RfAntennaPattern.ISOTROPIC
                                && Math.abs(
                                transmission.antennaState().peakGainDbi()
                        ) < 1.0E-12
                                && transmission.antennaState().polarization()
                                == RfPolarization.UNKNOWN
                )
                        ? linearGainToDbi(
                        transmission.antennaGainLinear()
                )
                        : AntennaPatternModel.gainTowardDbi(
                        transmission.antennaState(),
                        directionTxToRx
                );

        double receiverGainDbi =
                AntennaPatternModel.gainTowardDbi(
                        receiverAntenna,
                        directionRxToTx
                );

        double polarizationLossDb =
                PolarizationLossModel.mismatchLossDb(
                        transmission.antennaState().polarization(),
                        receiverAntenna.polarization()
                );

        double materialLossDb =
                MaterialAttenuationModel.estimateLossDb(
                        level,
                        transmission.transmitterPosition(),
                        receiverPosition,
                        transmission.centerFrequencyHz()
                );

        String dimensionId =
                level.dimension()
                        .location()
                        .toString();

        double shadowingDb =
                StableShadowing.offsetDb(
                        dimensionId,
                        transmission.transmitterPosition(),
                        receiverPosition,
                        transmission.centerFrequencyHz()
                );

        double fadingDb =
                SmallScaleFading.fadingDb(
                        transmission.transmissionId(),
                        UUID.nameUUIDFromBytes(
                                (
                                        receiverPosition.x
                                                + ":"
                                                + receiverPosition.y
                                                + ":"
                                                + receiverPosition.z
                                ).getBytes(
                                        java.nio.charset.StandardCharsets.UTF_8
                                )
                        ),
                        level.getGameTime(),
                        materialLossDb > 0.1
                                ? FadingModel.RAYLEIGH
                                : FadingModel.RICIAN,
                        6.0
                );

        double receivedDbm =
                txPowerDbm
                        + antennaGainDbi
                        + receiverGainDbi
                        - polarizationLossDb
                        - pathLossDb
                        - materialLossDb
                        + shadowingDb
                        + fadingDb;

        return RfMath.dbmToWatts(
                receivedDbm
        );
    }

    private static double linearGainToDbi(
            double linear
    ) {
        if (linear <= 0.0) {
            return 0.0;
        }

        return 10.0
                * Math.log10(
                linear
        );
    }
}
