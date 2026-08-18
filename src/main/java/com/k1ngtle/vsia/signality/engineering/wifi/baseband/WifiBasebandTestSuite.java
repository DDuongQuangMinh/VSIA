package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

import com.k1ngtle.vsia.signality.engineering.math.Complex;
import com.k1ngtle.vsia.signality.engineering.phy.Modulation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class WifiBasebandTestSuite {
    private WifiBasebandTestSuite() {
    }

    public static List<WifiBasebandTestResult> runAll() {
        return List.of(
                bitOrderRoundTrip(),
                scramblerRoundTrip(),
                bccImpulseVector(),
                bccRateHalfRoundTrip(),
                bccRateThreeQuarterRoundTrip(),
                interleaverRoundTrip(),
                qam64RoundTrip(),
                legacySubcarrierGeometry(),
                legacy54MbpsPpduGeometry(),
                legacy54MbpsPpduRoundTrip()
        );
    }

    private static WifiBasebandTestResult bitOrderRoundTrip() {
        byte[] input =
                new byte[] {
                        (byte) 0xA5,
                        0x5A,
                        0x01
                };

        byte[] output =
                WifiBitOrder.lsbFirstBitsToBytes(
                        WifiBitOrder.bytesToLsbFirstBits(
                                input
                        )
                );

        return result(
                "wifi-w1-bit-order-roundtrip",
                Arrays.equals(
                        input,
                        output
                ),
                "LSB-first PSDU byte/bit conversion must round-trip"
        );
    }

    private static WifiBasebandTestResult scramblerRoundTrip() {
        int[] input =
                new int[
                        257
                        ];

        for (int i = 0;
             i < input.length;
             i++) {
            input[i] =
                    (
                            i * 17
                                    + 3
                    )
                            & 1;
        }

        int[] scrambled =
                WifiScrambler.apply(
                        input,
                        0x5D
                );

        int[] descrambled =
                WifiScrambler.apply(
                        scrambled,
                        0x5D
                );

        return result(
                "wifi-w1-scrambler-roundtrip",
                Arrays.equals(
                        input,
                        descrambled
                ),
                "x^7+x^4+1 additive scrambler must be self-inverse for the same non-zero seed"
        );
    }

    private static WifiBasebandTestResult bccImpulseVector() {
        int[] input =
                new int[] {
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                };

        int[] expected =
                new int[] {
                        1, 1,
                        1, 0,
                        0, 0,
                        1, 1,
                        1, 1,
                        0, 1,
                        1, 1
                };

        int[] actual =
                WifiBccEncoder
                        .encodeMotherRateHalf(
                                input
                        );

        return result(
                "wifi-w1-bcc-133-171-impulse",
                Arrays.equals(
                        expected,
                        actual
                ),
                "K=7 mother code with octal generators 133/171 must match the declared shift-register vector"
        );
    }

    private static WifiBasebandTestResult bccRateHalfRoundTrip() {
        int[] input =
                deterministicBits(
                        180
                );

        int[] coded =
                WifiBccEncoder.encode(
                        input,
                        BccCodeRate.RATE_1_2
                );

        int[] decoded =
                WifiViterbiDecoder.decode(
                        coded,
                        input.length,
                        BccCodeRate.RATE_1_2
                );

        return result(
                "wifi-w1-bcc-rate-half-roundtrip",
                Arrays.equals(
                        input,
                        decoded
                ),
                "Hard-decision Viterbi must invert the noiseless rate-1/2 BCC stream"
        );
    }

    private static WifiBasebandTestResult bccRateThreeQuarterRoundTrip() {
        int[] input =
                deterministicBits(
                        216
                );

        int[] coded =
                WifiBccEncoder.encode(
                        input,
                        BccCodeRate.RATE_3_4
                );

        int[] decoded =
                WifiViterbiDecoder.decode(
                        coded,
                        input.length,
                        BccCodeRate.RATE_3_4
                );

        return result(
                "wifi-w1-bcc-rate-three-quarter-roundtrip",
                Arrays.equals(
                        input,
                        decoded
                ),
                "Puncture/depuncture plus Viterbi must invert a noiseless rate-3/4 stream"
        );
    }

    private static WifiBasebandTestResult interleaverRoundTrip() {
        int[] input =
                deterministicBits(
                        192
                );

        int[] interleaved =
                LegacyOfdmInterleaver.interleave(
                        input,
                        4
                );

        int[] output =
                LegacyOfdmInterleaver.deinterleave(
                        interleaved,
                        4
                );

        return result(
                "wifi-w1-legacy-interleaver-roundtrip",
                Arrays.equals(
                        input,
                        output
                ),
                "Legacy two-permutation interleaver must round-trip for 16-QAM N_CBPS=192"
        );
    }

    private static WifiBasebandTestResult qam64RoundTrip() {
        int[] bits =
                deterministicBits(
                        6 * 64
                );

        Complex[] symbols =
                WifiConstellationMapper.map(
                        bits,
                        Modulation.QAM64
                );

        int[] output =
                WifiConstellationMapper.demapHard(
                        symbols,
                        Modulation.QAM64
                );

        return result(
                "wifi-w1-qam64-roundtrip",
                Arrays.equals(
                        bits,
                        output
                ),
                "Normalized Gray-coded 64-QAM hard demapper must recover noiseless mapped bits"
        );
    }

    private static WifiBasebandTestResult legacySubcarrierGeometry() {
        int[] carriers =
                LegacyOfdmSubcarrierMapper
                        .dataSubcarrierIndices();

        boolean count =
                carriers.length == 48;

        boolean dcAbsent =
                Arrays.stream(
                                carriers
                        )
                        .noneMatch(
                                value ->
                                        value == 0
                        );

        boolean pilotsAbsent =
                Arrays.stream(
                                carriers
                        )
                        .noneMatch(
                                value ->
                                        value == -21
                                                || value == -7
                                                || value == 7
                                                || value == 21
                        );

        return result(
                "wifi-w1-legacy-ofdm-subcarriers",
                count
                        && dcAbsent
                        && pilotsAbsent,
                "Legacy 64-point OFDM must expose 48 data carriers, excluding DC and four pilot carriers"
        );
    }

    private static WifiBasebandTestResult legacy54MbpsPpduGeometry() {
        byte[] payload =
                new byte[
                        100
                        ];

        LegacyOfdmPpdu ppdu =
                LegacyOfdmPpduEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        0x5D
                );

        boolean passed =
                ppdu.ofdmSymbols() == 4
                        && ppdu.padBits() == 42
                        && ppdu.puncturedCodedBits().length == 1152
                        && ppdu.frequencyDomainSymbols().length == 4;

        return result(
                "wifi-w1-legacy-54mbps-ppdu-geometry",
                passed,
                "100-byte PSDU at 54 Mbit/s must occupy four OFDM symbols with 42 pad bits and 1152 transmitted coded bits"
        );
    }

    private static WifiBasebandTestResult legacy54MbpsPpduRoundTrip() {
        byte[] payload =
                "VSIA-W1-BIT-LEVEL-PPDU"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        LegacyOfdmPpdu ppdu =
                LegacyOfdmPpduEncoder.encode(
                        payload,
                        LegacyOfdmRateProfile.ofMbps(
                                54
                        ),
                        0x5D
                );

        byte[] decoded =
                LegacyOfdmPpduDecoder.decodeNoiseless(
                        ppdu
                );

        return result(
                "wifi-w1-legacy-54mbps-ppdu-roundtrip",
                Arrays.equals(
                        payload,
                        decoded
                ),
                "PSDU -> scramble -> BCC -> puncture -> interleave -> QAM -> subcarriers -> inverse chain must round-trip"
        );
    }

    private static int[] deterministicBits(
            int count
    ) {
        int[] bits =
                new int[
                        count
                        ];

        int state =
                0x6D2B79F5;

        for (int i = 0;
             i < count;
             i++) {
            state ^=
                    state << 13;

            state ^=
                    state >>> 17;

            state ^=
                    state << 5;

            bits[i] =
                    state & 1;
        }

        return bits;
    }

    private static WifiBasebandTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiBasebandTestResult(
                id,
                passed,
                detail
        );
    }
}
