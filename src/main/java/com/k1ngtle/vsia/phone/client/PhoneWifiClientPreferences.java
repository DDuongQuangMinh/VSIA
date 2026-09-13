package com.k1ngtle.vsia.phone.client;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class PhoneWifiClientPreferences {
    private static final Map<String, Boolean>
            AUTO_JOIN =
            new HashMap<>();

    private static final Map<String, String>
            PASSWORDS =
            new HashMap<>();

    private PhoneWifiClientPreferences() {
    }

    public static boolean autoJoin(
            String bssid
    ) {
        return AUTO_JOIN
                .getOrDefault(
                        normalizeBssid(
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
                normalizeBssid(
                        bssid
                ),
                enabled
        );
    }

    public static void rememberPassword(
            String ssid,
            String security,
            String password
    ) {
        PASSWORDS.put(
                networkKey(
                        ssid,
                        security
                ),
                password == null
                        ? ""
                        : password
        );
    }

    public static String password(
            String ssid,
            String security
    ) {
        return PASSWORDS
                .getOrDefault(
                        networkKey(
                                ssid,
                                security
                        ),
                        ""
                );
    }

    public static boolean hasRememberedPassword(
            String ssid,
            String security
    ) {
        return PASSWORDS.containsKey(
                networkKey(
                        ssid,
                        security
                )
        );
    }

    public static void forget(
            String bssid
    ) {
        AUTO_JOIN.remove(
                normalizeBssid(
                        bssid
                )
        );
    }

    public static void forgetNetwork(
            String ssid,
            String security,
            String bssid
    ) {
        forget(
                bssid
        );

        PASSWORDS.remove(
                networkKey(
                        ssid,
                        security
                )
        );
    }

    public static void clearAll() {
        AUTO_JOIN.clear();
        PASSWORDS.clear();
    }

    private static String normalizeBssid(
            String value
    ) {
        return value == null
                ? ""
                : value.trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    private static String networkKey(
            String ssid,
            String security
    ) {
        String safeSsid =
                ssid == null
                        ? ""
                        : ssid.trim();

        String safeSecurity =
                security == null
                        ? ""
                        : security.trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return safeSsid
                + "\u0000"
                + safeSecurity;
    }
}
