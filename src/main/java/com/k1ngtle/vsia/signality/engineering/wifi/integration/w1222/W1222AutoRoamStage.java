package com.k1ngtle.vsia.signality.engineering.wifi.integration.w1222;

public enum W1222AutoRoamStage {
    SETUP,
    INITIAL_SCAN,
    CONNECT_AP1,
    BASELINE_HTTP,
    PREPARE_AUTO_ROAM,
    WAIT_BACKGROUND_SCAN,
    WAIT_AUTO_ROAM,
    OLD_AP_CLEANUP,
    POST_ROAM_DATA_ACK,
    POST_ROAM_HTTP,
    COOLDOWN_STABILITY,
    COMPLETE,
    FAILED
}
