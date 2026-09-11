package com.k1ngtle.vsia.signality.engineering.cellular;

import com.k1ngtle.vsia.signality.engineering.cellular.core.PduSession;
import com.k1ngtle.vsia.signality.engineering.cellular.nas.NasState;

public final class CellularAutomationController {
    private static final String CELLULAR_RECOVERY_RACH_RRC_NAS_V1 =
            "CELLULAR_RECOVERY_RACH_RRC_NAS_V1";

    private static final long SSB_INTERVAL_US = 100_000L;
    private static final long CELL_SEARCH_RETRY_US = 500_000L;
    private static final long ATTACH_DELAY_US = 150_000L;
    private static final long ATTACH_RETRY_US = 750_000L;
    private static final long RANDOM_ACCESS_RETRY_US = 300_000L;
    private static final long RRC_SETUP_RETRY_US = 400_000L;
    private static final long NAS_REGISTRATION_RETRY_US = 800_000L;
    private static final long PDU_RETRY_US = 1_000_000L;
    private static final long MEASUREMENT_INTERVAL_US = 500_000L;

    private static final int RANDOM_ACCESS_MAX_RETRIES = 4;
    private static final int RRC_SETUP_MAX_RETRIES = 3;
    private static final int NAS_REGISTRATION_MAX_RETRIES = 3;

    private long nextSsbMicros;
    private long nextSearchMicros;
    private long nextAttachMicros;
    private long nextRandomAccessMicros;
    private long nextRrcSetupMicros;
    private long nextNasRegistrationMicros;
    private long nextPduMicros;
    private long nextMeasurementMicros;

    private UeRanState observedUeState;
    private NasState observedNasState;
    private long stateEnteredMicros;

    private int randomAccessRetries;
    private int rrcSetupRetries;
    private int nasRegistrationRetries;

