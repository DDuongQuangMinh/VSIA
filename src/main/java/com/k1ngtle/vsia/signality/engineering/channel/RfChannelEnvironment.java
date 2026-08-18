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
        long tick =
                level.getGameTime();

        String dimensionId =
                level.dimension()
                        .location()
                        .toString();

        ActiveRfTransmission desired =
                RfTransmissionRegistry.get(
                        desiredTransmissionId,
                        tick
                );

        double materialLossDb =
                0.0;

        double shadowingDb =
                0.0;

        double fadingDb =
                0.0;

        if (desired != null) {
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
        }

        double totalDesiredAdjustmentDb =
                -materialLossDb
                        + shadowingDb
                        + fadingDb;

        double effectiveDesiredPowerWatts =
                rawDesiredPowerWatts
                        * dbToLinear(
                        totalDesiredAdjustmentDb
                );

        double interferenceWatts =
                0.0;

        int overlappingInterferers =
                0;

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

                double overlap =
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

                if (overlap <= 0.0) {
                    continue;
                }

                double distance =
                        Math.max(
                                0.01,
                                interferer
                                        .transmitterPosition()
                                        .distanceTo(
                                                receiverPosition
                                        )
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

                double antennaGainDbi =
                        linearGainToDbi(
                                interferer.antennaGainLinear()
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
                                + antennaGainDbi
                                - pathLossDb
                                - materialDb
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
                0.0,
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
