package com.k1ngtle.vsia.cockpit.display.stores;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import java.util.List;

public final class DebugStoresProvider
        implements F35StoresProvider {
    @Override
    public F35StoresSnapshot capture(
            F35CockpitSeatBlockEntity cockpit
    ) {
        return new F35StoresSnapshot(
                82.0,
                5300.0,
                "GAU",
                180,
                List.of(
                        new F35StoresSnapshot.Station(
                                1,
                                "AIM",
                                1,
                                false
                        ),
                        new F35StoresSnapshot.Station(
                                2,
                                "GBU",
                                1,
                                false
                        ),
                        new F35StoresSnapshot.Station(
                                3,
                                "AIM",
                                1,
                                true
                        ),
                        new F35StoresSnapshot.Station(
                                4,
                                "GBU",
                                1,
                                false
                        ),
                        new F35StoresSnapshot.Station(
                                5,
                                "AIM",
                                1,
                                false
                        ),
                        new F35StoresSnapshot.Station(
                                6,
                                "AIM",
                                1,
                                false
                        )
                )
        );
    }
}
