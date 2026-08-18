package com.k1ngtle.vsia.signality.engineering.wifi;

public enum WifiFrameType {
    MANAGEMENT(0),
    CONTROL(1),
    DATA(2),
    EXTENSION(3);

    private final int bits;

    WifiFrameType(
            int bits
    ) {
        this.bits = bits;
    }

    public int bits() {
        return bits;
    }

    public static WifiFrameType fromFrameControl(
            int frameControl
    ) {
        int value =
                (frameControl >>> 2)
                        & 0x03;

        for (WifiFrameType type : values()) {
            if (type.bits == value) {
                return type;
            }
        }

        return EXTENSION;
    }
}
