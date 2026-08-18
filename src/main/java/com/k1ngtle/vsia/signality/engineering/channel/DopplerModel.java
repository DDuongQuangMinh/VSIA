package com.k1ngtle.vsia.signality.engineering.channel;

public final class DopplerModel {
    public static final double SPEED_OF_LIGHT_MPS =
            299_792_458.0;

    private DopplerModel() {
    }

    public static double shiftHz(
            double carrierFrequencyHz,
            double radialRelativeVelocityMetersPerSecond
    ) {
        return (
                radialRelativeVelocityMetersPerSecond
                        / SPEED_OF_LIGHT_MPS
        )
                * carrierFrequencyHz;
    }
}
