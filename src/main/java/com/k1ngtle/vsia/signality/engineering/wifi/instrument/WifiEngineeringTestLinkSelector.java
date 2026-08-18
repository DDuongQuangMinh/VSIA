package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

public final class WifiEngineeringTestLinkSelector {
    private WifiEngineeringTestLinkSelector() {
    }

    public static boolean frequenciesOverlap(
            double frequencyAHz,
            double bandwidthAHz,
            double frequencyBHz,
            double bandwidthBHz
    ) {
        if (!Double.isFinite(frequencyAHz)
                || !Double.isFinite(frequencyBHz)
                || !Double.isFinite(bandwidthAHz)
                || !Double.isFinite(bandwidthBHz)
                || bandwidthAHz <= 0.0
                || bandwidthBHz <= 0.0) {
            return false;
        }

        double halfA =
                bandwidthAHz / 2.0;

        double halfB =
                bandwidthBHz / 2.0;

        return Math.abs(
                frequencyAHz - frequencyBHz
        ) <= halfA + halfB;
    }

    public static double candidateScore(
            double distanceBlocks,
            boolean exactProfile,
            boolean exactFrequency
    ) {
        double score =
                Math.max(
                        0.0,
                        distanceBlocks
                );

        if (!exactProfile) {
            score +=
                    1000.0;
        }

        if (!exactFrequency) {
            score +=
                    100.0;
        }

        return score;
    }
}
