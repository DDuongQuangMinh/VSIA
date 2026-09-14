package com.k1ngtle.vsia.signality.radar.network;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record RadarMeasurement(
        UUID emitterId,
        UUID sourceTargetId,
        long measurementTick,
        Vec3 sensorPosition,
        Vec3 position,
        Vec3 velocity,
        double rangeMeters,
        double bearingRad,
        double elevationRad,
        double radialVelocityMps,
        double snrLinear,
        double scrLinear,
        double positionVarianceMeters2,
        double radialVelocityVarianceMps2,
        boolean trackQuality
) {
}
