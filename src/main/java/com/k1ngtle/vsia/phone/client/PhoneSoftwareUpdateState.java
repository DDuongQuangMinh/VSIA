package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.network.PhoneNetworkState;

public final class PhoneSoftwareUpdateState {
    public enum CheckState {
        IDLE,
        CHECKING,
        AVAILABLE,
        UP_TO_DATE,
        DOWNLOADING,
        READY_TO_INSTALL,
        INSTALLING,
        ERROR
    }

    private static final String LATEST_VERSION = "26.0.1";

    private static String currentVersion = "26.0";
    private static boolean automaticallyDownload = true;
    private static boolean automaticallyInstall = true;

    private static CheckState state = CheckState.IDLE;
    private static long stateStartedAt;
    private static String errorMessage = "";

    private PhoneSoftwareUpdateState() {
    }

    public static String currentVersion() {
        return currentVersion;
    }

    public static String latestVersion() {
        return LATEST_VERSION;
    }

    public static boolean automaticallyDownload() {
        return automaticallyDownload;
    }

    public static void setAutomaticallyDownload(boolean enabled) {
        automaticallyDownload = enabled;

        if (!enabled) {
            automaticallyInstall = false;
        }
    }

    public static boolean automaticallyInstall() {
        return automaticallyInstall;
    }

    public static void setAutomaticallyInstall(boolean enabled) {
        automaticallyInstall = enabled;

        if (enabled) {
            automaticallyDownload = true;
        }
    }

    public static CheckState state() {
        tick();
        return state;
    }

    public static String errorMessage() {
        return errorMessage;
    }

    public static boolean updateAvailable() {
        return !currentVersion.equals(LATEST_VERSION);
    }

    public static void checkForUpdate() {
        if (!networkAvailable()) {
            state = CheckState.ERROR;
            errorMessage = "Unable to Check for Update";
            stateStartedAt = System.currentTimeMillis();
            return;
        }

        state = CheckState.CHECKING;
        errorMessage = "";
        stateStartedAt = System.currentTimeMillis();
    }

    public static void downloadUpdate() {
        if (!updateAvailable()) {
            state = CheckState.UP_TO_DATE;
            return;
        }

        if (!networkAvailable()) {
            state = CheckState.ERROR;
            errorMessage = "Internet connection required";
            return;
        }

        state = CheckState.DOWNLOADING;
        stateStartedAt = System.currentTimeMillis();
    }

    public static void installUpdate() {
        if (!updateAvailable()) {
            state = CheckState.UP_TO_DATE;
            return;
        }

        if (PhoneNetworkState.get().getBatteryPercent() < 20) {
            state = CheckState.ERROR;
            errorMessage = "Battery must be at least 20%";
            return;
        }

        state = CheckState.INSTALLING;
        stateStartedAt = System.currentTimeMillis();
    }

    public static void tick() {
        long elapsed = System.currentTimeMillis() - stateStartedAt;

        if (state == CheckState.CHECKING
                && elapsed >= 900L) {
            state = updateAvailable()
                    ? CheckState.AVAILABLE
                    : CheckState.UP_TO_DATE;

            if (state == CheckState.AVAILABLE
                    && automaticallyDownload) {
                downloadUpdate();
            }
        }

        if (state == CheckState.DOWNLOADING
                && elapsed >= 1500L) {
            state = CheckState.READY_TO_INSTALL;

            if (automaticallyInstall) {
                installUpdate();
            }
        }

        if (state == CheckState.INSTALLING
                && elapsed >= 1600L) {
            currentVersion = LATEST_VERSION;
            state = CheckState.UP_TO_DATE;
        }
    }

    public static void resetSettingsOnly() {
        automaticallyDownload = true;
        automaticallyInstall = true;
        state = CheckState.IDLE;
        errorMessage = "";
    }

    private static boolean networkAvailable() {
        PhoneNetworkState network = PhoneNetworkState.get();

        return network.isWifiUsable()
                || network.isCellularUsable();
    }
}
