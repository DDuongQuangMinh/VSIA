package com.k1ngtle.vsia.signality.engineering.channel;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.UUID;

public final class RfChannelEnvironment {
    private RfChannelEnvironment() {
    }

    public static RfChannelAssessment assess(
            ServerLevel level,
            UUID receiverId,
            UUID desiredTransmissionId,
            Vec3 receiverPosition,
            double rawDesiredPowerWatts,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double receiverNoiseFigureDb
    ) {
        return assess(
                level,
                receiverId,
                desiredTransmissionId,
                receiverPosition,
                rawDesiredPowerWatts,
                receiverCenterFrequencyHz,
                receiverBandwidthHz,
                receiverNoiseFigureDb,
                RfAntennaState.isotropic(),
                Vec3.ZERO
        );
    }

    public static RfChannelAssessment assess(
            ServerLevel level,
            UUID receiverId,
            UUID desiredTransmissionId,
            Vec3 receiverPosition,
            double rawDesiredPowerWatts,
            double receiverCenterFrequencyHz,
            double receiverBandwidthHz,
            double receiverNoiseFigureDb,
            RfAntennaState receiverAntenna,
            Vec3 receiverVelocityMetersPerSecond
    ) {
        long tick = level.getGameTime();
        String dimensionId = level.dimension().location().toString();

        ActiveRfTransmission desired =
                RfTransmissionRegistry.get(
                        desiredTransmissionId,
                        tick
                );

        double materialLossDb = 0.0;
        double shadowingDb = 0.0;
        double fadingDb = 0.0;
        double txDirectionalGainDbi = 0.0;
        double rxDirectionalGainDbi = 0.0;
        double polarizationLossDb = 0.0;
        double radialRelativeVelocityMps = 0.0;
        double dopplerHz = 0.0;

        if (desired != null) {
            Vec3 txToRx =
                    receiverPosition.subtract(
                            desired.transmitterPosition()
                    );

            Vec3 directionTxToRx =
                    safeDirection(
                            txToRx
                    );

            Vec3 directionRxToTx =
                    directionTxToRx.scale(
                            -1.0
                    );

            materialLossDb =
                    MaterialAttenuationModel.estimateLossDb(
                            level,
                            desired.transmitterPosition(),
                            receiverPosition,
                            receiverCenterFrequencyHz
                    );

            shadowingDb =
                    StableShadowing.offsetDb(
                            dimensionId,
                            desired.transmitterPosition(),
                            receiverPosition,
                            receiverCenterFrequencyHz
                    );

            FadingModel fadingModel =
                    materialLossDb > 0.1
                            ? FadingModel.RAYLEIGH
                            : FadingModel.RICIAN;

            fadingDb =
                    SmallScaleFading.fadingDb(
                            desiredTransmissionId,
                            receiverId,
                            tick,
                            fadingModel,
                            6.0
                    );

            double legacyTxGainDbi =
                    linearGainToDbi(
                            desired.antennaGainLinear()
                    );

            double actualTxGainDbi =
                    effectiveTransmitGainDbi(
                            desired,
                            directionTxToRx
                    );

            txDirectionalGainDbi =
                    actualTxGainDbi
                            - legacyTxGainDbi;

            rxDirectionalGainDbi =
                    AntennaPatternModel.gainTowardDbi(
                            receiverAntenna,
                            directionRxToTx
                    );

            polarizationLossDb =
                    PolarizationLossModel.mismatchLossDb(
                            desired.antennaState()
                                    .polarization(),
                            receiverAntenna.polarization()
                    );

            Vec3 relativeVelocity =
                    receiverVelocityMetersPerSecond.subtract(
                            desired.transmitterVelocityMetersPerSecond()
                    );

            double separationRateMps =
                    relativeVelocity.dot(
                            directionTxToRx
                    );

            radialRelativeVelocityMps =
                    -separationRateMps;

            dopplerHz =
                    DopplerModel.shiftHz(
                            receiverCenterFrequencyHz,
                            radialRelativeVelocityMps
                    );
        }

        double totalDesiredAdjustmentDb =
                -materialLossDb
                        + shadowingDb
                        + fadingDb
                        + txDirectionalGainDbi
                        + rxDirectionalGainDbi
                        - polarizationLossDb;

        double effectiveDesiredPowerWatts =
                rawDesiredPowerWatts
                        * dbToLinear(
                        totalDesiredAdjustmentDb
                );

        double interferenceWatts = 0.0;
        int overlappingInterferers = 0;

        if (RfChannelSettings.ENABLE_INTERFERENCE) {
            Collection<ActiveRfTransmission> active =
                    RfTransmissionRegistry.activeInDimension(
                            dimensionId,
                            tick
                    );

            for (ActiveRfTransmission interferer : active) {
                if (interferer.transmissionId()
                        .equals(
                                desiredTransmissionId
                        )) {
                    continue;
                }

                double spectralOverlap =
                        RfChannelSettings.ENABLE_SPECTRAL_OVERLAP
                                ? SpectralOverlap.fractionOfReceiverBandwidth(
                                receiverCenterFrequencyHz,
                                receiverBandwidthHz,
                                interferer.centerFrequencyHz(),
                                interferer.bandwidthHz()
                        )
                                : (
                                Math.abs(
                                        receiverCenterFrequencyHz
                                                - interferer.centerFrequencyHz()
                                )
                                        <= receiverBandwidthHz / 2.0
                                        ? 1.0
                                        : 0.0
                        );

                double temporalOverlap =
                        desired == null
                                ? (
                                interferer.activeAt(
                                        tick
                                )
                                        ? 1.0
                                        : 0.0
                        )
                                : TemporalOverlap.fractionOfDesired(
                                desired.startTick(),
                                desired.endTick(),
                                interferer.startTick(),
                                interferer.endTick()
                        );

                double overlap =
                        spectralOverlap
                                * temporalOverlap;

                if (overlap <= 0.0) {
                    continue;
                }

                Vec3 txToRx =
                        receiverPosition.subtract(
                                interferer.transmitterPosition()
                        );

                Vec3 directionTxToRx =
                        safeDirection(
                                txToRx
                        );

                Vec3 directionRxToTx =
                        directionTxToRx.scale(
                                -1.0
                        );

                double distance =
                        Math.max(
                                0.01,
                                txToRx.length()
                        );

                double pathLossDb =
                        RfMath.freeSpacePathLossDb(
                                distance,
                                interferer.centerFrequencyHz()
                        );

                double txPowerDbm =
                        RfMath.wattsToDbm(
                                Math.max(
                                        interferer.transmitPowerWatts(),
                                        1.0E-30
                                )
                        );

                double txGainDbi =
                        effectiveTransmitGainDbi(
                                interferer,
                                directionTxToRx
                        );

                double rxGainDbi =
                        AntennaPatternModel.gainTowardDbi(
                                receiverAntenna,
                                directionRxToTx
                        );

                double polarizationDb =
                        PolarizationLossModel.mismatchLossDb(
                                interferer.antennaState()
                                        .polarization(),
                                receiverAntenna.polarization()
                        );

                double materialDb =
                        MaterialAttenuationModel.estimateLossDb(
                                level,
                                interferer.transmitterPosition(),
                                receiverPosition,
                                interferer.centerFrequencyHz()
                        );

                double shadowDb =
                        StableShadowing.offsetDb(
                                dimensionId,
                                interferer.transmitterPosition(),
                                receiverPosition,
                                interferer.centerFrequencyHz()
                        );

                double fadingInterfererDb =
                        SmallScaleFading.fadingDb(
                                interferer.transmissionId(),
                                receiverId,
                                tick,
                                materialDb > 0.1
                                        ? FadingModel.RAYLEIGH
                                        : FadingModel.RICIAN,
                                6.0
                        );

                double receivedDbm =
                        txPowerDbm
                                + txGainDbi
                                + rxGainDbi
                                - pathLossDb
                                - materialDb
                                - polarizationDb
                                + shadowDb
                                + fadingInterfererDb;

                double receivedWatts =
                        RfMath.dbmToWatts(
                                receivedDbm
                        );

                interferenceWatts +=
                        receivedWatts
                                * overlap;

                overlappingInterferers++;
            }
        }

        double noiseDbm =
                RfMath.noiseFloorDbm(
                        receiverBandwidthHz,
                        RfMath.STANDARD_TEMPERATURE_K,
                        receiverNoiseFigureDb
                );

        double noiseWatts =
                RfMath.dbmToWatts(
                        noiseDbm
                );

        double rawSnrDb =
                ratioDb(
                        effectiveDesiredPowerWatts,
                        noiseWatts
                );

        double sinrDb =
                ratioDb(
                        effectiveDesiredPowerWatts,
                        noiseWatts
                                + interferenceWatts
                );

        return new RfChannelAssessment(
                rawDesiredPowerWatts,
                effectiveDesiredPowerWatts,
                interferenceWatts,
                noiseWatts,
                rawSnrDb,
                sinrDb,
                materialLossDb,
                shadowingDb,
                fadingDb,
                txDirectionalGainDbi,
                rxDirectionalGainDbi,
                polarizationLossDb,
                radialRelativeVelocityMps,
                dopplerHz,
                overlappingInterferers
        );
    }

