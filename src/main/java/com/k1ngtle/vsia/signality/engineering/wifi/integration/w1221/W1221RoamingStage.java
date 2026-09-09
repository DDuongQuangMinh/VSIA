package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1221;

public enum W1221RoamingStage {
    SETUP,
    INITIAL_SCAN,
    CONNECT_AP1,
    BASELINE_HTTP,
    ROAM_SCAN,
    ROAM_CONNECTING,
    POST_ROAM_DATA_ACK,
    POST_ROAM_HTTP,
    COMPLETE,
    FAILED
}
