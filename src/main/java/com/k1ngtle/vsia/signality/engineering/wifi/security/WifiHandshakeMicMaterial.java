package com.k1ngtle.vsia.signality.engineering.wifi.security;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class WifiHandshakeMicMaterial {
    private WifiHandshakeMicMaterial() {
    }

    public static String canonicalMac(
            String mac
    ) {
        if (mac == null) {
            throw new IllegalArgumentException(
                    "MAC address cannot be null"
            );
        }

        String normalized =
                mac.replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        )
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (normalized.length() != 12) {
            throw new IllegalArgumentException(
                    "Invalid MAC address: "
                            + mac
            );
        }

        StringBuilder out =
                new StringBuilder(
                        17
                );

        for (int i = 0; i < 6; i++) {
            int start =
                    i * 2;

            String pair =
                    normalized.substring(
                            start,
                            start + 2
                    );

            Integer.parseInt(
                    pair,
                    16
            );

            if (i > 0) {
                out.append(
                        ':'
                );
            }

            out.append(
                    pair
            );
        }

        return out.toString();
    }

    public static byte[] micData(
            String messageLabel,
            String apMac,
            String stationMac
    ) {
        if (messageLabel == null
                || messageLabel.isBlank()) {
            throw new IllegalArgumentException(
                    "messageLabel"
            );
        }

        return (
                messageLabel
                        + "|"
                        + canonicalMac(
                        apMac
                )
                        + "|"
                        + canonicalMac(
                        stationMac
                )
        ).getBytes(
                StandardCharsets.UTF_8
        );
    }
}
