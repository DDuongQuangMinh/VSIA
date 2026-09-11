package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasState;

public final class CellularAutomationController {
    private static final long SSB_INTERVAL_US =
            100_000L;

    private static final long CELL_SEARCH_RETRY_US =
            500_000L;

    private static final long ATTACH_RETRY_US =
            750_000L;

    private static final long PDU_RETRY_US =
            1_000_000L;

    private static final long MEASUREMENT_INTERVAL_US =
            500_000L;

    private long nextSsbMicros;
    private long nextSearchMicros;
    private long nextAttachMicros;
    private long nextPduMicros;
    private long nextMeasurementMicros;

    public Action nextAction(
            long nowMicros,
            CellularMode mode,
            UeRanState ueState,
            NasState nasState,
            boolean hasDiscoveredCells,
            PduSession pduSession
    ) {
        long now = Math.max(0L, nowMicros);

        if (mode == CellularMode.BASE_STATION) {
            if (now >= nextSsbMicros) {
                nextSsbMicros =
                        now + SSB_INTERVAL_US;
                return Action.BROADCAST_SYSTEM_INFORMATION;
            }

            return Action.NONE;
        }

        if (mode != CellularMode.UE) {
            return Action.NONE;
        }

        if (ueState == UeRanState.DETACHED) {
            if (now >= nextSearchMicros) {
                nextSearchMicros =
                        now + CELL_SEARCH_RETRY_US;
                nextAttachMicros =
                        now + 150_000L;
                return Action.START_CELL_SEARCH;
            }

            return Action.NONE;
        }

        if (ueState == UeRanState.CELL_SEARCH) {
            if (hasDiscoveredCells
                    && now >= nextAttachMicros) {
                nextAttachMicros =
                        now + ATTACH_RETRY_US;
                return Action.SELECT_AND_ATTACH;
            }

            if (!hasDiscoveredCells
                    && now >= nextSearchMicros) {
                nextSearchMicros =
                        now + CELL_SEARCH_RETRY_US;
                return Action.START_CELL_SEARCH;
            }

            return Action.NONE;
        }

        if (ueState == UeRanState.REGISTERED
                && nasState == NasState.REGISTERED
                && (pduSession == null
                || !pduSession.active())) {
            if (now >= nextPduMicros) {
                nextPduMicros =
                        now + PDU_RETRY_US;
                return Action.REQUEST_PDU_SESSION;
            }

            return Action.NONE;
        }

        if (ueState == UeRanState.REGISTERED
                && now >= nextMeasurementMicros) {
            nextMeasurementMicros =
                    now + MEASUREMENT_INTERVAL_US;
            return Action.SEND_MEASUREMENT_REPORT;
        }

        return Action.NONE;
    }

    public void reset() {
        nextSsbMicros = 0L;
        nextSearchMicros = 0L;
        nextAttachMicros = 0L;
        nextPduMicros = 0L;
        nextMeasurementMicros = 0L;
    }

    public enum Action {
        NONE,
        BROADCAST_SYSTEM_INFORMATION,
        START_CELL_SEARCH,
        SELECT_AND_ATTACH,
        REQUEST_PDU_SESSION,
        SEND_MEASUREMENT_REPORT
    }
}
