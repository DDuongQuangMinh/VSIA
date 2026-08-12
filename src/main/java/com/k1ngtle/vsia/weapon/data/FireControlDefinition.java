package com.k1ngtle.vsia.weapon.data;

import com.k1ngtle.vsia.weapon.state.FireMode;
import java.util.List;

public record FireControlDefinition(float roundsPerMinute, List<FireMode> fireModes,
                                    FireMode defaultMode, int burstSize) {
    public FireControlDefinition(float roundsPerMinute, List<FireMode> fireModes, FireMode defaultMode) {
        this(roundsPerMinute, fireModes, defaultMode, 3);
    }

    public FireControlDefinition {
        if (roundsPerMinute <= 0) throw new IllegalArgumentException("roundsPerMinute must be positive");
        fireModes = List.copyOf(fireModes);
        if (fireModes.isEmpty() || !fireModes.contains(defaultMode)) {
            throw new IllegalArgumentException("defaultMode must be in fireModes");
        }
        if (burstSize < 1) throw new IllegalArgumentException("burstSize must be positive");
    }

    public int ticksPerShot() {
        return Math.max(1, Math.round(1200.0F / roundsPerMinute));
    }
}
