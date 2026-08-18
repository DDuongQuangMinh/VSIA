package com.k1ngtle.vsia.signality.engineering.wifi.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class WifiHandshakeHotfixTestSuite {
    private WifiHandshakeHotfixTestSuite() {
    }

    public static List<WifiHandshakeHotfixTestResult> runAll() {
        return List.of(
                canonicalLowercase(),
                canonicalHyphenated(),
                micMaterialCaseIndependent(),
                messageLabelsDistinct(),
                invalidMacRejected()
        );
    }

    private static WifiHandshakeHotfixTestResult canonicalLowercase() {
        return result(
                "wifi-w182-canonical-lowercase",
                WifiHandshakeMicMaterial
                        .canonicalMac(
                                "aa:bb:cc:11:22:33"
                        )
                        .equals(
                                "AA:BB:CC:11:22:33"
                        ),
                "Lowercase MAC strings must canonicalize to uppercase colon notation"
        );
    }

    private static WifiHandshakeHotfixTestResult canonicalHyphenated() {
        return result(
                "wifi-w182-canonical-hyphen",
                WifiHandshakeMicMaterial
                        .canonicalMac(
                                "AA-BB-CC-11-22-33"
                        )
                        .equals(
                                "AA:BB:CC:11:22:33"
                        ),
                "Hyphenated MAC strings must canonicalize identically"
        );
    }

    private static WifiHandshakeHotfixTestResult micMaterialCaseIndependent() {
        byte[] stationSide =
                WifiHandshakeMicMaterial.micData(
                        "M2",
                        "4f:c5:8c:aa:bb:cc",
                        "b4:6c:af:11:22:33"
                );

        byte[] apSide =
                WifiHandshakeMicMaterial.micData(
                        "M2",
                        "4F:C5:8C:AA:BB:CC",
                        "B4-6C-AF-11-22-33"
                );

        return result(
                "wifi-w182-mic-material-consistency",
                Arrays.equals(
                        stationSide,
                        apSide
                ),
                "AP and station must produce byte-identical MIC input despite MAC formatting differences"
        );
    }

    private static WifiHandshakeHotfixTestResult messageLabelsDistinct() {
        String m2 =
                new String(
                        WifiHandshakeMicMaterial.micData(
                                "M2",
                                "00:11:22:33:44:55",
                                "66:77:88:99:AA:BB"
                        ),
                        StandardCharsets.UTF_8
                );

        String m3 =
                new String(
                        WifiHandshakeMicMaterial.micData(
                                "M3",
                                "00:11:22:33:44:55",
                                "66:77:88:99:AA:BB"
                        ),
                        StandardCharsets.UTF_8
                );

        return result(
                "wifi-w182-mic-message-domain",
                !m2.equals(
                        m3
                ),
                "M2/M3/M4 MIC input domains must remain distinct"
        );
    }

    private static WifiHandshakeHotfixTestResult invalidMacRejected() {
        boolean rejected =
                false;

        try {
            WifiHandshakeMicMaterial.canonicalMac(
                    "not-a-mac"
            );
        } catch (IllegalArgumentException expected) {
            rejected =
                    true;
        }

        return result(
                "wifi-w182-invalid-mac",
                rejected,
                "Invalid MAC material must fail deterministically instead of creating mismatched MIC input"
        );
    }

    private static WifiHandshakeHotfixTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiHandshakeHotfixTestResult(
                id,
                passed,
                detail
        );
    }
}
