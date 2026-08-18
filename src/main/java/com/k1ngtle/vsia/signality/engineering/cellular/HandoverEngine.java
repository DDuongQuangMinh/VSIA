package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.Collection;
import java.util.UUID;

public final class HandoverEngine {
    private final double hysteresisDb;

    public HandoverEngine(double hysteresisDb) {
        if (hysteresisDb < 0.0) throw new IllegalArgumentException("hysteresisDb");
        this.hysteresisDb = hysteresisDb;
    }

    public HandoverDecision evaluate(
            UUID servingCellId,
            double servingRsrpDbm,
            Collection<CellRecord> neighbours
    ) {
        CellRecord best = null;

        for (CellRecord candidate : neighbours) {
            if (candidate.baseStationId().equals(servingCellId)) continue;
            if (best == null || candidate.rsrpDbm() > best.rsrpDbm()) {
                best = candidate;
            }
        }

        if (best == null) return HandoverDecision.none();

        double margin = best.rsrpDbm() - servingRsrpDbm;
        if (margin > hysteresisDb) {
            return new HandoverDecision(true, best, margin);
        }

        return HandoverDecision.none();
    }
}
