package com.k1ngtle.vsia.signality.engineering.wifi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public final class WifiAmpdu {
    private WifiAmpdu() {
    }

    public static byte[] encode(
            List<byte[]> mpdus
    ) {
        try {
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream();

            DataOutputStream out =
                    new DataOutputStream(bytes);

            out.writeInt(
                    mpdus.size()
            );

            for (byte[] mpdu : mpdus) {
                CRC32 crc =
                        new CRC32();

                crc.update(mpdu);

                out.writeInt(
                        mpdu.length
                );

                out.writeInt(
                        (int) crc.getValue()
                );

                out.write(
                        mpdu
                );

                int padding =
                        (4 - (mpdu.length % 4)) % 4;

                for (int i = 0;
                     i < padding;
                     i++) {
                    out.writeByte(0);
                }
            }

            return bytes.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to encode A-MPDU",
                    exception
            );
        }
    }

    public static List<byte[]> decode(
            byte[] encoded
    ) {
        try {
            DataInputStream in =
                    new DataInputStream(
                            new ByteArrayInputStream(
                                    encoded
                            )
                    );

            int count =
                    in.readInt();

            if (count < 0 || count > 256) {
                throw new IllegalArgumentException(
                        "Invalid A-MPDU subframe count"
                );
            }

            List<byte[]> result =
                    new ArrayList<>(count);

            for (int i = 0;
                 i < count;
                 i++) {
                int length =
                        in.readInt();

                long expectedCrc =
                        Integer.toUnsignedLong(
                                in.readInt()
                        );

                if (length < 0
                        || length > 1_048_576) {
                    throw new IllegalArgumentException(
                            "Invalid MPDU length"
                    );
                }

                byte[] mpdu =
                        new byte[length];

                in.readFully(
                        mpdu
                );

                CRC32 crc =
                        new CRC32();

                crc.update(
                        mpdu
                );

                if (crc.getValue()
                        != expectedCrc) {
                    throw new IllegalArgumentException(
                            "A-MPDU delimiter CRC mismatch"
                    );
                }

                int padding =
                        (4 - (length % 4)) % 4;

                for (int p = 0;
                     p < padding;
                     p++) {
                    in.readByte();
                }

                result.add(
                        mpdu
                );
            }

            return result;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Failed to decode A-MPDU",
                    exception
            );
        }
    }
}
