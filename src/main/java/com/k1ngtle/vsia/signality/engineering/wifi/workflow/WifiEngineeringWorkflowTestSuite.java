package com.k1ngtle.vsia.signality.engineering.wifi.workflow;

import java.util.List;

public final class WifiEngineeringWorkflowTestSuite {
    private WifiEngineeringWorkflowTestSuite() {
    }

    public static List<WifiEngineeringWorkflowTestResult> runAll() {
        return List.of(
                apSsidFromMac(),
                apSsidFallback(),
                firstDiscovered(),
                firstDiscoveredSkipsBlank(),
                payloadClamp()
        );
    }

    private static WifiEngineeringWorkflowTestResult apSsidFromMac() {
        return result(
                "wifi-w18-ap-ssid",
                WifiEngineeringWorkflowLogic
                        .defaultApSsid(
                                "00:11:22:AA:BB:CC"
                        )
                        .equals(
                                "VSIA-AP-AABBCC"
                        ),
                "Default engineering AP SSID must be deterministic from the endpoint MAC"
        );
    }

    private static WifiEngineeringWorkflowTestResult apSsidFallback() {
        return result(
                "wifi-w18-ap-ssid-fallback",
                WifiEngineeringWorkflowLogic
                        .defaultApSsid(
                                ""
                        )
                        .equals(
                                "VSIA-AP-000000"
                        ),
                "Blank MAC input must still produce a valid deterministic engineering SSID"
        );
    }

    private static WifiEngineeringWorkflowTestResult firstDiscovered() {
        return result(
                "wifi-w18-first-discovered",
                WifiEngineeringWorkflowLogic
                        .firstDiscoveredSsid(
                                List.of(
                                        "AP-A",
                                        "AP-B"
                                )
                        )
                        .equals(
                                "AP-A"
                        ),
                "Connect-first workflow must preserve discovered network order"
        );
    }

    private static WifiEngineeringWorkflowTestResult firstDiscoveredSkipsBlank() {
        return result(
                "wifi-w18-first-discovered-skip-blank",
                WifiEngineeringWorkflowLogic
                        .firstDiscoveredSsid(
                                List.of(
                                        "",
                                        "AP-B"
                                )
                        )
                        .equals(
                                "AP-B"
                        ),
                "Connect-first must ignore blank SSID entries"
        );
    }

    private static WifiEngineeringWorkflowTestResult payloadClamp() {
        return result(
                "wifi-w18-data-byte-clamp",
                WifiEngineeringWorkflowLogic
                        .clampDataBytes(
                                1
                        ) == 64
                        && WifiEngineeringWorkflowLogic
                        .clampDataBytes(
                                9000
                        ) == 4096,
                "Engineering DATA payload must remain inside the bounded 64..4096 byte range"
        );
    }

    private static WifiEngineeringWorkflowTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiEngineeringWorkflowTestResult(
                id,
                passed,
                detail
        );
    }
}
