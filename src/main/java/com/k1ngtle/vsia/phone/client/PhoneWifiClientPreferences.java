package com.k1ngtle.vsia.phone.client;

import java.util.HashMap;
import java.util.Map;

public final class PhoneWifiClientPreferences {
    private static final Map<String, Boolean>
            AUTO_JOIN =
            new HashMap<>();

    private PhoneWifiClientPreferences() {
    }

    public static boolean autoJoin(
            String bssid
    ) {
        return AUTO_JOIN
                .getOrDefault(
                        normalize(
                                bssid
                        ),
                        true
                );
    }

    public static void setAutoJoin(
            String bssid,
            boolean enabled
    ) {
        AUTO_JOIN.put(
                normalize(
                        bssid
                ),
                enabled
        );
    }

    public static void forget(
            String bssid
    ) {
        AUTO_JOIN.remove(
                normalize(
                        bssid
                )
        );
    }

    private static String normalize(
            String value
    ) {
        return value == null
                ? ""
                : value.trim()
                .toUpperCase(
                        java.util.Locale.ROOT
                );
    }
}
