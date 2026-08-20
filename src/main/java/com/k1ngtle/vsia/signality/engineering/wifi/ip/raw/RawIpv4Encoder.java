package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address;

import java.util.Arrays;

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
        return encodeWithOptions(
                sourceIp,
                destinationIp,
                dscpEcn,
                identification,
                dontFragment,
                moreFragments,
                fragmentOffsetUnits,
                ttl,
                protocol,
                new byte[0],
                payload
        );
    }

    public static byte[] encodeWithOptions(
            String sourceIp,
            String destinationIp,
            int dscpEcn,
            int identification,
            boolean dontFragment,
            boolean moreFragments,
            int fragmentOffsetUnits,
            int ttl,
            int protocol,
            byte[] options,
            byte[] payload
    ) {
        if (dscpEcn < 0 || dscpEcn > 255) {
            throw new IllegalArgumentException("dscpEcn");
        }

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

        byte[] headerOptions =
                options == null
                        ? new byte[0]
                        : options.clone();

        RawIpv4Options.parse(headerOptions);
        RawIpv4Options.validateForFragmentOffset(
                headerOptions,
                fragmentOffsetUnits
        );

        byte[] body =
                payload == null
                        ? new byte[0]
                        : payload.clone();

        int headerBytes =
                20 + headerOptions.length;

        int ihlWords =
                headerBytes / 4;

        if (ihlWords < 5 || ihlWords > 15) {
            throw new IllegalArgumentException(
                    "IPv4 IHL outside 5..15"
            );
        }

        int totalLength =
                headerBytes + body.length;

        if (totalLength > 65535) {
            throw new IllegalArgumentException(
                    "IPv4 packet too large"
            );
        }

        byte[] out =
                new byte[totalLength];

        out[0] =
                (byte) (
                        0x40 | ihlWords
                );

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

        put16(
                out,
                6,
                flagsAndOffset
        );

        out[8] =
                (byte) ttl;

        out[9] =
                (byte) protocol;

        byte[] source =
                Ipv4Address.parse(sourceIp);

        byte[] destination =
                Ipv4Address.parse(destinationIp);

        System.arraycopy(
                source,
                0,
                out,
                12,
                4
        );

        System.arraycopy(
                destination,
                0,
                out,
                16,
                4
        );

        System.arraycopy(
                headerOptions,
                0,
                out,
                20,
                headerOptions.length
        );

        int checksum =
                InternetChecksum.compute(
                        Arrays.copyOfRange(
                                out,
                                0,
                                headerBytes
                        )
                );

        put16(
                out,
                10,
                checksum
        );

        System.arraycopy(
                body,
                0,
                out,
                headerBytes,
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
