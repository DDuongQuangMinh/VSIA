package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
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

        RawIpv4Options.parse(
                packet.options()
        );

        if (packet.fragmentOffset() != 0
                || packet.moreFragments()) {
            throw new IllegalArgumentException(
                    "Input must be an unfragmented IPv4 datagram"
            );
        }

        if (mtu < 68 || mtu > 65535) {
            throw new IllegalArgumentException(
                    "IPv4 MTU must be in 68..65535"
            );
        }

        if (packet.totalLength() <= mtu) {
            return List.of(
                    Arrays.copyOf(
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

        byte[] payload =
                packet.payload();

        byte[] firstOptions =
                packet.options();

        byte[] copiedOptions =
                RawIpv4Options.copiedForNonInitialFragment(
                        firstOptions
                );

        List<byte[]> out =
                new ArrayList<>();

        int offsetBytes =
                0;

        while (offsetBytes < payload.length) {
            byte[] childOptions =
                    offsetBytes == 0
                            ? firstOptions
                            : copiedOptions;

            int childHeaderBytes =
                    20 + childOptions.length;

            int maxFragmentPayload =
                    ((mtu - childHeaderBytes) / 8) * 8;

            if (maxFragmentPayload < 8) {
                throw new IllegalArgumentException(
                        "MTU too small for IPv4 fragmentation with options"
                );
            }

            int remaining =
                    payload.length
                            - offsetBytes;

            int partLength =
                    Math.min(
                            maxFragmentPayload,
                            remaining
                    );

            boolean more =
                    offsetBytes + partLength
                            < payload.length;

            if (more
                    && (partLength & 7) != 0) {
                throw new IllegalStateException(
                        "Non-final fragment payload not 8-byte aligned"
                );
            }

            byte[] part =
                    Arrays.copyOfRange(
                            payload,
                            offsetBytes,
                            offsetBytes + partLength
                    );

            out.add(
                    RawIpv4Encoder.encodeWithOptions(
                            packet.sourceAddress(),
                            packet.destinationAddress(),
                            packet.dscpEcn(),
                            packet.identification(),
                            false,
                            more,
                            offsetBytes / 8,
                            packet.ttl(),
                            packet.protocol(),
                            childOptions,
                            part
                    )
            );

            offsetBytes +=
                    partLength;
        }

        return List.copyOf(
                out
        );
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
