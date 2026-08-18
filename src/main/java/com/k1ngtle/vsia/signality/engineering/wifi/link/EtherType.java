package com.k1ngtle.vsia.signality.engineering.wifi.link;

public enum EtherType {
    IPV4(
            0x0800
    ),
    ARP(
            0x0806
    ),
    IPV6(
            0x86DD
    ),
    UNKNOWN(
            -1
    );

    private final int value;

    EtherType(
            int value
    ) {
        this.value =
                value;
    }

    public int value() {
        return value;
    }

    public static EtherType fromValue(
            int value
    ) {
        for (EtherType type
                : values()) {
            if (type.value
                    == value) {
                return type;
            }
        }

        return UNKNOWN;
    }
}
