package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

public final class WifiSoftDemapper {
    private WifiSoftDemapper() {
    }

    public static double[] llr(
            Complex[] symbols,
            Modulation modulation,
            double noiseVariance
    ) {
        int bitsPerSymbol =
                modulation.bitsPerSymbol();

        int constellationSize =
                modulation.order();

        int[][] labels =
                new int[
                        constellationSize
                        ][
                        bitsPerSymbol
                        ];

        Complex[] points =
                new Complex[
                        constellationSize
                        ];

        for (int value = 0;
             value < constellationSize;
             value++) {
            int[] bits =
                    new int[
                            bitsPerSymbol
                            ];

            for (int bit = 0;
                 bit < bitsPerSymbol;
                 bit++) {
                bits[bit] =
                        (
                                value
                                        >>> (
                                        bitsPerSymbol - 1 - bit
                                )
                        )
                                & 1;
            }

            labels[value] =
                    bits;

            points[value] =
                    WifiConstellationMapper.map(
                            bits,
                            modulation
                    )[0];
        }

        double variance =
                Math.max(
                        1.0E-9,
                        noiseVariance
                );

        double[] result =
                new double[
                        symbols.length
                                * bitsPerSymbol
                        ];

        for (int symbolIndex = 0;
             symbolIndex < symbols.length;
             symbolIndex++) {
            Complex observed =
                    symbols[symbolIndex];

            for (int bitIndex = 0;
                 bitIndex < bitsPerSymbol;
                 bitIndex++) {
                double best0 =
                        Double.POSITIVE_INFINITY;

                double best1 =
                        Double.POSITIVE_INFINITY;

                for (int candidate = 0;
                     candidate < constellationSize;
                     candidate++) {
                    double distance =
                            observed.subtract(
                                    points[candidate]
                            ).magnitudeSquared();

                    if (labels[candidate][bitIndex] == 0) {
                        best0 =
                                Math.min(
                                        best0,
                                        distance
                                );
                    } else {
                        best1 =
                                Math.min(
                                        best1,
                                        distance
                                );
                    }
                }

                result[
                        symbolIndex
                                * bitsPerSymbol
                                + bitIndex
                        ] =
                        (
                                best0 - best1
                        )
                                / variance;
            }
        }

        return result;
    }
}
