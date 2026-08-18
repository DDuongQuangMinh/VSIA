package com.k1ngtle.vsia.signality.engineering.wifi.ip;

public final class Ipv4Address {
    private Ipv4Address() {
    }

    public static byte[] parse(
            String address
    ) {
        if (address == null) {
            throw new IllegalArgumentException(
                    "address"
            );
        }

        String[] parts =
                address.trim()
                        .split(
                                "\\."
                        );

        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "Invalid IPv4 address: "
                            + address
            );
        }

        byte[] out =
                new byte[
                        4
                ];

        for (int i = 0; i < 4; i++) {
            int value =
                    Integer.parseInt(
                            parts[i]
                    );

            if (value < 0
                    || value > 255) {
                throw new IllegalArgumentException(
                        "Invalid IPv4 address: "
                                + address
                );
            }

            out[i] =
                    (
                            byte
                    ) value;
        }

        return out;
    }

    public static boolean isUsableUnicast(
            String address
    ) {
        try {
            byte[] value =
                    parse(
                            address
                    );

            int first =
                    value[0]
                            & 0xFF;

            return first != 0
                    && first != 127
                    && first < 224
                    && !address.equals(
                    "255.255.255.255"
            );
        } catch (Exception ignored) {
            return false;
        }
    }
}
