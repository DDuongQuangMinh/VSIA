package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;

public final class LegacyOfdmPpduDecoder {
    private LegacyOfdmPpduDecoder() {
    }

    public static byte[] decodeNoiseless(
            LegacyOfdmPpdu ppdu
    ) {
        LegacyOfdmRateProfile rate =
                ppdu.rate();

        Complex[][] frequencySymbols =
                ppdu.frequencyDomainSymbols();

        int nCbps =
                rate.codedBitsPerSymbol();

        int[] deinterleaved =
                new int[
                        ppdu.ofdmSymbols()
                                * nCbps
                        ];

        for (int symbol = 0;
             symbol < ppdu.ofdmSymbols();
             symbol++) {
            Complex[] constellation =
                    LegacyOfdmSubcarrierMapper
                            .extractData(
                                    frequencySymbols[symbol]
                            );

            int[] interleaved =
                    WifiConstellationMapper
                            .demapHard(
                                    constellation,
                                    rate.modulation()
                            );

            int[] coded =
                    LegacyOfdmInterleaver
                            .deinterleave(
                                    interleaved,
                                    rate.bitsPerSubcarrier()
                            );

            System.arraycopy(
                    coded,
                    0,
                    deinterleaved,
                    symbol * nCbps,
                    nCbps
            );
        }

        int inputBitCount =
                ppdu.ofdmSymbols()
                        * rate.dataBitsPerSymbol();

        int[] decodedScrambled =
                WifiViterbiDecoder.decode(
                        deinterleaved,
                        inputBitCount,
                        rate.codeRate()
                );

        int[] descrambled =
                WifiScrambler.apply(
                        decodedScrambled,
                        ppdu.scramblerSeed()
                );

        int payloadBits =
                ppdu.psduLengthBytes()
                        * 8;

        int[] psduBits =
                new int[
                        payloadBits
                        ];

        System.arraycopy(
                descrambled,
                LegacyOfdmPpduEncoder.SERVICE_BITS,
                psduBits,
                0,
                payloadBits
        );

        return WifiBitOrder
                .lsbFirstBitsToBytes(
                        psduBits
                );
    }
}
