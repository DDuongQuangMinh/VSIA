package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import java.util.List;

public final class WifiEngineeringTestLinkTestSuite {
    private WifiEngineeringTestLinkTestSuite() {
    }

    public static List<WifiEngineeringTestLinkTestResult> runAll() {
        return List.of(
                overlapExact(),
                overlapPartial(),
                rejectSeparated(),
                rankExactProfile()
        );
    }

    private static WifiEngineeringTestLinkTestResult overlapExact() {
        return result(
                "wifi-w164-frequency-overlap-exact",
                WifiEngineeringTestLinkSelector.frequenciesOverlap(
                        6.1E9,
                        320.0E6,
                        6.1E9,
                        320.0E6
                ),
                "Equal EHT center frequencies must be compatible"
        );
    }

    private static WifiEngineeringTestLinkTestResult overlapPartial() {
        return result(
                "wifi-w164-frequency-overlap-partial",
                WifiEngineeringTestLinkSelector.frequenciesOverlap(
                        6.1E9,
                        320.0E6,
                        6.2E9,
                        80.0E6
                ),
                "Overlapping receive bandwidths must remain eligible"
        );
    }

    private static WifiEngineeringTestLinkTestResult rejectSeparated() {
        return result(
                "wifi-w164-frequency-reject-separated",
                !WifiEngineeringTestLinkSelector.frequenciesOverlap(
                        2.437E9,
                        20.0E6,
                        6.1E9,
                        320.0E6
                ),
                "Widely separated Wi-Fi channels must not be selected as peers"
        );
    }

    private static WifiEngineeringTestLinkTestResult rankExactProfile() {
        double exact =
                WifiEngineeringTestLinkSelector.candidateScore(
                        10.0,
                        true,
                        true
                );

        double other =
                WifiEngineeringTestLinkSelector.candidateScore(
                        5.0,
                        false,
                        true
                );

        return result(
                "wifi-w164-peer-ranking",
                exact < other,
                "An exact-profile RF peer should outrank an unrelated profile at similar range"
        );
    }

    private static WifiEngineeringTestLinkTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiEngineeringTestLinkTestResult(
                id,
                passed,
                detail
        );
    }
}
