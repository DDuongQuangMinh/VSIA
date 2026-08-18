package com.k1ngtle.vsia.signality.engineering.wifi;

public record WifiMacTimingProfile(
        int slotTimeUs,
        int sifsUs,
        int rtsThresholdBytes,
        int ackTimeoutUs,
        int ctsTimeoutUs
) {
    public static WifiMacTimingProfile dsssLegacy() {
        return new WifiMacTimingProfile(
                20,
                10,
                512,
                120,
                120
        );
    }

    public static WifiMacTimingProfile ofdmDefault() {
        return new WifiMacTimingProfile(
                9,
                16,
                512,
                60,
                60
        );
    }

    public static WifiMacTimingProfile forProtocol(
            String protocol
    ) {
        String value =
                protocol == null
                        ? ""
                        : protocol.toLowerCase();

        if (value.contains(
                "80211b"
        )
                || value.contains(
                "802.11b"
        )) {
            return dsssLegacy();
        }

        return ofdmDefault();
    }

    public int aifsUs(
            WifiAccessCategory category
    ) {
        return sifsUs
                + category.aifsn()
                * slotTimeUs;
    }

    public int difsUs() {
        return sifsUs
                + 2
                * slotTimeUs;
    }
}
