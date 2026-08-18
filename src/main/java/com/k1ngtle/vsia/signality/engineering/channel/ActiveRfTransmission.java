package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record ActiveRfTransmission(
        UUID transmissionId,
        UUID transmitterId,
        String dimensionId,
        Vec3 transmitterPosition,
        double centerFrequencyHz,
        double bandwidthHz,
        double transmitPowerWatts,
        double antennaGainLinear,
        RfAntennaState antennaState,
        Vec3 transmitterVelocityMetersPerSecond,
        long startTick,
        long endTick,
        long payloadBits
) {
    public ActiveRfTransmission {
        if (transmissionId == null) {
            throw new IllegalArgumentException(
                    "transmissionId"
            );
        }

        if (transmitterId == null) {
            throw new IllegalArgumentException(
                    "transmitterId"
            );
        }

        if (dimensionId == null) {
            dimensionId = "";
        }

        if (transmitterPosition == null) {
            throw new IllegalArgumentException(
                    "transmitterPosition"
            );
        }

        antennaState =
                antennaState == null
                        ? RfAntennaState.isotropic()
                        : antennaState;

        transmitterVelocityMetersPerSecond =
                transmitterVelocityMetersPerSecond == null
                        ? Vec3.ZERO
                        : transmitterVelocityMetersPerSecond;

        if (centerFrequencyHz <= 0.0) {
            throw new IllegalArgumentException(
                    "centerFrequencyHz"
            );
        }

        if (bandwidthHz <= 0.0) {
            throw new IllegalArgumentException(
                    "bandwidthHz"
            );
        }
    }

    public ActiveRfTransmission(
            UUID transmissionId,
            UUID transmitterId,
            String dimensionId,
            Vec3 transmitterPosition,
            double centerFrequencyHz,
            double bandwidthHz,
            double transmitPowerWatts,
            double antennaGainLinear,
            long startTick,
            long endTick,
            long payloadBits
    ) {
        this(
                transmissionId,
                transmitterId,
                dimensionId,
                transmitterPosition,
                centerFrequencyHz,
                bandwidthHz,
                transmitPowerWatts,
                antennaGainLinear,
                RfAntennaState.isotropic(),
                Vec3.ZERO,
                startTick,
                endTick,
                payloadBits
        );
    }

    public boolean activeAt(
            long tick
    ) {
        return tick >= startTick
                && tick <= endTick;
    }
}
