package com.k1ngtle.vsia.signality.internet.server;

public enum ServerRackPtpProfile {
    DEFAULT,
    POWER,
    TELECOM;

    public static ServerRackPtpProfile byName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception exception) {
            return POWER;
        }
    }
}