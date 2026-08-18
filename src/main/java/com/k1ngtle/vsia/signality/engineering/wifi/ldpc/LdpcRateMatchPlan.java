package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public record LdpcRateMatchPlan(
        int motherCodewordBits,
        int informationBits,
        int shortenedBits,
        int puncturedBits,
        int transmittedBits
) {
    public LdpcRateMatchPlan {
        if (motherCodewordBits < 1
                || informationBits < 1
                || shortenedBits < 0
                || puncturedBits < 0
                || transmittedBits < 1) {
            throw new IllegalArgumentException(
                    "invalid LDPC rate-match plan"
            );
        }
    }
}
