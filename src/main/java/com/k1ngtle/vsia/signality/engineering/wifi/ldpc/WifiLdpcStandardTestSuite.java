package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WifiLdpcStandardTestSuite {
    private WifiLdpcStandardTestSuite() {
    }

    public static List<WifiLdpcStandardTestResult> runAll() {
        List<WifiLdpcStandardTestResult> results =
                new ArrayList<>();

        runFamily(
                results,
                WifiLdpcStandardProfiles.n648Family(),
                WifiLdpcCodewordLength.N_648,
                27,
                "w13c"
        );

        runFamily(
                results,
                WifiLdpcStandardProfiles.n1296Family(),
                WifiLdpcCodewordLength.N_1296,
                54,
                "w13d"
        );

        return List.copyOf(
                results
        );
    }

    private static void runFamily(
            List<WifiLdpcStandardTestResult> results,
            List<WifiLdpcStandardProfile> profiles,
            WifiLdpcCodewordLength expectedLength,
            int expectedZ,
            String phase
    ) {
        for (WifiLdpcStandardProfile profile
                : profiles) {
            results.add(
                    matrixGeometry(
                            profile,
                            expectedLength,
                            expectedZ,
                            phase
                    )
            );

            results.add(
                    systematicEncoding(
                            profile,
                            phase
                    )
            );

            results.add(
                    cleanDecode(
                            profile,
                            phase
                    )
            );

            results.add(
                    awgnDecode(
                            profile,
                            phase
                    )
            );
        }

        results.add(
                familyCompleteness(
                        profiles,
                        expectedLength,
                        phase
                )
        );
    }

    private static WifiLdpcStandardTestResult matrixGeometry(
            WifiLdpcStandardProfile profile,
            WifiLdpcCodewordLength expectedLength,
            int expectedZ,
            String phase
    ) {
        QcLdpcBaseMatrix matrix =
                profile.matrix();

        int expectedN =
                expectedLength.bits();

        int expectedK =
                expectedN
                        * profile.metadata()
                        .rate()
                        .numerator()
                        / profile.metadata()
                        .rate()
                        .denominator();

        boolean passed =
                matrix.standardized()
                        && matrix.expansionFactor() == expectedZ
                        && matrix.baseColumns() == 24
                        && matrix.codewordBits() == expectedN
                        && matrix.informationBits() == expectedK
                        && profile.metadata()
                        .codewordLength()
                        == expectedLength;

        return result(
                id(
                        profile,
                        phase,
                        "geometry"
                ),
                passed,
                "Pinned WLAN profile geometry and metadata must match codeword length, Z and target rate"
        );
    }

    private static WifiLdpcStandardTestResult systematicEncoding(
            WifiLdpcStandardProfile profile,
            String phase
    ) {
        QcLdpcEncoder encoder =
                new QcLdpcEncoder(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits(),
                        profile.metadata()
                                .codewordLength()
                                .ordinal()
                                * 10
                                + profile.metadata()
                                .rate()
                                .ordinal()
                                + 1
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
                id(
                        profile,
                        phase,
                        "encode"
                ),
                passed,
                "Systematic encoding must preserve information bits and produce zero syndrome"
        );
    }

    private static WifiLdpcStandardTestResult cleanDecode(
            WifiLdpcStandardProfile profile,
            String phase
    ) {
        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits(),
                        100
                                + profile.metadata()
                                .codewordLength()
                                .ordinal()
                                * 10
                                + profile.metadata()
                                .rate()
                                .ordinal()
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
                id(
                        profile,
                        phase,
                        "clean-decode"
                ),
                passed,
                "Pinned WLAN profile must round-trip through the existing min-sum decoder"
        );
    }

    private static WifiLdpcStandardTestResult awgnDecode(
            WifiLdpcStandardProfile profile,
            String phase
    ) {
        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits(),
                        200
                                + profile.metadata()
                                .codewordLength()
                                .ordinal()
                                * 10
                                + profile.metadata()
                                .rate()
                                .ordinal()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double snrDb =
                regressionSnrDb(
                        profile
                );

        double[] llrs =
                LdpcAwgn.bpskLlrs(
                        codeword,
                        snrDb,
                        0x1296000L
                                + profile.metadata()
                                .codewordLength()
                                .ordinal()
                                * 16L
                                + profile.metadata()
                                .rate()
                                .ordinal()
                );

        LdpcDecodeResult decoded =
                codec.decode(
                        llrs,
                        70
                );

        boolean passed =
                decoded.converged()
                        && decoded.syndromeWeight() == 0
                        && Arrays.equals(
                        information,
                        decoded.informationBits()
                );

        return result(
                id(
                        profile,
                        phase,
                        "awgn"
                ),
                passed,
                "Pinned WLAN profile must recover its deterministic BPSK/AWGN regression vector"
        );
    }

    private static double regressionSnrDb(
            WifiLdpcStandardProfile profile
    ) {
        return switch (profile.metadata().rate()) {
            case RATE_1_2 -> 12.0;
            case RATE_2_3 -> 12.0;
            case RATE_3_4 -> 12.0;
            case RATE_5_6 -> 12.0;
        };
    }

    private static WifiLdpcStandardTestResult familyCompleteness(
            List<WifiLdpcStandardProfile> profiles,
            WifiLdpcCodewordLength length,
            String phase
    ) {
        boolean passed =
                profiles.size() == 4
                        && profiles.stream()
                        .allMatch(
                                value ->
                                        value.metadata()
                                                .codewordLength()
                                                == length
                        )
                        && profiles.stream()
                        .map(
                                value ->
                                        value.metadata()
                                                .rate()
                        )
                        .distinct()
                        .count()
                        == 4;

        return result(
                "wifi-"
                        + phase
                        + "-ieee-n"
                        + length.bits()
                        + "-family-complete",
                passed,
                "Standard profile registry must contain 1/2, 2/3, 3/4 and 5/6 exactly once for this codeword length"
        );
    }

    private static String id(
            WifiLdpcStandardProfile profile,
            String phase,
            String suffix
    ) {
        String rate =
                switch (profile.metadata().rate()) {
                    case RATE_1_2 -> "r12";
                    case RATE_2_3 -> "r23";
                    case RATE_3_4 -> "r34";
                    case RATE_5_6 -> "r56";
                };

        return "wifi-"
                + phase
                + "-ieee-n"
                + profile.metadata()
                .codewordLength()
                .bits()
                + "-"
                + rate
                + "-"
                + suffix;
    }

    private static int[] deterministicBits(
            int count,
            int salt
    ) {
        int[] bits =
                new int[
                        count
                        ];

        int state =
                0x2468ACE1
                        ^ (
                        salt * 0x45D9F3B
                );

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
