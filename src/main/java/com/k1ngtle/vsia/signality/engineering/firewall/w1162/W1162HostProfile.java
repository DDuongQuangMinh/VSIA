package com.k1ngtle.vsia.signality.engineering.firewall.w1162;

public record W1162HostProfile(
        String name,
        String ipv4,
        String subnetMask,
        String defaultGateway,
        String macAddress
) {
    public W1162HostProfile {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name");
        }

        if (!W1162Ipv4.valid(ipv4)) {
            throw new IllegalArgumentException("ipv4");
        }

        if (!W1162Ipv4.contiguousMask(subnetMask)) {
            throw new IllegalArgumentException("subnetMask");
        }

        if (defaultGateway == null
                || defaultGateway.isBlank()
                || !W1162Ipv4.valid(defaultGateway)) {
            throw new IllegalArgumentException("defaultGateway");
        }

        if (macAddress == null || macAddress.isBlank()) {
            throw new IllegalArgumentException("macAddress");
        }
    }

    public boolean destinationOnLink(String destinationIpv4) {
        return W1162Ipv4.sameSubnet(
                ipv4,
                destinationIpv4,
                subnetMask
        );
    }

    public String nextHop(String destinationIpv4) {
        return destinationOnLink(destinationIpv4)
                ? destinationIpv4
                : defaultGateway;
    }

    public String network() {
        return W1162Ipv4.network(
                ipv4,
                subnetMask
        );
    }

    public int prefixLength() {
        return W1162Ipv4.prefixLength(
                subnetMask
        );
    }
}
