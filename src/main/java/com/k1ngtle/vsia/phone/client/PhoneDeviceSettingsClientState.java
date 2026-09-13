package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkState;

public final class PhoneDeviceSettingsClientState {
    private static boolean airplaneMode;
    private static boolean bluetoothEnabled = true;
    private static boolean wifiEnabledInAirplanePreference;
    private static boolean wifiStateBeforeAirplane = true;
    private static boolean wifiExplicitlyChangedInAirplane;
    private static boolean wifiValueDuringAirplane;

    private PhoneDeviceSettingsClientState() {
    }

    public static boolean airplaneMode() {
        return airplaneMode;
    }

    public static boolean bluetoothEnabled() {
        return bluetoothEnabled;
    }

    public static void setBluetoothEnabled(boolean enabled) {
        bluetoothEnabled = enabled;
    }

    public static void toggleAirplaneMode() {
        setAirplaneMode(!airplaneMode);
    }

    public static void setAirplaneMode(boolean enabled) {
        if (airplaneMode == enabled) {
            return;
        }

        PhoneNetworkController controller = PhoneNetworkController.get();

        if (enabled) {
            wifiStateBeforeAirplane = PhoneNetworkState.get().getWifi().enabled();
            wifiExplicitlyChangedInAirplane = false;
            wifiValueDuringAirplane = wifiEnabledInAirplanePreference;
            airplaneMode = true;
            controller.setCellularRadioEnabled(false);
            controller.setWifiEnabled(wifiEnabledInAirplanePreference);
            controller.requestRefresh();
            return;
        }

        boolean desiredWifi = wifiExplicitlyChangedInAirplane
                ? wifiValueDuringAirplane
                : wifiStateBeforeAirplane;

        airplaneMode = false;
        controller.setCellularRadioEnabled(true);
        controller.setWifiEnabled(desiredWifi);
        controller.requestRefresh();
        wifiExplicitlyChangedInAirplane = false;
        wifiValueDuringAirplane = false;
    }

    public static void setWifiEnabledFromSettings(boolean enabled) {
        if (airplaneMode) {
            wifiExplicitlyChangedInAirplane = true;
            wifiValueDuringAirplane = enabled;
            wifiEnabledInAirplanePreference = enabled;
        }
        PhoneNetworkController.get().setWifiEnabled(enabled);
    }

    public static void resetNetworkSettings() {
        PhoneNetworkController controller =
                PhoneNetworkController.get();

        airplaneMode = false;
        bluetoothEnabled = true;
        wifiEnabledInAirplanePreference = false;
        wifiStateBeforeAirplane = true;
        wifiExplicitlyChangedInAirplane = false;
        wifiValueDuringAirplane = false;

        controller.setCellularRadioEnabled(true);
        controller.setWifiEnabled(false);
        controller.setWifiEnabled(true);
        controller.requestRefresh();
    }

    public static void resetToDefaults() {
        resetNetworkSettings();
    }
}
