package com.k1ngtle.vsia.phone.client;

public final class PhoneSystemSettings {
    public enum Wallpaper {
        AERO("Aero"),
        DUSK("Dusk"),
        GRAPHITE("Graphite");

        private final String displayName;

        Wallpaper(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private static boolean use24HourTime = true;
    private static boolean lowPowerMode;
    private static boolean screenTimeEnabled = true;
    private static long screenTicks;
    private static boolean showHomeSearch = true;
    private static Wallpaper wallpaper = Wallpaper.AERO;
    private static float brightness = 1.0F;

    private PhoneSystemSettings() {
    }

    public static boolean use24HourTime() {
        return use24HourTime;
    }

    public static void setUse24HourTime(boolean enabled) {
        use24HourTime = enabled;
    }

    public static boolean lowPowerMode() {
        return lowPowerMode;
    }

    public static void setLowPowerMode(boolean enabled) {
        lowPowerMode = enabled;
    }

    public static boolean screenTimeEnabled() {
        return screenTimeEnabled;
    }

    public static void setScreenTimeEnabled(boolean enabled) {
        screenTimeEnabled = enabled;
    }

    public static void onPhoneTick() {
        if (screenTimeEnabled) {
            screenTicks++;
        }
    }

    public static long screenTimeSeconds() {
        return screenTicks / 20L;
    }

    public static String formattedScreenTime() {
        long seconds = screenTimeSeconds();
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;

        if (hours > 0L) {
            return hours + "h " + minutes + "m";
        }

        if (minutes > 0L) {
            return minutes + "m";
        }

        return seconds + "s";
    }

    public static boolean showHomeSearch() {
        return showHomeSearch;
    }

    public static void setShowHomeSearch(boolean enabled) {
        showHomeSearch = enabled;
    }

    public static Wallpaper wallpaper() {
        return wallpaper;
    }

    public static void setWallpaper(Wallpaper value) {
        wallpaper = value == null
                ? Wallpaper.AERO
                : value;
    }

    public static float brightness() {
        return brightness;
    }

    public static void setBrightness(float value) {
        brightness = Math.max(
                0.25F,
                Math.min(1.0F, value)
        );
    }

    public static int brightnessOverlayColor() {
        float darkness = 1.0F - brightness;

        if (darkness <= 0.001F) {
            return 0;
        }

        int alpha = Math.round(
                darkness * 165.0F
        );

        return (alpha << 24);
    }

    public static void resetHomeScreenLayout() {
        showHomeSearch = true;
        wallpaper = Wallpaper.AERO;
    }

    public static void clearUsageData() {
        screenTicks = 0L;
    }

    public static void resetToDefaults() {
        use24HourTime = true;
        lowPowerMode = false;
        screenTimeEnabled = true;
        showHomeSearch = true;
        wallpaper = Wallpaper.AERO;
        brightness = 1.0F;
    }
}
