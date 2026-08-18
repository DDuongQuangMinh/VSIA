package com.k1ngtle.vsia.signality.engineering.channel;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RfAntennaTransform {
    private RfAntennaTransform() {
    }

    public static RfAntennaState toWorld(
            Level level,
            Vec3 localOrigin,
            RfAntennaState localAntenna
    ) {
        if (localAntenna == null) {
            return RfAntennaState.isotropic();
        }

        Vec3 worldBoresight =
                VsWorldPoseResolver.directionToWorld(
                        level,
                        localOrigin,
                        localAntenna.boresight()
                );

        return new RfAntennaState(
                localAntenna.pattern(),
                localAntenna.polarization(),
                worldBoresight,
                localAntenna.peakGainDbi(),
                localAntenna.horizontalBeamwidthDeg(),
                localAntenna.verticalBeamwidthDeg(),
                localAntenna.frontToBackRatioDb()
        );
    }
}
