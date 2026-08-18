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

        for (WifiLdpcStandardProfile profile
                : WifiLdpcStandardProfiles.n648Family()) {
            results.add(
                    matrixGeometry(
                            profile
                    )
            );
            results.add(
                    systematicEncoding(
                            profile
                    )
            );
            results.add(
                    cleanDecode(
                            profile
                    )
            );
            results.add(
                    awgnDecode(
                            profile
                    )
            );
        }

        results.add(
                familyCompleteness()
        );

        return List.copyOf(
                results
        );
    }

    private static WifiLdpcStandardTestResult matrixGeometry(
            WifiLdpcStandardProfile profile
    ) {
        QcLdpcBaseMatrix matrix =
                profile.matrix();

        int expectedK =
                648
                        * profile.metadata()
                        .rate()
                        .numerator()
                        / profile.metadata()
                        .rate()
                        .denominator();

        boolean passed =
                matrix.standardized()
                        && matrix.expansionFactor() == 27
                        && matrix.baseColumns() == 24
                        && matrix.codewordBits() == 648
                        && matrix.informationBits() == expectedK
                        && profile.metadata()
                        .codewordLength()
                        == WifiLdpcCodewordLength.N_648;

        return result(
                id(
                        profile,
                        "geometry"
                ),
                passed,
                "Pinned N=648 profile geometry and metadata must match its target rate"
        );
    }

    private static WifiLdpcStandardTestResult systematicEncoding(
            WifiLdpcStandardProfile profile
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
                        "encode"
                ),
                passed,
                "Systematic encoding must preserve information bits and produce zero syndrome"
        );
    }

    private static WifiLdpcStandardTestResult cleanDecode(
            WifiLdpcStandardProfile profile
    ) {
        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits(),
                        10
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
                        40
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
                        "clean-decode"
                ),
                passed,
                "Pinned N=648 profile must round-trip through the existing min-sum decoder"
        );
    }

    private static WifiLdpcStandardTestResult awgnDecode(
            WifiLdpcStandardProfile profile
    ) {
        WifiLdpcLabCodec codec =
                new WifiLdpcLabCodec(
                        profile.matrix()
                );

        int[] information =
                deterministicBits(
                        profile.matrix()
                                .informationBits(),
                        20
                                + profile.metadata()
                                .rate()
                                .ordinal()
                );

        int[] codeword =
                codec.encode(
                        information
                );

        double snrDb =
                switch (profile.metadata().rate()) {
                    case RATE_1_2 -> 5.5;
                    case RATE_2_3 -> 10.0;
                    case RATE_3_4 -> 8.0;
                    case RATE_5_6 -> 9.5;
                };

        double[] llrs =
                LdpcAwgn.bpskLlrs(
                        codeword,
                        snrDb,
                        0x648000L
                                + profile.metadata()
                                .rate()
                                .ordinal()
                );

        LdpcDecodeResult decoded =
                codec.decode(
                        llrs,
                        60
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
                        "awgn"
                ),
                passed,
                "Pinned N=648 profile must recover its deterministic BPSK/AWGN vector"
        );
    }

    private static WifiLdpcStandardTestResult familyCompleteness() {
        List<WifiLdpcStandardProfile> profiles =
                WifiLdpcStandardProfiles
                        .n648Family();

        boolean passed =
                profiles.size() == 4
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
                "wifi-w13c-ieee-n648-family-complete",
                passed,
                "N=648 standard profile registry must contain 1/2, 2/3, 3/4 and 5/6 exactly once"
        );
    }

    private static String id(
            WifiLdpcStandardProfile profile,
            String suffix
    ) {
        String rate =
                switch (profile.metadata().rate()) {
                    case RATE_1_2 -> "r12";
                    case RATE_2_3 -> "r23";
                    case RATE_3_4 -> "r34";
                    case RATE_5_6 -> "r56";
                };

        return "wifi-w13c-ieee-n648-"
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
