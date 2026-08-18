package com.k1ngtle.vsia.signality.engineering.reality;

public final class RfPropagationDelayModel {
    public static final double SPEED_OF_LIGHT_MPS =
            299_792_458.0;

    private RfPropagationDelayModel() {
    }

    public static double delaySeconds(
            double distanceMeters
    ) {
        return Math.max(
                0.0,
                distanceMeters
        )
                / SPEED_OF_LIGHT_MPS;
    }

    public static double delayMicros(
            double distanceMeters
    ) {
        return delaySeconds(
                distanceMeters
        )
                * 1_000_000.0;
    }
}
