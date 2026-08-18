package com.k1ngtle.vsia.signality.engineering.wifi.ldpc;

public enum WifiLdpcCodewordLength {
    N_648(648),
    N_1296(1296),
    N_1944(1944);

    private final int bits;

    WifiLdpcCodewordLength(
            int bits
    ) {
        this.bits =
                bits;
    }

    public int bits() {
        return bits;
    }
}
