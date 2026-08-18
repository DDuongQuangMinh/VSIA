package com.k1ngtle.vsia.signality.engineering.wifi.arp;

public record ArpPacket(
        int hardwareType,
        int protocolType,
        int hardwareLength,
        int protocolLength,
        ArpOperation operation,
        String senderMac,
        String senderIp,
        String targetMac,
        String targetIp
) {
    public static final int ETHERNET_HARDWARE_TYPE =
            1;

    public static final int IPV4_PROTOCOL_TYPE =
            0x0800;

    public static final int ETHERNET_ADDRESS_BYTES =
            6;

    public static final int IPV4_ADDRESS_BYTES =
            4;

    public boolean ethernetIpv4() {
        return hardwareType
                == ETHERNET_HARDWARE_TYPE
                && protocolType
                == IPV4_PROTOCOL_TYPE
                && hardwareLength
                == ETHERNET_ADDRESS_BYTES
                && protocolLength
                == IPV4_ADDRESS_BYTES;
    }
}
