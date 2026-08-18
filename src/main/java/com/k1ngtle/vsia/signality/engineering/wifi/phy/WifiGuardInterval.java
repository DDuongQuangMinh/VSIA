package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public enum WifiGuardInterval {
    GI_0_4_US(0.4),
    GI_0_8_US(0.8),
    GI_1_6_US(1.6),
    GI_3_2_US(3.2);

    private final double microseconds;

    WifiGuardInterval(
            double microseconds
    ) {
        this.microseconds =
                microseconds;
    }

    public double microseconds() {
        return microseconds;
    }

    public double seconds() {
        return microseconds
                * 1.0E-6;
    }
}
