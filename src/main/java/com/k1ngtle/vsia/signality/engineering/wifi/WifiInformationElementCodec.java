package com.k1ngtle.vsia.signality.engineering.wifi;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class WifiInformationElementCodec {
    private WifiInformationElementCodec() {
    }

    public static byte[] encode(
            List<WifiInformationElement> elements
    ) {
        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        for (WifiInformationElement element : elements) {
            byte[] data =
                    element.data();

            out.write(
                    element.id()
            );

            out.write(
                    data.length
            );

            out.writeBytes(
                    data
            );
        }

        return out.toByteArray();
    }

    public static List<WifiInformationElement> decode(
            byte[] bytes
    ) {
        List<WifiInformationElement> result =
                new ArrayList<>();

        int offset =
                0;

        while (offset < bytes.length) {
            if (offset + 2 > bytes.length) {
                throw new IllegalArgumentException(
                        "Truncated Information Element header"
                );
            }

            int id =
                    bytes[offset]
                            & 0xFF;

            int length =
                    bytes[offset + 1]
                            & 0xFF;

            offset += 2;

            if (offset + length
                    > bytes.length) {
                throw new IllegalArgumentException(
                        "Truncated Information Element body"
                );
            }

            byte[] data =
                    new byte[length];

            System.arraycopy(
                    bytes,
                    offset,
                    data,
                    0,
                    length
            );

            offset +=
                    length;

            result.add(
                    new WifiInformationElement(
                            id,
                            data
                    )
            );
        }

        return List.copyOf(
                result
        );
    }
}
