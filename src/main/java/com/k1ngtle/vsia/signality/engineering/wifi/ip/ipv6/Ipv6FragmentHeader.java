package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record Ipv6FragmentHeader(
        int nextHeader,
        int fragmentOffsetUnits,
        boolean moreFragments,
        long identification
) {
    public static final int BYTES = 8;
    public static final int NEXT_HEADER_FRAGMENT = 44;

    public Ipv6FragmentHeader {
        if (nextHeader < 0 || nextHeader > 255) throw new IllegalArgumentException("nextHeader");
        if (fragmentOffsetUnits < 0 || fragmentOffsetUnits > 0x1FFF) throw new IllegalArgumentException("offset");
        if (identification < 0 || identification > 0xFFFFFFFFL) throw new IllegalArgumentException("identification");
    }

    public byte[] encode() {
        byte[] out = new byte[8];
        out[0] = (byte) nextHeader;

        int field = (fragmentOffsetUnits & 0x1FFF) << 3;
        if (moreFragments) field |= 1;

        out[2] = (byte) (field >>> 8);
        out[3] = (byte) field;

        out[4] = (byte) (identification >>> 24);
        out[5] = (byte) (identification >>> 16);
        out[6] = (byte) (identification >>> 8);
        out[7] = (byte) identification;

        return out;
    }

    public static Ipv6FragmentHeader decode(byte[] raw) {
        if (raw == null || raw.length < 8) {
            throw new IllegalArgumentException("Truncated IPv6 Fragment header");
        }

        int field =
                ((raw[2] & 0xFF) << 8)
                        | (raw[3] & 0xFF);

        int offset = (field >>> 3) & 0x1FFF;
        boolean more = (field & 1) != 0;

        long id =
                ((long) (raw[4] & 0xFF) << 24)
                        | ((long) (raw[5] & 0xFF) << 16)
                        | ((long) (raw[6] & 0xFF) << 8)
                        | (raw[7] & 0xFFL);

        return new Ipv6FragmentHeader(
                raw[0] & 0xFF,
                offset,
                more,
                id
        );
    }
}
