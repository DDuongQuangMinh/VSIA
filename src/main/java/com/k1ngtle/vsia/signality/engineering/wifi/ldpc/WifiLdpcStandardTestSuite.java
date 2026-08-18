package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.Arrays;
import java.util.List;

public final class WifiLdpcStandardTestSuite {
    private WifiLdpcStandardTestSuite() {
    }

    public static List<WifiLdpcStandardTestResult> runAll() {
        return List.of(
                matrixGeometry(),
                matrixMetadataConsistency(),
                systematicEncoding(),
                cleanDecode(),
                awgnDecode()
        );
    }

    private static WifiLdpcStandardTestResult matrixGeometry() {
        WifiLdpcStandardProfile profile =
                WifiLdpcStandardProfiles
                        .n648RateThreeQuarter();

        QcLdpcBaseMatrix matrix =
                profile.matrix();

        boolean passed =
                matrix.standardized()
                        && matrix.expansionFactor() == 27
                        && matrix.baseRows() == 6
                        && matrix.baseColumns() == 24
                        && matrix.codewordBits() == 648
                        && matrix.informationBits() == 486
                        && Math.abs(
                        matrix.nominalRate()
                                - 0.75
                ) < 1.0E-12;

        return result(
                "wifi-w13b-ieee-n648-r34-geometry",
                passed,
                "Pinned WLAN QC-LDPC profile must expand to N=648, K=486, R=3/4, Z=27"
        );
    }

    private static WifiLdpcStandardTestResult matrixMetadataConsistency() {
        WifiLdpcStandardProfile profile =
                WifiLdpcStandardProfiles
                        .n648RateThreeQuarter();

        boolean passed =
                profile.metadata()
                        .codewordLength()
                        == WifiLdpcCodewordLength.N_648
                        && profile.metadata()
                        .rate()
                        == WifiLdpcTargetRate.RATE_3_4
                        && profile.metadata()
                        .expansionFactor()
                        == 27
                        && profile.matrix()
                        .standardized();

        return result(
                "wifi-w13b-ieee-profile-metadata",
                passed,
                "Standard-profile metadata must match the pinned N=648 R=3/4 matrix"
        );
    }

    private static WifiLdpcStandardTestResult systematicEncoding() {
        WifiLdpcStandardProfile profile =
                WifiLdpcStandardProfiles
                        .n648RateThreeQuarter();

        QcLdpcEncoder encoder =
                new QcLdpcEncoder(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits()
                );

        int[] codeword =
                encoder.encode(
                        information
                );

        boolean passed =
                Arrays.equals(
                        information,
                        Arrays.copyOf(
                                codeword,
                                information.length
                        )
                )
                        && encoder.syndromeWeight(
                        codeword
                ) == 0;

        return result(
                "wifi-w13b-ieee-n648-r34-encode",
                passed,
                "Pinned WLAN matrix must produce a systematic codeword with zero syndrome"
        );
    }

    private static WifiLdpcStandardTestResult cleanDecode() {
        WifiLdpcStandardProfile profile =
                WifiLdpcStandardProfiles
                        .n648RateThreeQuarter();

        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double[] llrs =
                new double[
                        codeword.length
                        ];

        for (int i = 0;
             i < codeword.length;
             i++) {
            llrs[i] =
                    codeword[i] == 1
                            ? 12.0
                            : -12.0;
        }

        LdpcDecodeResult decoded =
                codec.decode(
                        llrs,
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
                "wifi-w13b-ieee-n648-r34-clean-decode",
                passed,
                "Pinned WLAN matrix must round-trip through the existing soft min-sum decoder"
        );
    }

    private static WifiLdpcStandardTestResult awgnDecode() {
        WifiLdpcStandardProfile profile =
                WifiLdpcStandardProfiles
                        .n648RateThreeQuarter();

        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double[] llrs =
                LdpcAwgn.bpskLlrs(
                        codeword,
                        8.0,
                        0x64834L
                );

        LdpcDecodeResult decoded =
                codec.decode(
                        llrs,
                        50
                );

        boolean passed =
                decoded.converged()
                        && decoded.syndromeWeight() == 0
                        && Arrays.equals(
                        information,
                        decoded.informationBits()
                );

        return result(
                "wifi-w13b-ieee-n648-r34-awgn",
                passed,
                "Pinned WLAN N=648 R=3/4 profile must recover the deterministic 8 dB AWGN vector"
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
                0x2468ACE1;

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

    private static WifiLdpcStandardTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiLdpcStandardTestResult(
                id,
                passed,
                detail
        );
    }
}
