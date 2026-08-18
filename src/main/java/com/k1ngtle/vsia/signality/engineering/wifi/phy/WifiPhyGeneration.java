package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public enum WifiPhyGeneration {
    LEGACY_OFDM,
    HT,
    VHT,
    HE,
    EHT;

    public static WifiPhyGeneration fromProtocol(
            String protocol
    ) {
        String value =
                protocol == null
                        ? ""
                        : protocol.toLowerCase();

        if (value.contains("80211be")
                || value.contains("802.11be")) {
            return EHT;
        }

        if (value.contains("80211ax")
                || value.contains("802.11ax")) {
            return HE;
        }

        if (value.contains("80211ac")
                || value.contains("802.11ac")) {
            return VHT;
        }

        if (value.contains("80211n")
                || value.contains("802.11n")) {
            return HT;
        }

        return LEGACY_OFDM;
    }
}
