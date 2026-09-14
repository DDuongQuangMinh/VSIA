package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;

public final class PhoneResetService {
    private PhoneResetService() {
    }

    public static void resetAllSettings() {
        PhoneSystemSettings.resetToDefaults();
        PhoneNotificationSettings.resetToDefaults();
        PhonePrivacyClientState.resetToDefaults();
        PhoneAccessibilityClientPreferences.resetToDefaults();
        PhoneLocaleSettings.resetToDefaults();
        PhoneSoftwareUpdateState.resetSettingsOnly();
        PhoneDeviceSettingsClientState.resetToDefaults();

        PhoneNetworkController.get().requestRefresh();
    }

    public static void resetNetworkSettings() {
        PhoneWifiClientPreferences.clearAll();
        PhoneDeviceSettingsClientState.resetNetworkSettings();

        PhoneNetworkController.get().requestRefresh();
    }

    public static void resetHomeScreenLayout() {
        PhoneSystemSettings.resetHomeScreenLayout();
    }

    public static void eraseLocalContentAndSettings() {
        resetAllSettings();
        resetNetworkSettings();

        PhoneNotificationManager.get().clearHistory();
        PhoneClockClientState.reset();
        PhoneSystemSettings.clearUsageData();
        PhonePersonalAppsState.clearAll();

        PhoneNetworkController.get().requestRefresh();
    }
}
