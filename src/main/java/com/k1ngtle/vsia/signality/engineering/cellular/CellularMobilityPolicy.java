package com.k1ngtle.vsia.signality.engineering.cellular;

import java.util.UUID;

public final class CellularMobilityPolicy {
    private final double hysteresisDb;
    private final long timeToTriggerMillis;

    private UUID pendingTarget;
    private long pendingSinceMillis = -1L;

    public CellularMobilityPolicy(
            double hysteresisDb,
            long timeToTriggerMillis
    ) {
        this.hysteresisDb = Math.max(0.0, hysteresisDb);
        this.timeToTriggerMillis = Math.max(0L, timeToTriggerMillis);
    }

    public Decision evaluate(
            UUID servingCell,
            double servingRsrpDbm,
            UUID candidateCell,
            double candidateRsrpDbm,
            long nowMillis
    ) {
        if (servingCell == null
                || candidateCell == null
                || servingCell.equals(candidateCell)
                || !Double.isFinite(servingRsrpDbm)
                || !Double.isFinite(candidateRsrpDbm)) {
            clear();
            return Decision.no("invalid measurement");
        }

        double marginDb =
                candidateRsrpDbm
                        - servingRsrpDbm;

        if (marginDb < hysteresisDb) {
            clear();
            return Decision.no("A3 margin below hysteresis");
        }

        if (!candidateCell.equals(pendingTarget)) {
            pendingTarget = candidateCell;
            pendingSinceMillis = nowMillis;

            if (timeToTriggerMillis == 0L) {
                Decision decision = Decision.yes(
                        candidateCell,
                        marginDb,
                        0L
                );
                clear();
                return decision;
            }

            return Decision.no("A3 time-to-trigger started");
        }

        long elapsed =
                Math.max(
                        0L,
                        nowMillis - pendingSinceMillis
                );

        if (elapsed < timeToTriggerMillis) {
            return Decision.no(
                    "A3 time-to-trigger pending "
                            + elapsed
                            + "/"
                            + timeToTriggerMillis
                            + " ms"
            );
        }

        Decision decision = Decision.yes(
                candidateCell,
                marginDb,
                elapsed
        );

        clear();
        return decision;
    }

    public void clear() {
        pendingTarget = null;
        pendingSinceMillis = -1L;
    }

    public record Decision(
            boolean handover,
            UUID targetCell,
            double marginDb,
            long timeAboveThresholdMillis,
            String reason
    ) {
        public static Decision no(String reason) {
            return new Decision(
                    false,
                    null,
                    0.0,
                    0L,
                    reason == null ? "" : reason
            );
        }

        public static Decision yes(
                UUID target,
                double marginDb,
                long elapsed
        ) {
            return new Decision(
                    true,
                    target,
                    marginDb,
                    elapsed,
                    "3GPP-style A3 hysteresis + time-to-trigger satisfied"
            );
        }
    }
}
