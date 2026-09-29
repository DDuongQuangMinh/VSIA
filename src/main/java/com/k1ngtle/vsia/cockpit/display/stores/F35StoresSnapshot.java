package com.k1ngtle.vsia.cockpit.display.stores;

import java.util.List;

public record F35StoresSnapshot(
        double fuelPercent,
        double fuelKg,
        String gunLabel,
        int gunRounds,
        List<Station> stations
) {
    public F35StoresSnapshot {
        stations =
                List.copyOf(
                        stations
                );
    }

    public record Station(
            int index,
            String label,
            int count,
            boolean selected
    ) {
    }
}
