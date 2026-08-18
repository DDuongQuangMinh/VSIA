package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyOfdmPpduEncoder {
    public static final int SERVICE_BITS =
            16;

    public static final int TAIL_BITS =
            6;

    private LegacyOfdmPpduEncoder() {
    }

    public static LegacyOfdmPpdu encode(
            byte[] psdu,
            LegacyOfdmRateProfile rate,
            int scramblerSeed
    ) {
        byte[] payload =
                psdu == null
                        ? new byte[0]
                        : psdu.clone();

        int payloadBits =
                payload.length
                        * 8;

        int nDbps =
                rate.dataBitsPerSymbol();

        int uncodedWithoutPad =
                SERVICE_BITS
                        + payloadBits
                        + TAIL_BITS;

        int symbols =
                Math.max(
                        1,
                        (uncodedWithoutPad
                                + nDbps
                                - 1)
                                / nDbps
                );

        int totalDataBits =
                symbols
                        * nDbps;

        int padBits =
                totalDataBits
                        - uncodedWithoutPad;

        int[] data =
                new int[
                        totalDataBits
                        ];

        int[] payloadBitArray =
                WifiBitOrder
                        .bytesToLsbFirstBits(
                                payload
                        );

        System.arraycopy(
                payloadBitArray,
                0,
                data,
                SERVICE_BITS,
                payloadBitArray.length
        );

        int[] scrambled =
                WifiScrambler.apply(
                        data,
                        scramblerSeed
                );

        int tailStart =
                SERVICE_BITS
                        + payloadBits;

        for (int i = 0;
             i < TAIL_BITS;
             i++) {
            scrambled[tailStart + i] =
                    0;
        }

        int[] punctured =
                WifiBccEncoder.encode(
                        scrambled,
                        rate.codeRate()
                );

        int nCbps =
                rate.codedBitsPerSymbol();

        if (punctured.length
                != symbols * nCbps) {
            throw new IllegalStateException(
                    "BCC/puncturing length does not match OFDM symbol geometry"
            );
        }

        int[] interleaved =
                new int[
                        punctured.length
                        ];

        Complex[][] frequencySymbols =
                new Complex[
                        symbols
                        ][];

        for (int symbol = 0;
             symbol < symbols;
             symbol++) {
            int offset =
                    symbol
                            * nCbps;

            int[] codedSymbol =
                    new int[
                            nCbps
                            ];

            System.arraycopy(
                    punctured,
                    offset,
                    codedSymbol,
                    0,
                    nCbps
            );

            int[] interleavedSymbol =
                    LegacyOfdmInterleaver
                            .interleave(
                                    codedSymbol,
                                    rate.bitsPerSubcarrier()
                            );

            System.arraycopy(
                    interleavedSymbol,
                    0,
                    interleaved,
                    offset,
                    nCbps
            );

            Complex[] constellation =
                    WifiConstellationMapper.map(
                            interleavedSymbol,
                            rate.modulation()
                    );

            frequencySymbols[symbol] =
                    LegacyOfdmSubcarrierMapper.map(
                            constellation
                    );
        }

        return new LegacyOfdmPpdu(
                rate,
                scramblerSeed,
                payload.length,
                symbols,
                padBits,
                scrambled,
                punctured,
                interleaved,
                frequencySymbols
        );
    }
}
