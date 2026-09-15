package com.k1ngtle.vsia.signality.radar.iff;

public enum IffTransponderState {
    OFF,
    STANDBY,
    NORMAL,
    EMERGENCY;

    public boolean repliesToInterrogation() {
        return this == NORMAL || this == EMERGENCY;
    }
}
