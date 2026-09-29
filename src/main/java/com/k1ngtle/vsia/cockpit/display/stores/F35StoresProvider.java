package com.k1ngtle.vsia.cockpit.display.stores;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;

public interface F35StoresProvider {
    F35StoresSnapshot capture(
            F35CockpitSeatBlockEntity cockpit
    );
}
