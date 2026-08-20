package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public record Ipv6Prefix(Ipv6Address network, int length) {
    public Ipv6Prefix {
        if (network == null) {
            throw new IllegalArgumentException("network");
        }
        if (length < 0 || length > 128) {
            throw new IllegalArgumentException("IPv6 prefix length must be 0..128");
        }

        network = mask(network, length);
    }

    public static Ipv6Prefix parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("prefix");
        }

        int slash = text.indexOf('/');
        if (slash <= 0) {
            throw new IllegalArgumentException("IPv6 prefix must contain /length");
        }

        Ipv6Address address = Ipv6Address.parse(text.substring(0, slash));
        int length = Integer.parseInt(text.substring(slash + 1));
        return new Ipv6Prefix(address, length);
    }

    public boolean contains(Ipv6Address address) {
        byte[] a = address.bytes();
        byte[] n = network.bytes();

        int fullBytes = length / 8;
        int remainingBits = length % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (a[i] != n[i]) return false;
        }

        if (remainingBits == 0) return true;

        int mask = 0xFF << (8 - remainingBits);
        return (a[fullBytes] & mask) == (n[fullBytes] & mask);
    }

    public static Ipv6Address mask(Ipv6Address address, int length) {
        byte[] out = address.bytes();

        int fullBytes = length / 8;
        int remainingBits = length % 8;

        if (remainingBits != 0) {
            int mask = 0xFF << (8 - remainingBits);
            out[fullBytes] = (byte) (out[fullBytes] & mask);
            fullBytes++;
        }

        for (int i = fullBytes; i < 16; i++) {
            out[i] = 0;
        }

        return Ipv6Address.fromBytes(out);
    }

    @Override
    public String toString() {
        return network + "/" + length;
    }
}
