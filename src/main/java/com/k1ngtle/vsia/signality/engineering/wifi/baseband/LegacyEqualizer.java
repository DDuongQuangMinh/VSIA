package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyEqualizer {
    private LegacyEqualizer() {
    }

    public static Complex[] equalize(
            Complex[] receivedBins,
            LegacyChannelEstimate estimate
    ) {
        Complex[] channel =
                estimate.frequencyResponse();

        if (receivedBins.length
                != channel.length) {
            throw new IllegalArgumentException(
                    "FFT/channel-estimate size mismatch"
            );
        }

        Complex[] result =
                new Complex[
                        receivedBins.length
                        ];

        for (int i = 0;
             i < result.length;
             i++) {
            if (channel[i].magnitudeSquared()
                    < 1.0E-18) {
                result[i] =
                        receivedBins[i];
            } else {
                result[i] =
                        WifiComplexMath.divide(
                                receivedBins[i],
                                channel[i]
                        );
            }
        }

        return result;
    }
}
