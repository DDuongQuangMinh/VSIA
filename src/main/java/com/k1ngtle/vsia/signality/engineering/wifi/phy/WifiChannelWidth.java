package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public enum WifiChannelWidth {
    MHZ_20(20),
    MHZ_40(40),
    MHZ_80(80),
    MHZ_160(160),
    MHZ_320(320);

    private final int mhz;

    WifiChannelWidth(
            int mhz
    ) {
        this.mhz = mhz;
    }

    public int mhz() {
        return mhz;
    }

    public double hz() {
        return mhz
                * 1_000_000.0;
    }

    public static WifiChannelWidth nearest(
            double bandwidthHz
    ) {
        double mhz =
                bandwidthHz
                        / 1_000_000.0;

        WifiChannelWidth best =
                MHZ_20;

        double error =
                Double.POSITIVE_INFINITY;

        for (WifiChannelWidth width : values()) {
            double candidate =
                    Math.abs(
                            width.mhz
                                    - mhz
                    );

            if (candidate < error) {
                error =
                        candidate;
                best =
                        width;
            }
        }

        return best;
    }
}
