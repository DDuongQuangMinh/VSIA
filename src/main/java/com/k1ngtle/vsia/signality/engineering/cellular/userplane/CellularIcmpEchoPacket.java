package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

// CELLULAR_USER_PLANE_V1
public record CellularIcmpEchoPacket(
        String sourceAddress,
        String destinationAddress,
        int identifier,
        int sequence,
        int payloadBytes,
        int checksum,
        boolean reply
) {
    public CellularIcmpEchoPacket {
        if (!validIpv4(sourceAddress)) {
            throw new IllegalArgumentException("invalid IPv4 source: " + sourceAddress);
        }
        if (!validIpv4(destinationAddress)) {
            throw new IllegalArgumentException("invalid IPv4 destination: " + destinationAddress);
        }
        if (payloadBytes < 0 || payloadBytes > 1400) {
            throw new IllegalArgumentException("payloadBytes must be in 0..1400");
        }
    }

    public static CellularIcmpEchoPacket request(
            String sourceAddress,
            String destinationAddress,
            int identifier,
            int sequence,
            int payloadBytes
    ) {
        return new CellularIcmpEchoPacket(
                sourceAddress,
                destinationAddress,
                identifier & 0xffff,
                sequence & 0xffff,
                payloadBytes,
                calculateChecksum(false, identifier, sequence, payloadBytes),
                false
        );
    }

    public CellularIcmpEchoPacket toReply() {
        return new CellularIcmpEchoPacket(
                destinationAddress,
                sourceAddress,
                identifier,
                sequence,
                payloadBytes,
                calculateChecksum(true, identifier, sequence, payloadBytes),
                true
        );
    }

    public int icmpBytes() {
        return 8 + payloadBytes;
    }

    public int ipv4Bytes() {
        return 20 + icmpBytes();
    }

    public static boolean validIpv4(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }

        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3) {
                return false;
            }

            int octet = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                octet = octet * 10 + (c - '0');
            }

            if (octet > 255) {
                return false;
            }

            if (part.length() > 1 && part.charAt(0) == '0') {
                return false;
            }
        }

        return true;
    }

    private static int calculateChecksum(
            boolean reply,
            int identifier,
            int sequence,
            int payloadBytes
    ) {
        long sum = 0L;
        int type = reply ? 0 : 8;

        sum += (long) type << 8;
        sum += identifier & 0xffffL;
        sum += sequence & 0xffffL;

        for (int i = 0; i < payloadBytes; i += 2) {
            int high = deterministicPayloadByte(i);
            int low =
                    i + 1 < payloadBytes
                            ? deterministicPayloadByte(i + 1)
                            : 0;

            sum += ((high & 0xffL) << 8) | (low & 0xffL);
            sum = fold(sum);
        }

        sum = fold(sum);
        return (int) (~sum) & 0xffff;
    }

    private static int deterministicPayloadByte(int index) {
        return (index * 31 + 7) & 0xff;
    }

    private static long fold(long value) {
        while ((value & 0xffff0000L) != 0L) {
            value = (value & 0xffffL) + (value >>> 16);
        }
        return value;
    }
}
