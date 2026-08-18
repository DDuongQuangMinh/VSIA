package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.Arrays;
import java.util.List;

public final class WifiLdpcTestSuite {
    private WifiLdpcTestSuite() {
    }

    public static List<WifiLdpcTestResult> runAll() {
        return List.of(
                matrixGeometry(),
                systematicEncoding(),
                singleErrorSyndrome(),
                cleanDecode(),
                noisyDecode(),
                rateMatchRestore(),
                familyPlanner(),
                targetRateMapping()
        );
    }

    private static WifiLdpcTestResult matrixGeometry() {
        QcLdpcBaseMatrix profile =
                WifiLdpcLabProfiles
                        .referenceRateHalfZ27();

        boolean passed =
                profile.expansionFactor() == 27
                        && profile.baseRows() == 4
                        && profile.baseColumns() == 8
                        && profile.codewordBits() == 216
                        && profile.informationBits() == 108
                        && Math.abs(
                        profile.nominalRate() - 0.5
                ) < 1.0E-12
                        && !profile.standardized();

        return result(
                "wifi-w13-qc-matrix-geometry",
                passed,
                "Reference profile must be 4x8 QC, Z=27, N=216, K=108, and explicitly non-standardized"
        );
    }

    private static WifiLdpcTestResult systematicEncoding() {
        WifiLdpcLabCodec codec =
                codec();

        int[] information =
                deterministicBits(
                        codec.profile()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        int[] prefix =
                Arrays.copyOf(
                        codeword,
                        information.length
                );

        boolean passed =
                Arrays.equals(
                        information,
                        prefix
                )
                        && codec.syndromeWeight(
                        codeword
                ) == 0;

        return result(
                "wifi-w13-systematic-encode-zero-syndrome",
                passed,
                "Encoded QC-LDPC codeword must preserve systematic bits and satisfy every parity check"
        );
    }

    private static WifiLdpcTestResult singleErrorSyndrome() {
        WifiLdpcLabCodec codec =
                codec();

        int[] information =
                deterministicBits(
                        codec.profile()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        codeword[17] ^=
                1;

        return result(
                "wifi-w13-single-error-nonzero-syndrome",
                codec.syndromeWeight(
                        codeword
                ) > 0,
                "A one-bit codeword corruption must produce a non-zero LDPC syndrome"
        );
    }

    private static WifiLdpcTestResult cleanDecode() {
        WifiLdpcLabCodec codec =
                codec();

        int[] information =
                deterministicBits(
                        codec.profile()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double[] llr =
                new double[
                        codeword.length
                        ];

        for (int i = 0;
             i < codeword.length;
             i++) {
            llr[i] =
                    codeword[i] == 1
                            ? 12.0
                            : -12.0;
        }

        LdpcDecodeResult decoded =
                codec.decode(
                        llr,
                        20
                );

        boolean passed =
                decoded.converged()
                        && decoded.syndromeWeight() == 0
                        && Arrays.equals(
                        information,
                        decoded.informationBits()
                );

        return result(
                "wifi-w13-clean-minsum-roundtrip",
                passed,
                "Layered normalized min-sum must recover a noiseless systematic codeword"
        );
    }

    private static WifiLdpcTestResult noisyDecode() {
        WifiLdpcLabCodec codec =
                codec();

        int[] information =
                deterministicBits(
                        codec.profile()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double[] llr =
                LdpcAwgn.bpskLlrs(
                        codeword,
                        6.5,
                        0x51A13L
                );

        LdpcDecodeResult decoded =
                codec.decode(
                        llr,
                        30
                );

        boolean passed =
                decoded.converged()
                        && decoded.syndromeWeight() == 0
                        && Arrays.equals(
                        information,
                        decoded.informationBits()
                );

        return result(
                "wifi-w13-awgn-minsum-roundtrip",
                passed,
                "Reference QC-LDPC code must recover the deterministic 6.5 dB BPSK/AWGN vector"
        );
    }

    private static WifiLdpcTestResult rateMatchRestore() {
        int[] mother =
                new int[
                        216
                        ];

        for (int i = 0;
             i < mother.length;
             i++) {
            mother[i] =
                    i & 1;
        }

        LdpcRateMatchPlan plan =
                new LdpcRateMatchPlan(
                        216,
                        108,
                        8,
                        12,
                        196
                );

        int[] transmitted =
                LdpcRateMatcher.transmitBits(
                        mother,
                        plan
                );

        double[] channel =
                new double[
                        transmitted.length
                        ];

        for (int i = 0;
             i < channel.length;
             i++) {
            channel[i] =
                    transmitted[i] == 1
                            ? 9.0
                            : -9.0;
        }

        double[] restored =
                LdpcRateMatcher.restoreLlrs(
                        channel,
                        plan
                );

        boolean passed =
                transmitted.length == 196
                        && restored.length == 216
                        && restored[0] < -40.0
                        && restored[7] < -40.0
                        && restored[215] == 0.0
                        && restored[204] == 0.0;

        return result(
                "wifi-w13-rate-match-shortening-puncture",
                passed,
                "Shortened bits must restore as known zero LLRs while punctured tail bits restore as erasures"
        );
    }

    private static WifiLdpcTestResult familyPlanner() {
        WifiLdpcCodewordPlan small =
                WifiLdpcEngineeringPlanner.plan(
                        250,
                        WifiLdpcTargetRate.RATE_1_2
                );

        WifiLdpcCodewordPlan medium =
                WifiLdpcEngineeringPlanner.plan(
                        700,
                        WifiLdpcTargetRate.RATE_3_4
                );

        WifiLdpcCodewordPlan large =
                WifiLdpcEngineeringPlanner.plan(
                        4000,
                        WifiLdpcTargetRate.RATE_5_6
                );

        boolean passed =
                small.codewordLength()
                        == WifiLdpcCodewordLength.N_648
                        && medium.codewordLength()
                        == WifiLdpcCodewordLength.N_1296
                        && large.codewordLength()
                        == WifiLdpcCodewordLength.N_1944
                        && large.codewordCount() > 1;

        return result(
                "wifi-w13-family-codeword-planner",
                passed,
                "Engineering planner should escalate through 648/1296/1944-bit target lengths as payload grows"
        );
    }

    private static WifiLdpcTestResult targetRateMapping() {
        boolean passed =
                WifiLdpcTargetRate.nearest(
                        0.50
                ) == WifiLdpcTargetRate.RATE_1_2
                        && WifiLdpcTargetRate.nearest(
                        2.0 / 3.0
                ) == WifiLdpcTargetRate.RATE_2_3
                        && WifiLdpcTargetRate.nearest(
                        0.75
                ) == WifiLdpcTargetRate.RATE_3_4
                        && WifiLdpcTargetRate.nearest(
                        5.0 / 6.0
                ) == WifiLdpcTargetRate.RATE_5_6;

        return result(
                "wifi-w13-target-rate-mapping",
                passed,
                "Modern Wi-Fi MCS coding rates must map to the 1/2, 2/3, 3/4 and 5/6 LDPC target families"
        );
    }

    private static WifiLdpcLabCodec codec() {
        return new WifiLdpcLabCodec(
                WifiLdpcLabProfiles
                        .referenceRateHalfZ27()
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
                0x13579BDF;

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

    private static WifiLdpcTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiLdpcTestResult(
                id,
                passed,
                detail
        );
    }
}
