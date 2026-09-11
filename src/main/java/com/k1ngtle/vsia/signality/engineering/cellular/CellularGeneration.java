package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.Locale;

public enum CellularGeneration {
    G1_ANALOG("1G", false, false, true),
    G2_GSM("2G", true, true, true),
    G3_UMTS("3G", true, true, true),
    G4_LTE("4G LTE", true, true, true),
    G5_NR("5G NR", true, true, true);

    private final String displayName;
    private final boolean packetData;
    private final boolean sms;
    private final boolean mobility;

    CellularGeneration(
            String displayName,
            boolean packetData,
            boolean sms,
            boolean mobility
    ) {
        this.displayName = displayName;
        this.packetData = packetData;
        this.sms = sms;
        this.mobility = mobility;
    }

    public String displayName() {
        return displayName;
    }

    public boolean packetData() {
        return packetData;
    }

    public boolean sms() {
        return sms;
    }

    public boolean mobility() {
        return mobility;
    }

    public static CellularGeneration fromProtocol(String protocol) {
        String value = protocol == null
                ? ""
                : protocol.toLowerCase(Locale.ROOT);

        if (value.contains("5g") || value.contains("nr")) {
            return G5_NR;
        }
        if (value.contains("lte") || value.contains("4g")) {
            return G4_LTE;
        }
        if (value.contains("umts") || value.contains("wcdma") || value.contains("3g")) {
            return G3_UMTS;
        }
        if (value.contains("gsm") || value.contains("2g")) {
            return G2_GSM;
        }
        return G1_ANALOG;
    }
}
