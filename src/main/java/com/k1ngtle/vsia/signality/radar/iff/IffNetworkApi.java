package com.k1ngtle.vsia.signality.radar.iff;

public final class IffNetworkApi {
    private IffNetworkApi() {
    }

    public static void setNetworkKey(String networkId, String key) {
        IffNetworkKeyRegistry.setKey(networkId, key);
    }

    public static void resetNetworkKey(String networkId) {
        IffNetworkKeyRegistry.resetKey(networkId);
    }

    public static String defaultTestKey() {
        return IffNetworkKeyRegistry.DEFAULT_TEST_KEY;
    }
}