    public static double equivalentSignalPowerForSinr(
            RfChannelAssessment assessment
    ) {
        if (!Double.isFinite(
                assessment.sinrDb()
        )) {
            return assessment.sinrDb()
                    > 0.0
                    ? Double.MAX_VALUE
                    : 0.0;
        }

        return assessment.noisePowerWatts()
                * Math.pow(
                10.0,
                assessment.sinrDb()
                        / 10.0
        );
    }

    public static long estimateAirtimeTicks(
            long payloadBits,
            double bandwidthHz
    ) {
        double conservativeBitRate =
                Math.max(
                        1.0,
                        bandwidthHz
                                * 0.50
                );

        double seconds =
                Math.max(
                        0.0,
                        payloadBits
                                / conservativeBitRate
                );

        return Math.max(
                1L,
                (long) Math.ceil(
                        seconds
                                * 20.0
                )
        );
    }

    private static double effectiveTransmitGainDbi(
            ActiveRfTransmission transmission,
            Vec3 direction
    ) {
        RfAntennaState antenna =
                transmission.antennaState();

        if (antenna == null
                || (
                antenna.pattern()
                        == RfAntennaPattern.ISOTROPIC
                        && Math.abs(
                        antenna.peakGainDbi()
                ) < 1.0E-12
                        && antenna.polarization()
                        == RfPolarization.UNKNOWN
        )) {
            return linearGainToDbi(
                    transmission.antennaGainLinear()
            );
        }

        return AntennaPatternModel.gainTowardDbi(
                antenna,
                direction
        );
    }

    private static Vec3 safeDirection(
            Vec3 vector
    ) {
        if (vector == null
                || vector.lengthSqr()
                < 1.0E-18) {
            return new Vec3(
                    0.0,
                    0.0,
                    1.0
            );
        }

        return vector.normalize();
    }

    private static double ratioDb(
            double numerator,
            double denominator
    ) {
        if (numerator <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        if (denominator <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                numerator
                        / denominator
        );
    }

    private static double dbToLinear(
            double db
    ) {
        return Math.pow(
                10.0,
                db / 10.0
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
