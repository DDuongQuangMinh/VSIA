package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Ipv6SourceFragmenter {
    private Ipv6SourceFragmenter() {
    }

    public static List<byte[]> fragment(
            byte[] original,
            int mtu,
            long identification
    ) {
        RawIpv6Packet packet = RawIpv6Codec.decode(original);

        if (mtu < 1280) {
            throw new IllegalArgumentException("IPv6 minimum link MTU is 1280");
        }

        if (packet.totalLength() <= mtu) {
            return List.of(Arrays.copyOf(original, packet.totalLength()));
        }

        int maxPayload =
                ((mtu - 40 - Ipv6FragmentHeader.BYTES) / 8) * 8;

        if (maxPayload < 8) {
            throw new IllegalArgumentException("MTU too small for source fragmentation");
        }

        List<byte[]> out = new ArrayList<>();
        int offset = 0;
        byte[] originalPayload = packet.payload();

        while (offset < originalPayload.length) {
            int remaining = originalPayload.length - offset;
            int length = Math.min(maxPayload, remaining);
            boolean more = offset + length < originalPayload.length;

            if (more && (length & 7) != 0) {
                throw new IllegalStateException("Non-final IPv6 fragment not 8-byte aligned");
            }

            Ipv6FragmentHeader fragmentHeader =
                    new Ipv6FragmentHeader(
                            packet.nextHeader(),
                            offset / 8,
                            more,
                            identification
                    );

            byte[] payload = new byte[8 + length];
            System.arraycopy(fragmentHeader.encode(), 0, payload, 0, 8);
            System.arraycopy(originalPayload, offset, payload, 8, length);

            out.add(
                    RawIpv6Codec.encode(
                            packet.source(),
                            packet.destination(),
                            packet.trafficClass(),
                            packet.flowLabel(),
                            Ipv6FragmentHeader.NEXT_HEADER_FRAGMENT,
                            packet.hopLimit(),
                            payload
                    )
            );

            offset += length;
        }

        return List.copyOf(out);
    }
}
