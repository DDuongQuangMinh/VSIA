package com.k1ngtle.vsia.signality.engineering.channel;

public final class SpectralOverlap {
    private SpectralOverlap() {
    }

    public static double overlapHz(
            double centerAHz,
            double bandwidthAHz,
            double centerBHz,
            double bandwidthBHz
    ) {
        if (bandwidthAHz <= 0.0
                || bandwidthBHz <= 0.0) {
            return 0.0;
        }

        double aLow =
                centerAHz
                        - bandwidthAHz / 2.0;

        double aHigh =
                centerAHz
                        + bandwidthAHz / 2.0;

        double bLow =
                centerBHz
                        - bandwidthBHz / 2.0;

        double bHigh =
                centerBHz
                        + bandwidthBHz / 2.0;

        return Math.max(
                0.0,
                Math.min(
                        aHigh,
                        bHigh
                )
                        - Math.max(
                        aLow,
                        bLow
                )
        );
    }

    public static double fractionOfReceiverBandwidth(
            double receiverCenterHz,
            double receiverBandwidthHz,
            double interfererCenterHz,
            double interfererBandwidthHz
    ) {
        if (receiverBandwidthHz <= 0.0) {
            return 0.0;
        }

        return clamp01(
                overlapHz(
                        receiverCenterHz,
                        receiverBandwidthHz,
                        interfererCenterHz,
                        interfererBandwidthHz
                )
                        / receiverBandwidthHz
        );
    }

    private static double clamp01(
            double value
    ) {
        return Math.max(
                0.0,
                Math.min(
                        1.0,
                        value
                )
        );
    }
}
