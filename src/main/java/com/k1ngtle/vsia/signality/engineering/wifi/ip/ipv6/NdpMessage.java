package com.k1ngtle.vsia.signality.engineering.wifi.ip.ipv6;

public sealed interface NdpMessage permits
        NdpMessage.RouterSolicitation,
        NdpMessage.RouterAdvertisement,
        NdpMessage.NeighborSolicitation,
        NdpMessage.NeighborAdvertisement {

    record RouterSolicitation(
            String sourceLinkLayerAddress
    ) implements NdpMessage {
    }

    record RouterAdvertisement(
            int currentHopLimit,
            boolean managed,
            boolean otherConfig,
            int routerLifetimeSeconds,
            Ipv6Prefix prefix,
            boolean autonomous,
            boolean onLink,
            int validLifetimeSeconds,
            int preferredLifetimeSeconds,
            String sourceLinkLayerAddress
    ) implements NdpMessage {
        public RouterAdvertisement {
            if (currentHopLimit < 0 || currentHopLimit > 255) throw new IllegalArgumentException("hop limit");
            if (routerLifetimeSeconds < 0 || routerLifetimeSeconds > 65535) throw new IllegalArgumentException("router lifetime");
            if (prefix == null) throw new IllegalArgumentException("prefix");
        }
    }

    record NeighborSolicitation(
            Ipv6Address target,
            String sourceLinkLayerAddress
    ) implements NdpMessage {
        public NeighborSolicitation {
            if (target == null) throw new IllegalArgumentException("target");
        }
    }

    record NeighborAdvertisement(
            Ipv6Address target,
            boolean router,
            boolean solicited,
            boolean override,
            String targetLinkLayerAddress
    ) implements NdpMessage {
        public NeighborAdvertisement {
            if (target == null) throw new IllegalArgumentException("target");
        }
    }
}
