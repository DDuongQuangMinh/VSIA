package com.k1ngtle.vsia.signality.engineering.wifi.ip.raw;

import com.k1ngtle.vsia.signality.engineering.wifi.ip.InternetChecksum;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Decoder;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.RawIpv4Packet;

import java.util.Arrays;

public final class RawIpv4TransitForwarder {
    private RawIpv4TransitForwarder() {
    }

    public static FragmentInfo inspect(byte[] rawIpv4) {
        RawIpv4Packet packet = RawIpv4Decoder.decode(rawIpv4);

        if (!packet.checksumValid()) {
            throw new IllegalArgumentException("IPv4 header checksum invalid");
        }

        return new FragmentInfo(
                packet.sourceAddress(),
                packet.destinationAddress(),
                packet.ttl(),
                packet.protocol(),
                packet.identification(),
                packet.dontFragment(),
                packet.moreFragments(),
                packet.fragmentOffset(),
                packet.totalLength(),
                packet.headerBytes(),
                packet.payload()
        );
    }

    public static byte[] forward(
            byte[] rawIpv4,
            int outgoingTtl
    ) {
        FragmentInfo before = inspect(rawIpv4);

        if (outgoingTtl < 1 || outgoingTtl > 255) {
            throw new IllegalArgumentException("outgoingTtl");
        }

        byte[] out = Arrays.copyOf(rawIpv4, before.totalLength());

        out[8] = (byte) outgoingTtl;
        out[10] = 0;
        out[11] = 0;

        int checksum = InternetChecksum.compute(
                out,
                0,
                before.headerBytes()
        );

        out[10] = (byte) (checksum >>> 8);
        out[11] = (byte) checksum;

        FragmentInfo after = inspect(out);

        if (!before.sourceIp().equals(after.sourceIp())
                || !before.destinationIp().equals(after.destinationIp())
                || before.protocol() != after.protocol()
                || before.identification() != after.identification()
                || before.dontFragment() != after.dontFragment()
                || before.moreFragments() != after.moreFragments()
                || before.fragmentOffset() != after.fragmentOffset()
                || before.totalLength() != after.totalLength()
                || !Arrays.equals(before.payload(), after.payload())) {
            throw new IllegalStateException(
                    "Transit forwarding changed immutable IPv4 fragment state"
            );
        }

        return out;
    }

    public record FragmentInfo(
            String sourceIp,
            String destinationIp,
            int ttl,
            int protocol,
            int identification,
            boolean dontFragment,
            boolean moreFragments,
            int fragmentOffset,
            int totalLength,
            int headerBytes,
            byte[] payload
    ) {
        public FragmentInfo {
            payload = payload == null ? new byte[0] : payload.clone();
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }

        public boolean fragmented() {
            return moreFragments || fragmentOffset != 0;
        }

        public boolean firstFragment() {
            return fragmentOffset == 0;
        }

        public boolean nonInitialFragment() {
            return fragmentOffset != 0;
        }

        public int fragmentOffsetBytes() {
            return fragmentOffset * 8;
        }
    }
}
