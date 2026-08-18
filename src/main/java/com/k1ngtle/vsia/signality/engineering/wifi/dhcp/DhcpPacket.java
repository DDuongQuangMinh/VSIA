package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

import java.util.Map;

public record DhcpPacket(
        int op,
        int hardwareType,
        int hardwareLength,
        int hops,
        long transactionId,
        int seconds,
        int flags,
        String clientIp,
        String yourIp,
        String serverIp,
        String relayIp,
        String clientMac,
        Map<Integer, byte[]> options
) {
    public static final int BOOTREQUEST = 1;
    public static final int BOOTREPLY = 2;

    public static final int HTYPE_ETHERNET = 1;
    public static final int HLEN_ETHERNET = 6;

    public static final int BROADCAST_FLAG = 0x8000;

    public DhcpPacket {
        options = DhcpCodec.copyOptions(options);
    }

    @Override
    public Map<Integer, byte[]> options() {
        return DhcpCodec.copyOptions(options);
    }

    public DhcpMessageType messageType() {
        byte[] value = options.get(DhcpOption.MESSAGE_TYPE);

        if (value == null || value.length != 1) {
            throw new IllegalArgumentException(
                    "DHCP message type option missing"
            );
        }

        return DhcpMessageType.fromCode(value[0] & 0xFF);
    }

    public String optionIpv4(
            int code,
            String fallback
    ) {
        byte[] value = options.get(code);

        if (value == null || value.length != 4) {
            return fallback;
        }

        return DhcpCodec.formatIpv4(value, 0);
    }

    public long optionUnsigned32(
            int code,
            long fallback
    ) {
        byte[] value = options.get(code);

        if (value == null || value.length != 4) {
            return fallback;
        }

        return DhcpCodec.read32(value, 0);
    }
}
