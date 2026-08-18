package com.k1ngtle.vsia.signality.engineering.wifi.baseband;

public record LegacySignalField(
        LegacyOfdmRateProfile rate,
        int lengthBytes
) {
    public LegacySignalField {
        if (rate == null) {
            throw new IllegalArgumentException(
                    "rate"
            );
        }

        if (lengthBytes < 0
                || lengthBytes > 4095) {
            throw new IllegalArgumentException(
                    "lengthBytes must be 0..4095"
            );
        }
    }
}
