package com.k1ngtle.vsia.cockpit.display.telemetry;

public final class F35TelemetryRegistry {
    private static volatile AircraftTelemetryProvider provider =
            new VsShipTelemetryProvider();

    private F35TelemetryRegistry() {
    }

    public static AircraftTelemetryProvider provider() {
        return provider;
    }

    public static void setProvider(
            AircraftTelemetryProvider newProvider
    ) {
        provider =
                newProvider == null
                        ? new VsShipTelemetryProvider()
                        : newProvider;
    }

    public static void reset() {
        provider =
                new VsShipTelemetryProvider();
    }
}
