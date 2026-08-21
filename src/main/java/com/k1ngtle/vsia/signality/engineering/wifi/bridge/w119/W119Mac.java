package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

public final class W119Mac {
    public static final String BROADCAST =
            "FF:FF:FF:FF:FF:FF";

    private W119Mac() {
    }

    public static String normalize(String macAddress) {
        if (macAddress == null) {
            return "";
        }

        String hex =
                macAddress
                        .replace(":", "")
                        .replace("-", "")
                        .replace(".", "")
                        .trim()
                        .toUpperCase();

        if (hex.length() != 12) {
            return macAddress.trim().toUpperCase();
        }

        StringBuilder out =
                new StringBuilder(17);

        for (int i = 0; i < 12; i += 2) {
            if (out.length() > 0) {
                out.append(':');
            }

            out.append(
                    hex,
                    i,
                    i + 2
            );
        }

        return out.toString();
    }

    public static boolean equals(
            String first,
            String second
    ) {
        return normalize(first)
                .equals(
                        normalize(second)
                );
    }

    public static boolean isBroadcast(
            String macAddress
    ) {
        return BROADCAST.equals(
                normalize(macAddress)
        );
    }

    public static boolean isMulticast(
            String macAddress
    ) {
        String normalized =
                normalize(macAddress);

        if (normalized.length() != 17) {
            return false;
        }

        try {
            int firstOctet =
                    Integer.parseInt(
                            normalized.substring(0, 2),
                            16
                    );

            return (firstOctet & 0x01) != 0
                    && !isBroadcast(normalized);
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static boolean isGroup(
            String macAddress
    ) {
        return isBroadcast(macAddress)
                || isMulticast(macAddress);
    }
}
