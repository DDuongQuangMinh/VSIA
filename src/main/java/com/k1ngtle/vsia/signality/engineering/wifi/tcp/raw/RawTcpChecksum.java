package com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

import java.io.ByteArrayOutputStream;

public final class RawTcpChecksum {
    private RawTcpChecksum() {
    }

    public static int compute(
            String sourceIp,
            String destinationIp,
            byte[] tcpSegment
    ) {
        if (tcpSegment == null) {
            throw new IllegalArgumentException(
                    "tcpSegment"
            );
        }

        ByteArrayOutputStream pseudo =
                new ByteArrayOutputStream();

        pseudo.writeBytes(
                Ipv4Address.parse(
                        sourceIp
                )
        );

        pseudo.writeBytes(
                Ipv4Address.parse(
                        destinationIp
                )
        );

        pseudo.write(
                0
        );

        pseudo.write(
                6
        );

        pseudo.write(
                (
                        tcpSegment.length
                                >>> 8
                )
                        & 0xFF
        );

        pseudo.write(
                tcpSegment.length
                        & 0xFF
        );

        pseudo.writeBytes(
                tcpSegment
        );

        if (
                (
                        pseudo.size()
                                & 1
                )
                        != 0
        ) {
            pseudo.write(
                    0
            );
        }

        return InternetChecksum.compute(
                pseudo.toByteArray()
        );
    }

    public static boolean valid(
            String sourceIp,
            String destinationIp,
            byte[] tcpSegment
    ) {
        return compute(
                sourceIp,
                destinationIp,
                tcpSegment
        )
                == 0;
    }
}
