package com.k1ngtle.vsia.signality.engineering.wifi.live;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcsTable;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class WifiLivePhyTestSuite {
    private WifiLivePhyTestSuite() {
    }

    public static List<WifiLivePhyTestResult> runAll() {
        return List.of(
                analyticalBypass(),
                legacyWaveformPass(),
                legacyUnsupportedFallback(),
                ldpcSingleCodewordPass(),
                ldpcMultiCodewordPass(),
                lowSnrRejects(),
                frameLimitFallback()
        );
    }

    private static WifiLivePhyTestResult analyticalBypass() {
        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload(
                                "ANALYTICAL"
                        ),
                        WifiPhyGeneration.HE,
                        WifiMcsTable.byIndex(
                                9
                        ),
                        20.0,
                        WifiLivePhyMode.ANALYTICAL
                );

        return result(
                "wifi-w14-analytical-bypass",
                !decision.evaluated()
                        && decision.delivered()
                        && decision.path()
                        == WifiLivePhyPath.BYPASS,
                "Default analytical mode must not invoke the detailed bit-level PHY"
        );
    }

    private static WifiLivePhyTestResult legacyWaveformPass() {
        byte[] payload =
                payload(
                        "VSIA-W1.4-LEGACY-LIVE"
                );

        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload,
                        WifiPhyGeneration.LEGACY_OFDM,
                        WifiMcsTable.byIndex(
                                6
                        ),
                        35.0,
                        WifiLivePhyMode.BIT_LEVEL_AUTO
                );

        return result(
                "wifi-w14-legacy-waveform-pass",
                decision.evaluated()
                        && decision.delivered()
                        && decision.path()
                        == WifiLivePhyPath.LEGACY_OFDM_WAVEFORM,
                "Legacy MCS 6 must pass through the complete W1.2 waveform encoder/receiver"
        );
    }

    private static WifiLivePhyTestResult legacyUnsupportedFallback() {
        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload(
                                "LEGACY-MCS7"
                        ),
                        WifiPhyGeneration.LEGACY_OFDM,
                        WifiMcsTable.byIndex(
                                7
                        ),
                        40.0,
                        WifiLivePhyMode.BIT_LEVEL_AUTO
                );

        return result(
                "wifi-w14-legacy-unsupported-fallback",
                !decision.evaluated()
                        && decision.delivered()
                        && decision.path()
                        == WifiLivePhyPath.UNSUPPORTED_FALLBACK,
                "Legacy MCS 7 has no legacy 802.11a/g rate equivalent and must fall back instead of inventing one"
        );
    }

    private static WifiLivePhyTestResult ldpcSingleCodewordPass() {
        byte[] payload =
                payload(
                        "VSIA-W1.4-HE-LDPC"
                );

        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload,
                        WifiPhyGeneration.HE,
                        WifiMcsTable.byIndex(
                                11
                        ),
                        30.0,
                        WifiLivePhyMode.BIT_LEVEL_AUTO
                );

        return result(
                "wifi-w14-ldpc-single-codeword",
                decision.evaluated()
                        && decision.delivered()
                        && decision.codewords() == 1
                        && decision.path()
                        == WifiLivePhyPath.STANDARD_LDPC_FEC,
                "Modern detailed mode must encode, rate-match and soft-decode a pinned LDPC codeword"
        );
    }

    private static WifiLivePhyTestResult ldpcMultiCodewordPass() {
        byte[] payload =
                new byte[
                        600
                ];

        for (int i = 0;
             i < payload.length;
             i++) {
            payload[i] =
                    (byte) (
                            i * 31
                                    + 7
                    );
        }

        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload,
                        WifiPhyGeneration.EHT,
                        WifiMcsTable.byIndex(
                                12
                        ),
                        40.0,
                        WifiLivePhyMode.BIT_LEVEL_AUTO
                );

        return result(
                "wifi-w14-ldpc-multi-codeword",
                decision.evaluated()
                        && decision.delivered()
                        && decision.codewords() >= 3,
                "A large EHT MAC frame must be segmented across multiple pinned LDPC codewords"
        );
    }

    private static WifiLivePhyTestResult lowSnrRejects() {
        byte[] payload =
                new byte[
                        256
                ];

        Arrays.fill(
                payload,
                (byte) 0x5A
        );

        WifiLivePhyDecision decision =
                WifiLivePhyEngine.evaluate(
                        payload,
                        WifiPhyGeneration.HE,
                        WifiMcsTable.byIndex(
                                11
                        ),
                        -15.0,
                        WifiLivePhyMode.BIT_LEVEL_AUTO
                );

        return result(
                "wifi-w14-low-snr-reject",
                decision.evaluated()
                        && !decision.delivered(),
                "Detailed LDPC gate must be capable of rejecting a deterministic severely degraded frame"
        );
    }

    private static WifiLivePhyTestResult frameLimitFallback() {
        int original =
                WifiLivePhySettings.MAX_FRAME_BYTES;

        try {
            WifiLivePhySettings.MAX_FRAME_BYTES =
                    32;

            WifiLivePhyDecision decision =
                    WifiLivePhyEngine.evaluate(
                            new byte[
                                    64
                            ],
                            WifiPhyGeneration.HE,
                            WifiMcsTable.byIndex(
                                    5
                            ),
                            30.0,
                            WifiLivePhyMode.BIT_LEVEL_AUTO
                    );

            return result(
                    "wifi-w14-frame-limit-fallback",
                    !decision.evaluated()
                            && decision.delivered()
                            && decision.path()
                            == WifiLivePhyPath.UNSUPPORTED_FALLBACK,
                    "Frames above the configured detailed-PHY budget must remain on the analytical path"
            );
        } finally {
            WifiLivePhySettings.MAX_FRAME_BYTES =
                    original;
        }
    }

    private static byte[] payload(
            String value
    ) {
        return value.getBytes(
                StandardCharsets.UTF_8
        );
    }

    private static WifiLivePhyTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiLivePhyTestResult(
                id,
                passed,
                detail
        );
    }
}
