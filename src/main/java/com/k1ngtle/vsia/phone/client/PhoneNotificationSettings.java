package com.k1ngtle.vsia.phone.client;

public final class PhoneNotificationSettings {
    private static boolean messagesNotificationsEnabled = true;
    private static boolean showPreviews = true;
    private static boolean alertSoundEnabled = true;
    private static boolean hapticsEnabled = true;
    private static float alertVolume = 0.75F;

    private static boolean doNotDisturbEnabled;
    private static boolean allowMessagesInFocus;

    private PhoneNotificationSettings() {
    }

    public static boolean messagesNotificationsEnabled() {
        return messagesNotificationsEnabled;
    }

    public static void setMessagesNotificationsEnabled(boolean enabled) {
        messagesNotificationsEnabled = enabled;
    }

    public static boolean showPreviews() {
        return showPreviews;
    }

    public static void setShowPreviews(boolean enabled) {
        showPreviews = enabled;
    }

    public static boolean alertSoundEnabled() {
        return alertSoundEnabled;
    }

    public static void setAlertSoundEnabled(boolean enabled) {
        alertSoundEnabled = enabled;
    }

    public static boolean hapticsEnabled() {
        return hapticsEnabled;
    }

    public static void setHapticsEnabled(boolean enabled) {
        hapticsEnabled = enabled;
    }

    public static float alertVolume() {
        return alertVolume;
    }

    public static void setAlertVolume(float value) {
        alertVolume = Math.max(0.0F, Math.min(1.0F, value));
    }

    public static boolean doNotDisturbEnabled() {
        return doNotDisturbEnabled;
    }

    public static void setDoNotDisturbEnabled(boolean enabled) {
        doNotDisturbEnabled = enabled;
    }

    public static boolean allowMessagesInFocus() {
        return allowMessagesInFocus;
    }

    public static void setAllowMessagesInFocus(boolean enabled) {
        allowMessagesInFocus = enabled;
    }

    public static boolean messagesSilencedByFocus() {
        return doNotDisturbEnabled && !allowMessagesInFocus;
    }
}
