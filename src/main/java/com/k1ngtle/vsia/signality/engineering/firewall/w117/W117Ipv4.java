package com.k1ngtle.vsia.signality.engineering.firewall.w117;

public final class W117Ipv4 {
    private W117Ipv4() {
    }

    public static long parse(String ip) {
        if (ip == null) {
            throw new IllegalArgumentException("IPv4 is null");
        }

        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4: " + ip);
        }

        long value = 0L;

        for (String part : parts) {
            int octet;

            try {
                octet = Integer.parseInt(part);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid IPv4: " + ip, ex);
            }

            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IPv4: " + ip);
            }

            value = (value << 8) | octet;
        }

        return value & 0xFFFFFFFFL;
    }

    public static boolean valid(String ip) {
        try {
            parse(ip);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean contiguousMask(String mask) {
        if (!valid(mask)) {
            return false;
        }

        long value = parse(mask);
        boolean zeroSeen = false;

        for (int bit = 31; bit >= 0; bit--) {
            boolean one = (value & (1L << bit)) != 0L;

            if (!one) {
                zeroSeen = true;
            } else if (zeroSeen) {
                return false;
            }
        }

        return true;
    }

    public static boolean sameSubnet(
            String a,
            String b,
            String mask
    ) {
        long m = parse(mask);
        return (parse(a) & m) == (parse(b) & m);
    }

    public static int prefixLength(String mask) {
        if (!contiguousMask(mask)) {
            throw new IllegalArgumentException("Invalid mask: " + mask);
        }

        return Long.bitCount(parse(mask));
    }
}
