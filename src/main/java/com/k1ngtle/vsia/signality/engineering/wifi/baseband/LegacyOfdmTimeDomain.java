package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;

public final class LegacyOfdmTimeDomain {
    public static final int FFT_SIZE =
            64;

    public static final int CP_SAMPLES =
            16;

    public static final int SYMBOL_SAMPLES =
            FFT_SIZE
                    + CP_SAMPLES;

    private LegacyOfdmTimeDomain() {
    }

    public static Complex[] withCyclicPrefix(
            Complex[] frequencyBins
    ) {
        if (frequencyBins.length != FFT_SIZE) {
            throw new IllegalArgumentException(
                    "Expected 64 frequency bins"
            );
        }

        Complex[] time =
                Fft.ifft(
                        frequencyBins
                );

        Complex[] output =
                new Complex[
                        SYMBOL_SAMPLES
                        ];

        System.arraycopy(
                time,
                FFT_SIZE - CP_SAMPLES,
                output,
                0,
                CP_SAMPLES
        );

        System.arraycopy(
                time,
                0,
                output,
                CP_SAMPLES,
                FFT_SIZE
        );

        return output;
    }

    public static Complex[] removeCyclicPrefix(
            Complex[] timeSymbol
    ) {
        if (timeSymbol.length != SYMBOL_SAMPLES) {
            throw new IllegalArgumentException(
                    "Expected 80 samples"
            );
        }

        Complex[] output =
                new Complex[
                        FFT_SIZE
                        ];

        System.arraycopy(
                timeSymbol,
                CP_SAMPLES,
                output,
                0,
                FFT_SIZE
        );

        return output;
    }

    public static Complex[] fftAfterCp(
            Complex[] timeSymbol
    ) {
        return Fft.fft(
                removeCyclicPrefix(
                        timeSymbol
                )
        );
    }
}
