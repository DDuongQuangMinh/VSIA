package com.k1ngtle.vsia.signality.engineering.conformance;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.CRC32;

public final class PacketInspector {
    private PacketInspector() {
    }

    public static PacketInspection inspect(
            byte[] bytes
    ) {
        byte[] data =
                bytes == null
                        ? new byte[0]
                        : bytes;

        CRC32 crc =
                new CRC32();

        crc.update(
                data
        );

        String sha256;

        try {
            sha256 =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest
                                            .getInstance(
                                                    "SHA-256"
                                            )
                                            .digest(
                                                    data
                                            )
                            );
        } catch (Exception exception) {
            sha256 =
                    "unavailable";
        }

        return new PacketInspection(
                data.length,
                HexFormat.of()
                        .formatHex(
                                data
                        ),
                crc.getValue(),
                sha256
        );
    }

    public static byte[] parseHex(
            String value
    ) {
        if (value == null) {
            return new byte[0];
        }

        String normalized =
                value.replace(
                                " ",
                                ""
                        )
                        .replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        );

        if ((normalized.length() & 1) != 0) {
            throw new IllegalArgumentException(
                    "Hex input must contain an even number of digits"
            );
        }

        return HexFormat.of()
                .parseHex(
                        normalized
                );
    }
}
