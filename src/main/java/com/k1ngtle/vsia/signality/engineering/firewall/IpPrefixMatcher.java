package com.k1ngtle.vsia.signality.engineering.firewall;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpPrefixMatcher {
    private IpPrefixMatcher() {
    }

    public static boolean matches(String address, String prefixText) {
        if (prefixText == null
                || prefixText.isBlank()
                || prefixText.equalsIgnoreCase("ANY")) {
            return true;
        }

        try {
            String[] parts = prefixText.split("/", 2);
            byte[] addressBytes = InetAddress.getByName(address).getAddress();
            byte[] networkBytes = InetAddress.getByName(parts[0]).getAddress();

            if (addressBytes.length != networkBytes.length) return false;

            int bits = parts.length == 2
                    ? Integer.parseInt(parts[1])
                    : networkBytes.length * 8;

            if (bits < 0 || bits > networkBytes.length * 8) return false;

            int fullBytes = bits / 8;
            int remaining = bits % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (addressBytes[i] != networkBytes[i]) return false;
            }

            if (remaining == 0) return true;

            int mask = (0xFF << (8 - remaining)) & 0xFF;
            return (addressBytes[fullBytes] & mask)
                    == (networkBytes[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException exception) {
            return false;
        }
    }

    public static IpFamily family(String address) {
        try {
            return InetAddress.getByName(address).getAddress().length == 16
                    ? IpFamily.IPV6
                    : IpFamily.IPV4;
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Invalid IP address: " + address, exception);
        }
    }
}
