package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

public record RouterPacket(
        String sourceIp,
        String destinationIp,
        int ttl,
        int protocol,
        int identification,
        boolean dontFragment,
        boolean moreFragments,
        int fragmentOffset,
        int totalLength,
        byte[] payload
) {
    public RouterPacket {
        sourceIp = sourceIp == null ? "" : sourceIp;
        destinationIp = destinationIp == null ? "" : destinationIp;
        payload = payload == null ? new byte[0] : payload.clone();

        if (ttl < 0 || ttl > 255) {
            throw new IllegalArgumentException("ttl");
        }
        if (protocol < 0 || protocol > 255) {
            throw new IllegalArgumentException("protocol");
        }
        if (identification < 0 || identification > 0xFFFF) {
            throw new IllegalArgumentException("identification");
        }
        if (fragmentOffset < 0 || fragmentOffset > 0x1FFF) {
            throw new IllegalArgumentException("fragmentOffset");
        }
        if (totalLength < 0 || totalLength > 65535) {
            throw new IllegalArgumentException("totalLength");
        }
    }

    public RouterPacket(
            String sourceIp,
            String destinationIp,
            int ttl,
            int protocol,
            byte[] payload
    ) {
        this(
                sourceIp,
                destinationIp,
                ttl,
                protocol,
                0,
                false,
                false,
                0,
                20 + (payload == null ? 0 : payload.length),
                payload
        );
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public RouterPacket withTtl(int nextTtl) {
        return new RouterPacket(
                sourceIp,
                destinationIp,
                nextTtl,
                protocol,
                identification,
                dontFragment,
                moreFragments,
                fragmentOffset,
                totalLength,
                payload
        );
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