    private Action lastAction = Action.NONE;

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
                nextSsbMicros = now + SSB_INTERVAL_US;
                return remember(Action.BROADCAST_SYSTEM_INFORMATION);
            }
            return remember(Action.NONE);
        }

        if (mode != CellularMode.UE) {
            return remember(Action.NONE);
        }

        observeState(now, ueState, nasState);

        if (ueState == UeRanState.DETACHED) {
            if (now >= nextSearchMicros) {
                nextSearchMicros = now + CELL_SEARCH_RETRY_US;
                nextAttachMicros = now + ATTACH_DELAY_US;
                return remember(Action.START_CELL_SEARCH);
            }
            return remember(Action.NONE);
        }

        if (ueState == UeRanState.CELL_SEARCH) {
            if (hasDiscoveredCells && now >= nextAttachMicros) {
                nextAttachMicros = now + ATTACH_RETRY_US;
                return remember(Action.SELECT_AND_ATTACH);
            }

            if (!hasDiscoveredCells && now >= nextSearchMicros) {
                nextSearchMicros = now + CELL_SEARCH_RETRY_US;
                return remember(Action.START_CELL_SEARCH);
            }

            return remember(Action.NONE);
        }

        if (ueState == UeRanState.RANDOM_ACCESS) {
            if (now < nextRandomAccessMicros) {
                return remember(Action.NONE);
            }

            if (randomAccessRetries < RANDOM_ACCESS_MAX_RETRIES) {
                randomAccessRetries++;
                nextRandomAccessMicros = now + RANDOM_ACCESS_RETRY_US;
                return remember(Action.RETRY_RANDOM_ACCESS);
            }

            nextRandomAccessMicros = Long.MAX_VALUE;
            return remember(Action.RECOVER_CELL_SEARCH);
        }

        if (ueState == UeRanState.RRC_CONNECTING) {
            if (now < nextRrcSetupMicros) {
                return remember(Action.NONE);
            }

            if (rrcSetupRetries < RRC_SETUP_MAX_RETRIES) {
                rrcSetupRetries++;
                nextRrcSetupMicros = now + RRC_SETUP_RETRY_US;
                return remember(Action.RETRY_RRC_SETUP);
            }

            nextRrcSetupMicros = Long.MAX_VALUE;
            return remember(Action.RESTART_RANDOM_ACCESS);
        }

        if (registrationInProgress(ueState, nasState)) {
            if (now < nextNasRegistrationMicros) {
                return remember(Action.NONE);
            }

            if (nasRegistrationRetries < NAS_REGISTRATION_MAX_RETRIES) {
                nasRegistrationRetries++;
                nextNasRegistrationMicros = now + NAS_REGISTRATION_RETRY_US;
                return remember(Action.RETRY_NAS_REGISTRATION);
            }

            nextNasRegistrationMicros = Long.MAX_VALUE;
            return remember(Action.RECOVER_CELL_SEARCH);
        }

        if (ueState == UeRanState.REGISTERED
                && nasState == NasState.REGISTERED
                && (pduSession == null || !pduSession.active())) {
            if (now >= nextPduMicros) {
                nextPduMicros = now + PDU_RETRY_US;
                return remember(Action.REQUEST_PDU_SESSION);
            }
            return remember(Action.NONE);
        }

        if (ueState == UeRanState.REGISTERED
                && now >= nextMeasurementMicros) {
            nextMeasurementMicros = now + MEASUREMENT_INTERVAL_US;
            return remember(Action.SEND_MEASUREMENT_REPORT);
        }

        return remember(Action.NONE);
    }

    private void observeState(
            long now,
            UeRanState ueState,
            NasState nasState
    ) {
        UeRanState previousUe = observedUeState;
        NasState previousNas = observedNasState;

        boolean ueChanged = previousUe != ueState;
        boolean nasChanged = previousNas != nasState;

        if (!ueChanged && !nasChanged) {
            return;
        }

        boolean wasRegistering =
                registrationInProgress(previousUe, previousNas);

        boolean nowRegistering =
                registrationInProgress(ueState, nasState);

        observedUeState = ueState;
        observedNasState = nasState;
        stateEnteredMicros = now;

        if (ueState == UeRanState.DETACHED) {
            randomAccessRetries = 0;
            rrcSetupRetries = 0;
            nasRegistrationRetries = 0;
            nextSearchMicros = now;
            nextAttachMicros = now + ATTACH_DELAY_US;
        }

        if (ueState == UeRanState.CELL_SEARCH
                && previousUe != UeRanState.CELL_SEARCH) {
            randomAccessRetries = 0;
            rrcSetupRetries = 0;
            nasRegistrationRetries = 0;
            nextSearchMicros = now + CELL_SEARCH_RETRY_US;
            nextAttachMicros = now + ATTACH_DELAY_US;
        }

        if (ueState == UeRanState.RANDOM_ACCESS
                && previousUe != UeRanState.RANDOM_ACCESS) {
            randomAccessRetries = 0;
            nextRandomAccessMicros = now + RANDOM_ACCESS_RETRY_US;
        }

        if (ueState == UeRanState.RRC_CONNECTING
                && previousUe != UeRanState.RRC_CONNECTING) {
            rrcSetupRetries = 0;
            nextRrcSetupMicros = now + RRC_SETUP_RETRY_US;
        }

        if (nowRegistering && !wasRegistering) {
            nasRegistrationRetries = 0;
            nextNasRegistrationMicros = now + NAS_REGISTRATION_RETRY_US;
        }

        if (ueState == UeRanState.REGISTERED) {
            randomAccessRetries = 0;
            rrcSetupRetries = 0;
            nasRegistrationRetries = 0;
            nextPduMicros = now;
            nextMeasurementMicros = now + MEASUREMENT_INTERVAL_US;
        }
    }

    private static boolean registrationInProgress(
            UeRanState ueState,
            NasState nasState
    ) {
        if (ueState == null || nasState == null) {
            return false;
        }

        if (ueState == UeRanState.RRC_CONNECTED
                || ueState == UeRanState.REGISTERING) {
            return true;
        }

        return nasState == NasState.REGISTERING
                || nasState == NasState.AUTHENTICATING
                || nasState == NasState.SECURITY_MODE;
    }

    private Action remember(Action action) {
        if (action != Action.NONE) {
            lastAction = action;
        }
        return action;
    }

    public int randomAccessRetries() {
        return randomAccessRetries;
    }

    public int rrcSetupRetries() {
        return rrcSetupRetries;
    }

    public int nasRegistrationRetries() {
        return nasRegistrationRetries;
    }

    public Action lastAction() {
        return lastAction;
    }

    public long stateAgeMicros(long nowMicros) {
        if (observedUeState == null) {
            return 0L;
        }

        return Math.max(0L, nowMicros - stateEnteredMicros);
    }

    public void reset() {
        nextSsbMicros = 0L;
        nextSearchMicros = 0L;
        nextAttachMicros = 0L;
        nextRandomAccessMicros = 0L;
        nextRrcSetupMicros = 0L;
        nextNasRegistrationMicros = 0L;
        nextPduMicros = 0L;
        nextMeasurementMicros = 0L;

        observedUeState = null;
        observedNasState = null;
        stateEnteredMicros = 0L;

        randomAccessRetries = 0;
        rrcSetupRetries = 0;
        nasRegistrationRetries = 0;

        lastAction = Action.NONE;
    }

    public enum Action {
        NONE,
        BROADCAST_SYSTEM_INFORMATION,
        START_CELL_SEARCH,
        SELECT_AND_ATTACH,
        RETRY_RANDOM_ACCESS,
        RETRY_RRC_SETUP,
        RETRY_NAS_REGISTRATION,
        RESTART_RANDOM_ACCESS,
        RECOVER_CELL_SEARCH,
        REQUEST_PDU_SESSION,
        SEND_MEASUREMENT_REPORT
    }
}
