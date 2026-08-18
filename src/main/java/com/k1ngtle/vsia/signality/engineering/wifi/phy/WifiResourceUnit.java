package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public enum WifiResourceUnit {
    RU_26(26, 24, 2),
    RU_52(52, 48, 4),
    RU_106(106, 102, 4),
    RU_242(242, 234, 8),
    RU_484(484, 468, 16),
    RU_996(996, 980, 16),
    RU_2X996(1992, 1960, 32),
    RU_4X996(3984, 3920, 64);

    private final int tones;
    private final int dataTones;
    private final int pilotTones;

    WifiResourceUnit(
            int tones,
            int dataTones,
            int pilotTones
    ) {
        this.tones =
                tones;
        this.dataTones =
                dataTones;
        this.pilotTones =
                pilotTones;
    }

    public int tones() {
        return tones;
    }

    public int dataTones() {
        return dataTones;
    }

    public int pilotTones() {
        return pilotTones;
    }
}
