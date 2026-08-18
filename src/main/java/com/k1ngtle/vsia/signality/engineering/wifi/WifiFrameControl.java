package com.k1ngtle.vsia.signality.engineering.wifi;

public record WifiFrameControl(
        int raw
) {
    public WifiFrameControl {
        raw &=
                0xFFFF;
    }

    public int protocolVersion() {
        return raw
                & 0x03;
    }

    public int typeBits() {
        return (
                raw >>> 2
        )
                & 0x03;
    }

    public int subtypeBits() {
        return (
                raw >>> 4
        )
                & 0x0F;
    }

    public boolean toDs() {
        return bit(
                8
        );
    }

    public boolean fromDs() {
        return bit(
                9
        );
    }

    public boolean moreFragments() {
        return bit(
                10
        );
    }

    public boolean retry() {
        return bit(
                11
        );
    }

    public boolean powerManagement() {
        return bit(
                12
        );
    }

    public boolean moreData() {
        return bit(
                13
        );
    }

    public boolean protectedFrame() {
        return bit(
                14
        );
    }

    public boolean order() {
        return bit(
                15
        );
    }

    public WifiFrameType type() {
        return WifiFrameType.fromFrameControl(
                raw
        );
    }

    public static int build(
            int protocolVersion,
            WifiFrameType type,
            int subtype,
            boolean toDs,
            boolean fromDs,
            boolean moreFragments,
            boolean retry,
            boolean powerManagement,
            boolean moreData,
            boolean protectedFrame,
            boolean order
    ) {
        int value =
                protocolVersion
                        & 0x03;

        value |=
                (
                        type.bits()
                                & 0x03
                )
                        << 2;

        value |=
                (
                        subtype
                                & 0x0F
                )
                        << 4;

        value =
                setBit(
                        value,
                        8,
                        toDs
                );

        value =
                setBit(
                        value,
                        9,
                        fromDs
                );

        value =
                setBit(
                        value,
                        10,
                        moreFragments
                );

        value =
                setBit(
                        value,
                        11,
                        retry
                );

        value =
                setBit(
                        value,
                        12,
                        powerManagement
                );

        value =
                setBit(
                        value,
                        13,
                        moreData
                );

        value =
                setBit(
                        value,
                        14,
                        protectedFrame
                );

        value =
                setBit(
                        value,
                        15,
                        order
                );

        return value
                & 0xFFFF;
    }

    private boolean bit(
            int index
    ) {
        return (
                raw
                        & (
                        1 << index
                )
        )
                != 0;
    }

    private static int setBit(
            int value,
            int bit,
            boolean enabled
    ) {
        if (enabled) {
            return value
                    | (
                    1 << bit
            );
        }

        return value
                & ~(
                1 << bit
        );
    }
}
