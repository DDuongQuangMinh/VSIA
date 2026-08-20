package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

public final class RawIpv4Encoder {
    private RawIpv4Encoder() {
    }

    public static byte[] encode(
            String sourceIp,
            String destinationIp,
            int dscpEcn,
            int identification,
            boolean dontFragment,
            boolean moreFragments,
            int fragmentOffsetUnits,
            int ttl,
            int protocol,
            byte[] payload
    ) {
        if (identification < 0 || identification > 0xFFFF) {
            throw new IllegalArgumentException("identification");
        }

        if (fragmentOffsetUnits < 0 || fragmentOffsetUnits > 0x1FFF) {
            throw new IllegalArgumentException("fragmentOffsetUnits");
        }

        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("ttl");
        }

        if (protocol < 0 || protocol > 255) {
            throw new IllegalArgumentException("protocol");
        }

        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        int totalLength =
                20 + body.length;

        if (totalLength > 65535) {
            throw new IllegalArgumentException("IPv4 packet too large");
        }

        byte[] out =
                new byte[totalLength];

        out[0] =
                0x45;

        out[1] =
                (byte) dscpEcn;

        put16(out, 2, totalLength);
        put16(out, 4, identification);

        int flagsAndOffset =
                fragmentOffsetUnits & 0x1FFF;

        if (dontFragment) {
            flagsAndOffset |= 0x4000;
        }

        if (moreFragments) {
            flagsAndOffset |= 0x2000;
        }

        put16(out, 6, flagsAndOffset);

        out[8] =
                (byte) ttl;

        out[9] =
                (byte) protocol;

        byte[] source =
                Ipv4Address.parse(sourceIp);

        byte[] destination =
                Ipv4Address.parse(destinationIp);

        System.arraycopy(source, 0, out, 12, 4);
        System.arraycopy(destination, 0, out, 16, 4);

        int checksum =
                InternetChecksum.compute(
                        java.util.Arrays.copyOfRange(
                                out,
                                0,
                                20
                        )
                );

        put16(out, 10, checksum);

        System.arraycopy(
                body,
                0,
                out,
                20,
                body.length
        );

        return out;
    }

    private static void put16(
            byte[] data,
            int offset,
            int value
    ) {
        data[offset] =
                (byte) (
                        value >>> 8
                );

        data[offset + 1] =
                (byte) value;
    }
}
