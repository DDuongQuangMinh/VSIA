package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyPilotPhaseCorrector {
    private static final int FFT_SIZE =
            64;

    private static final int[] PILOTS =
            new int[] {
                    -21,
                    -7,
                    7,
                    21
            };

    private static final double[] BASE =
            new double[] {
                    1.0,
                    1.0,
                    1.0,
                    -1.0
            };

    private LegacyPilotPhaseCorrector() {
    }

    public static Complex[] correct(
            Complex[] bins,
            int symbolIndex
    ) {
        if (bins.length != FFT_SIZE) {
            throw new IllegalArgumentException(
                    "Expected 64 FFT bins"
            );
        }

        int polarity =
                LegacyPilotPolarity.forSymbol(
                        symbolIndex
                );

        Complex phasorSum =
                Complex.ZERO;

        for (int i = 0;
             i < PILOTS.length;
             i++) {
            Complex observed =
                    bins[bin(
                            PILOTS[i]
                    )];

            double expected =
                    BASE[i]
                            * polarity;

            phasorSum =
                    phasorSum.add(
                            observed.scale(
                                    expected
                            )
                    );
        }

        double phase =
                WifiComplexMath.phase(
                        phasorSum
                );

        Complex correction =
                new Complex(
                        Math.cos(
                                -phase
                        ),
                        Math.sin(
                                -phase
                        )
                );

        Complex[] result =
                new Complex[
                        bins.length
                        ];

        for (int i = 0;
             i < bins.length;
             i++) {
            result[i] =
                    bins[i]
                            .multiply(
                                    correction
                            );
        }

        return result;
    }

    private static int bin(
            int signed
    ) {
        return (
                signed
                        + FFT_SIZE
        )
                % FFT_SIZE;
    }
}
