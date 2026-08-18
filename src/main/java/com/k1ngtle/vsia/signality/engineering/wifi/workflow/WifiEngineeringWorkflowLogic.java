package com.k1ngtle.vsia.signality.engineering.wifi.workflow;

import java.util.List;

public final class WifiEngineeringWorkflowLogic {
    public static final int DEFAULT_DATA_BYTES = 512;

    private WifiEngineeringWorkflowLogic() {
    }

    public static String defaultApSsid(
            String macAddress
    ) {
        String normalized =
                macAddress == null
                        ? ""
                        : macAddress.replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        )
                        .toUpperCase();

        if (normalized.length() > 6) {
            normalized =
                    normalized.substring(
                            normalized.length() - 6
                    );
        }

        if (normalized.isBlank()) {
            normalized =
                    "000000";
        }

        return "VSIA-AP-"
                + normalized;
    }

    public static String firstDiscoveredSsid(
            List<String> ssids
    ) {
        if (ssids == null) {
            return "";
        }

        return ssids.stream()
                .filter(
                        value ->
                                value != null
                                        && !value.isBlank()
                )
                .findFirst()
                .orElse(
                        ""
                );
    }

    public static int clampDataBytes(
            int requested
    ) {
        return Math.max(
                64,
                Math.min(
                        4096,
                        requested
                )
        );
    }
}
