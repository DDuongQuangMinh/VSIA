package com.k1ngtle.vsia.signality.engineering.firewall.w118;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

public final class W118DhcpRelay {
    private final String relayAddress;
    private final String serverAddress;

    public W118DhcpRelay(
            String relayAddress,
            String serverAddress
    ) {
        this.relayAddress = relayAddress;
        this.serverAddress = serverAddress;
    }

    public OSINetworkPacket relayClientToServer(
            OSINetworkPacket packet
    ) {
        if (!W118DhcpMessage.isDhcp(packet)) {
            return null;
        }

        OSINetworkPacket relayed =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        relayed.sourceIp = relayAddress;
        relayed.targetIp = serverAddress;
        relayed.sourcePort = 67;
        relayed.targetPort = 67;
        relayed.targetMac = "";
        relayed.payload.putString(
                "dhcp_giaddr",
                relayAddress
        );
        relayed.payload.putBoolean(
                "dhcp_relayed",
                true
        );

        return relayed;
    }

    public OSINetworkPacket relayServerToClient(
            OSINetworkPacket packet
    ) {
        if (!W118DhcpMessage.isDhcp(packet)) {
            return null;
        }

        OSINetworkPacket relayed =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        relayed.sourceIp = relayAddress;
        relayed.targetIp =
                W118DhcpMessage.BROADCAST_IP;
        relayed.targetMac =
                "FF:FF:FF:FF:FF:FF";
        relayed.sourcePort = 67;
        relayed.targetPort = 68;
        relayed.payload.putBoolean(
                "dhcp_relayed",
                true
        );

        return relayed;
    }
}
