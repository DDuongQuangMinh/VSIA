package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyMultipathChannel {
    private LegacyMultipathChannel() {
    }

    public static Complex[] apply(
            Complex[] input,
            Complex[] taps
    ) {
        if (taps == null
                || taps.length == 0) {
            throw new IllegalArgumentException(
                    "At least one channel tap is required"
            );
        }

        Complex[] output =
                new Complex[
                        input.length
                        ];

        for (int n = 0;
             n < output.length;
             n++) {
            Complex sum =
                    Complex.ZERO;

            for (int tap = 0;
                 tap < taps.length;
                 tap++) {
                int source =
                        n - tap;

                if (source < 0) {
                    break;
                }

                sum =
                        sum.add(
                                input[source]
                                        .multiply(
                                                taps[tap]
                                        )
                        );
            }

            output[n] =
                    sum;
        }

        return output;
    }
}
