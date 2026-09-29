package com.k1ngtle.vsia.cockpit.display.stores;

public final class F35StoresRegistry {
    private static volatile F35StoresProvider provider =
            new DebugStoresProvider();

    private F35StoresRegistry() {
    }

    public static F35StoresProvider provider() {
        return provider;
    }

    public static void setProvider(
            F35StoresProvider newProvider
    ) {
        provider =
                newProvider == null
                        ? new DebugStoresProvider()
                        : newProvider;
    }

    public static void reset() {
        provider =
                new DebugStoresProvider();
    }
}
