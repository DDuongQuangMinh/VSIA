package com.k1ngtle.vsia.signality.engineering.wifi.arp;

public final class MacAddressBytes {
    private MacAddressBytes() {
    }

    public static byte[] parse(
            String mac
    ) {
        if (mac == null) {
            throw new IllegalArgumentException(
                    "MAC address is null"
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
                        .trim();

        if (normalized.length()
                != 12) {
            throw new IllegalArgumentException(
                    "MAC address must contain 48 bits"
            );
        }

        byte[] bytes =
                new byte[6];

        for (int i = 0;
             i < bytes.length;
             i++) {
            int high =
                    Character.digit(
                            normalized.charAt(
                                    i * 2
                            ),
                            16
                    );

            int low =
                    Character.digit(
                            normalized.charAt(
                                    i * 2 + 1
                            ),
                            16
                    );

            if (high < 0
                    || low < 0) {
                throw new IllegalArgumentException(
                        "Invalid MAC address"
                );
            }

            bytes[i] =
                    (
                            byte
                    ) (
                    high << 4
                            | low
            );
        }

        return bytes;
    }

    public static String format(
            byte[] bytes,
            int offset
    ) {
        if (bytes == null
                || offset < 0
                || offset + 6
                > bytes.length) {
            throw new IllegalArgumentException(
                    "Six MAC bytes are required"
            );
        }

        return String.format(
                java.util.Locale.ROOT,
                "%02X:%02X:%02X:%02X:%02X:%02X",
                bytes[offset] & 0xFF,
                bytes[offset + 1] & 0xFF,
                bytes[offset + 2] & 0xFF,
                bytes[offset + 3] & 0xFF,
                bytes[offset + 4] & 0xFF,
                bytes[offset + 5] & 0xFF
        );
    }

    public static String zero() {
        return "00:00:00:00:00:00";
    }

    public static String broadcast() {
        return "FF:FF:FF:FF:FF:FF";
    }
}
