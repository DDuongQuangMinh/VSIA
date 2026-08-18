package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

public final class RawPacketHex {
    private static final char[] HEX =
            "0123456789ABCDEF".toCharArray();

    private RawPacketHex() {
    }

    public static String encode(
            byte[] bytes
    ) {
        if (bytes == null) {
            return "";
        }

        char[] out =
                new char[
                        bytes.length * 2
                ];

        for (int i = 0;
             i < bytes.length;
             i++) {
            int value =
                    bytes[i]
                            & 0xFF;

            out[i * 2] =
                    HEX[
                            value >>> 4
                    ];

            out[i * 2 + 1] =
                    HEX[
                            value & 0x0F
                    ];
        }

        return new String(
                out
        );
    }

    public static byte[] decode(
            String hex
    ) {
        if (hex == null) {
            return new byte[0];
        }

        String normalized =
                hex.replaceAll(
                        "[^0-9A-Fa-f]",
                        ""
                );

        if (
                (
                        normalized.length()
                                & 1
                )
                        != 0
        ) {
            throw new IllegalArgumentException(
                    "Hex string must contain an even number of digits"
            );
        }

        byte[] out =
                new byte[
                        normalized.length()
                                / 2
                ];

        for (int i = 0;
             i < out.length;
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
                        "Invalid hex digit"
                );
            }

            out[i] =
                    (
                            byte
                    ) (
                    high << 4
                            | low
            );
        }

        return out;
    }
}
