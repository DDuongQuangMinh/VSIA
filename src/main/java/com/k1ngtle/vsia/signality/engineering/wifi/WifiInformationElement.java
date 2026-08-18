package com.k1ngtle.vsia.signality.engineering.wifi;

public record WifiInformationElement(
        int id,
        byte[] data
) {
    public WifiInformationElement {
        if (id < 0 || id > 255) {
            throw new IllegalArgumentException(
                    "IE id must be 0..255"
            );
        }

        data =
                data == null
                        ? new byte[0]
                        : data.clone();

        if (data.length > 255) {
            throw new IllegalArgumentException(
                    "IE data length must be <= 255"
            );
        }
    }

    @Override
    public byte[] data() {
        return data.clone();
    }
}
