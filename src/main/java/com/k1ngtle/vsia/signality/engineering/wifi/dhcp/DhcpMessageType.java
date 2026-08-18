package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

public enum DhcpMessageType {
    DISCOVER(1),
    OFFER(2),
    REQUEST(3),
    DECLINE(4),
    ACK(5),
    NAK(6),
    RELEASE(7),
    INFORM(8);

    private final int code;

    DhcpMessageType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static DhcpMessageType fromCode(int code) {
        for (DhcpMessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported DHCP message type " + code
        );
    }
}
