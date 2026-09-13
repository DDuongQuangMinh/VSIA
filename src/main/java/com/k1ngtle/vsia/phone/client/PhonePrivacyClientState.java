package com.k1ngtle.vsia.phone.client;

public final class PhonePrivacyClientState {
    private static boolean localNetworkAllowed = true;

    private PhonePrivacyClientState() {
    }

    public static boolean localNetworkAllowed() {
        return localNetworkAllowed;
    }

    public static void setLocalNetworkAllowed(boolean allowed) {
        localNetworkAllowed = allowed;
    }
}
