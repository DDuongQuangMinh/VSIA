package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.math.Fft;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

public final class LegacySignalCodec {
    public static final int SIGNAL_DATA_BITS =
            24;

    public static final int SIGNAL_CODED_BITS =
            48;

    public static final int CP_SAMPLES =
            16;

    public static final int SYMBOL_SAMPLES =
            64;

    private LegacySignalCodec() {
    }

    public static int[] buildBits(
            LegacySignalField signal
    ) {
        int[] bits =
                new int[
                        SIGNAL_DATA_BITS
                        ];

        int rateCode =
                rateCode(
                        signal.rate()
                                .rateMbps()
                );

        for (int i = 0;
             i < 4;
             i++) {
            bits[i] =
                    (rateCode >>> i)
                            & 1;
        }

        bits[4] =
                0;

        int length =
                signal.lengthBytes();

        for (int i = 0;
             i < 12;
             i++) {
            bits[5 + i] =
                    (length >>> i)
                            & 1;
        }

        int parity =
                0;

        for (int i = 0;
             i <= 16;
             i++) {
            parity ^=
                    bits[i];
        }

        bits[17] =
                parity & 1;

        for (int i = 18;
             i < 24;
             i++) {
            bits[i] =
                    0;
        }

        return bits;
    }

    public static Complex[] encodeFrequency(
            LegacySignalField signal
    ) {
        int[] coded =
                WifiBccEncoder.encode(
                        buildBits(
                                signal
                        ),
                        BccCodeRate.RATE_1_2
                );

        int[] interleaved =
                LegacyOfdmInterleaver.interleave(
                        coded,
                        1
                );

        Complex[] constellation =
                WifiConstellationMapper.map(
                        interleaved,
                        Modulation.BPSK
                );

        return LegacyOfdmSubcarrierMapper.map(
                constellation
        );
    }

    public static Complex[] encodeTimeDomain(
            LegacySignalField signal
    ) {
        Complex[] frequency =
                encodeFrequency(
                        signal
                );

        Complex[] symbol =
                Fft.ifft(
                        frequency
                );

        Complex[] output =
                new Complex[
                        CP_SAMPLES
                                + SYMBOL_SAMPLES
                        ];

        System.arraycopy(
                symbol,
                symbol.length - CP_SAMPLES,
                output,
                0,
                CP_SAMPLES
        );

        System.arraycopy(
                symbol,
                0,
                output,
                CP_SAMPLES,
                symbol.length
        );

        return output;
    }

    public static LegacySignalField decodeFrequency(
            Complex[] equalizedBins
    ) {
        Complex[] data =
                LegacyOfdmSubcarrierMapper.extractData(
                        equalizedBins
                );

        int[] interleaved =
                WifiConstellationMapper.demapHard(
                        data,
                        Modulation.BPSK
                );

        int[] coded =
                LegacyOfdmInterleaver.deinterleave(
                        interleaved,
                        1
                );

        int[] bits =
                WifiViterbiDecoder.decode(
                        coded,
                        SIGNAL_DATA_BITS,
                        BccCodeRate.RATE_1_2
                );

        int rateCode =
                0;

        for (int i = 0;
             i < 4;
             i++) {
            rateCode |=
                    (bits[i] & 1)
                            << i;
        }

        int length =
                0;

        for (int i = 0;
             i < 12;
             i++) {
            length |=
                    (bits[5 + i] & 1)
                            << i;
        }

        int parity =
                0;

        for (int i = 0;
             i <= 16;
             i++) {
            parity ^=
                    bits[i];
        }

        if ((parity & 1)
                != (bits[17] & 1)) {
            throw new IllegalArgumentException(
                    "L-SIG parity check failed"
            );
        }

        for (int i = 18;
             i < 24;
             i++) {
            if ((bits[i] & 1) != 0) {
                throw new IllegalArgumentException(
                        "L-SIG tail is non-zero"
                );
            }
        }

        return new LegacySignalField(
                LegacyOfdmRateProfile.ofMbps(
                        rateMbps(
                                rateCode
                        )
                ),
                length
        );
    }

    private static int rateCode(
            int mbps
    ) {
        return switch (mbps) {
            case 6 -> 0xD;
            case 9 -> 0xF;
            case 12 -> 0x5;
            case 18 -> 0x7;
            case 24 -> 0x9;
            case 36 -> 0xB;
            case 48 -> 0x1;
            case 54 -> 0x3;
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported legacy rate"
                    );
        };
    }

    private static int rateMbps(
            int code
    ) {
        return switch (code & 0xF) {
            case 0xD -> 6;
            case 0xF -> 9;
            case 0x5 -> 12;
            case 0x7 -> 18;
            case 0x9 -> 24;
            case 0xB -> 36;
            case 0x1 -> 48;
            case 0x3 -> 54;
            default ->
                    throw new IllegalArgumentException(
                            "Unsupported L-SIG RATE code: "
                                    + code
                    );
        };
    }
}
