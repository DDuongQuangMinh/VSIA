package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

import java.util.ArrayList;
import java.util.List;

public final class LegacyOfdmSubcarrierMapper {
    public static final int FFT_SIZE =
            64;

    private static final int[] PILOTS =
            new int[] {
                    -21,
                    -7,
                    7,
                    21
            };

    private static final double[] PILOT_VALUES =
            new double[] {
                    1.0,
                    1.0,
                    1.0,
                    -1.0
            };

    private static final int[] DATA_SUBCARRIERS =
            buildDataSubcarriers();

    private LegacyOfdmSubcarrierMapper() {
    }

    public static int dataSubcarrierCount() {
        return DATA_SUBCARRIERS.length;
    }

    public static Complex[] map(
            Complex[] dataSymbols
    ) {
        return map(
                dataSymbols,
                0
        );
    }

    public static Complex[] map(
            Complex[] dataSymbols,
            int symbolIndex
    ) {
        if (dataSymbols.length
                != DATA_SUBCARRIERS.length) {
            throw new IllegalArgumentException(
                    "Legacy OFDM symbol requires exactly 48 data subcarriers"
            );
        }

        Complex[] bins =
                new Complex[
                        FFT_SIZE
                        ];

        java.util.Arrays.fill(
                bins,
                Complex.ZERO
        );

        for (int i = 0;
             i < DATA_SUBCARRIERS.length;
             i++) {
            bins[bin(
                    DATA_SUBCARRIERS[i]
            )] =
                    dataSymbols[i];
        }

        int polarity =
                LegacyPilotPolarity.forSymbol(
                        symbolIndex
                );

        for (int i = 0;
             i < PILOTS.length;
             i++) {
            bins[bin(
                    PILOTS[i]
            )] =
                    new Complex(
                            PILOT_VALUES[i]
                                    * polarity,
                            0.0
                    );
        }

        return bins;
    }

    public static Complex[] extractData(
            Complex[] bins
    ) {
        if (bins.length != FFT_SIZE) {
            throw new IllegalArgumentException(
                    "Expected 64 FFT bins"
            );
        }

        Complex[] data =
                new Complex[
                        DATA_SUBCARRIERS.length
                        ];

        for (int i = 0;
             i < DATA_SUBCARRIERS.length;
             i++) {
            data[i] =
                    bins[bin(
                            DATA_SUBCARRIERS[i]
                    )];
        }

        return data;
    }

    public static int[] dataSubcarrierIndices() {
        return DATA_SUBCARRIERS.clone();
    }

    private static int[] buildDataSubcarriers() {
        List<Integer> values =
                new ArrayList<>();

        for (int k = -26;
             k <= 26;
             k++) {
            if (k == 0
                    || isPilot(
                    k
            )) {
                continue;
            }

            values.add(
                    k
            );
        }

        int[] result =
                new int[
                        values.size()
                        ];

        for (int i = 0;
             i < values.size();
             i++) {
            result[i] =
                    values.get(
                            i
                    );
        }

        return result;
    }

    private static boolean isPilot(
            int k
    ) {
        for (int pilot : PILOTS) {
            if (pilot == k) {
                return true;
            }
        }

        return false;
    }

    private static int bin(
            int signedSubcarrier
    ) {
        return (
                signedSubcarrier
                        + FFT_SIZE
        )
                % FFT_SIZE;
    }
}
