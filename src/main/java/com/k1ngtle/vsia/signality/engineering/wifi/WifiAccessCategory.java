package com.k1ngtle.vsia.signality.engineering.wifi;

public enum WifiAccessCategory {
    VOICE(2, 3, 7, 1504),
    VIDEO(2, 7, 15, 3008),
    BEST_EFFORT(3, 15, 1023, 0),
    BACKGROUND(7, 15, 1023, 0);

    private final int aifsn;
    private final int cwMin;
    private final int cwMax;
    private final int txopMicroseconds;

    WifiAccessCategory(
            int aifsn,
            int cwMin,
            int cwMax,
            int txopMicroseconds
    ) {
        this.aifsn = aifsn;
        this.cwMin = cwMin;
        this.cwMax = cwMax;
        this.txopMicroseconds = txopMicroseconds;
    }

    public int aifsn() {
        return aifsn;
    }

    public int cwMin() {
        return cwMin;
    }

    public int cwMax() {
        return cwMax;
    }

    public int txopMicroseconds() {
        return txopMicroseconds;
    }
}
