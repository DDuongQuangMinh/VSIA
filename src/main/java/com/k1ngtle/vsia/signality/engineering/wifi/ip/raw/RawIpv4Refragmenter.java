package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RawIpv4Refragmenter {
    private RawIpv4Refragmenter() {
    }

    public static List<byte[]> refragment(
            byte[] rawFragment,
            int mtu,
            int outgoingTtl
    ) {
        RawIpv4Packet packet =
                RawIpv4Decoder.decode(
                        rawFragment
                );

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException(
                    "IPv4 header checksum invalid"
            );
        }

        if (outgoingTtl < 1 || outgoingTtl > 255) {
            throw new IllegalArgumentException(
                    "outgoingTtl"
            );
        }

        if (mtu < 68 || mtu > 65535) {
            throw new IllegalArgumentException(
                    "IPv4 MTU must be in 68..65535"
            );
        }

        if (packet.totalLength() <= mtu) {
            return List.of(
                    RawIpv4TransitForwarder.forward(
                            rawFragment,
                            outgoingTtl
                    )
            );
        }

        if (packet.dontFragment()) {
            throw new RawIpv4Fragmenter.FragmentationNeededException(
                    mtu
            );
        }

        if (packet.ihlWords() != 5
                || packet.options().length != 0) {
            throw new IllegalArgumentException(
                    "W1.11.3 re-fragmentation supports IHL=5 packets"
            );
        }

        byte[] payload =
                packet.payload();

        if (packet.moreFragments()
                && (payload.length & 7) != 0) {
            throw new IllegalArgumentException(
                    "Incoming non-final fragment payload must be 8-byte aligned"
            );
        }

        int headerBytes =
                packet.headerBytes();

        int maximumChildPayload =
                ((mtu - headerBytes) / 8) * 8;

        if (maximumChildPayload < 8) {
            throw new IllegalArgumentException(
                    "MTU too small for IPv4 re-fragmentation"
            );
        }

        List<byte[]> children =
                new ArrayList<>();

        int localOffsetBytes =
                0;

        while (localOffsetBytes < payload.length) {
            int remaining =
                    payload.length
                            - localOffsetBytes;

            int childPayloadLength =
                    Math.min(
                            maximumChildPayload,
                            remaining
                    );

            boolean moreInsideParent =
                    localOffsetBytes
                            + childPayloadLength
                            < payload.length;

            boolean childMoreFragments =
                    packet.moreFragments()
                            || moreInsideParent;

            if (childMoreFragments
                    && (childPayloadLength & 7) != 0) {
                throw new IllegalStateException(
                        "Non-final child payload is not 8-byte aligned"
                );
            }

            int childOffsetUnits =
                    packet.fragmentOffset()
                            + localOffsetBytes / 8;

            if (childOffsetUnits > 0x1FFF) {
                throw new IllegalArgumentException(
                        "IPv4 fragment offset exceeds 13-bit field"
                );
            }

            byte[] childPayload =
                    Arrays.copyOfRange(
                            payload,
                            localOffsetBytes,
                            localOffsetBytes
                                    + childPayloadLength
                    );

            children.add(
                    RawIpv4Encoder.encode(
                            packet.sourceAddress(),
                            packet.destinationAddress(),
                            packet.dscpEcn(),
                            packet.identification(),
                            false,
                            childMoreFragments,
                            childOffsetUnits,
                            outgoingTtl,
                            packet.protocol(),
                            childPayload
                    )
            );

            localOffsetBytes +=
                    childPayloadLength;
        }

        return List.copyOf(
                children
        );
    }
}
