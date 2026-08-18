package com.k1ngtle.vsia.signality.engineering.wifi.link;

public record LlcSnapFrame(
        int dsap,
        int ssap,
        int control,
        int oui,
        int etherType,
        byte[] payload
) {
    public LlcSnapFrame {
        payload =
                payload == null
                        ? new byte[0]
                        : payload.clone();
    }

    @Override
    public byte[] payload() {
        return payload.clone();
    }

    public EtherType etherTypeKind() {
        return EtherType.fromValue(
                etherType
        );
    }

    public boolean isRfc1042Snap() {
        return dsap == 0xAA
                && ssap == 0xAA
                && control == 0x03
                && oui == 0x000000;
    }
}
