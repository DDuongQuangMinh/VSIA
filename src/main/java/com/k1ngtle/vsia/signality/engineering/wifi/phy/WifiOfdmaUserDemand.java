package com.k1ngtle.vsia.signality.engineering.wifi.phy;

public record WifiOfdmaUserDemand(
        String stationId,
        double weight,
        double snrDb
) {
    public WifiOfdmaUserDemand {
        if (stationId == null
                || stationId.isBlank()) {
            throw new IllegalArgumentException(
                    "stationId"
            );
        }

        weight =
                Math.max(
                        0.01,
                        weight
                );
    }
}
