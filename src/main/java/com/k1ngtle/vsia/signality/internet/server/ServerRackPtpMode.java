package com.k1ngtle.vsia.signality.internet.server;

public enum ServerRackPtpMode {
    DISABLED,
    GRANDMASTER,
    CLIENT;

    public static ServerRackPtpMode byName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (Exception exception) {
            return DISABLED;
        }
    }
}