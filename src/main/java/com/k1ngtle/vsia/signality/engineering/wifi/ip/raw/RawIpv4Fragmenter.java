package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.List;

public final class RawIpv4Fragmenter {
    private RawIpv4Fragmenter() {
    }

    public static List<byte[]> fragment(
            byte[] rawPacket,
            int mtu
    ) {
        RawIpv4Packet packet =
                RawIpv4Decoder.decode(
                        rawPacket
                );

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "IPv4 header checksum invalid"
            );
        }

        if (packet.ihlWords() != 5
                || packet.options().length != 0) {
            throw new IllegalArgumentException(
                    "W1.11.1 fragmenter supports IHL=5 packets"
            );
        }

        if (packet.fragmentOffset() != 0
                || packet.moreFragments()) {
            throw new IllegalArgumentException(
                    "Input must be an unfragmented IPv4 datagram"
            );
        }

        if (packet.totalLength() <= mtu) {
            return List.of(
                    java.util.Arrays.copyOf(
                            rawPacket,
                            packet.totalLength()
                    )
            );
        }

        if (packet.dontFragment()) {
            throw new FragmentationNeededException(
                    mtu
            );
        }

        int maxFragmentPayload =
                ((mtu - 20) / 8) * 8;

        if (maxFragmentPayload < 8) {
            throw new IllegalArgumentException(
                    "MTU too small for IPv4 fragmentation"
            );
        }

        byte[] payload =
                packet.payload();

        List<byte[]> out =
                new ArrayList<>();

        int offsetBytes =
                0;

        while (offsetBytes < payload.length) {
            int remaining =
                    payload.length - offsetBytes;

            int partLength =
                    Math.min(
                            maxFragmentPayload,
                            remaining
                    );

            boolean more =
                    offsetBytes + partLength
                            < payload.length;

            if (more && (partLength & 7) != 0) {
                throw new IllegalStateException(
                        "Non-final fragment payload not 8-byte aligned"
                );
            }

            byte[] part =
                    java.util.Arrays.copyOfRange(
                            payload,
                            offsetBytes,
                            offsetBytes + partLength
                    );

            out.add(
                    RawIpv4Encoder.encode(
                            packet.sourceAddress(),
                            packet.destinationAddress(),
                            packet.dscpEcn(),
                            packet.identification(),
                            false,
                            more,
                            offsetBytes / 8,
                            packet.ttl(),
                            packet.protocol(),
                            part
                    )
            );

            offsetBytes +=
                    partLength;
        }

        return List.copyOf(out);
    }

    public static final class FragmentationNeededException
            extends IllegalStateException {
        private final int nextHopMtu;

        public FragmentationNeededException(
                int nextHopMtu
        ) {
            super(
                    "IPv4 DF datagram exceeds MTU "
                            + nextHopMtu
            );

            this.nextHopMtu =
                    nextHopMtu;
        }

        public int nextHopMtu() {
            return nextHopMtu;
        }
    }
}
