package com.k1ngtle.vsia.signality.engineering.channel;

public final class RfChannelSettings {
    private RfChannelSettings() {
    }

    public static boolean ENABLE_INTERFERENCE = true;
    public static boolean ENABLE_SPECTRAL_OVERLAP = true;
    public static boolean ENABLE_MATERIAL_ATTENUATION = true;
    public static boolean ENABLE_SHADOWING = true;
    public static boolean ENABLE_SMALL_SCALE_FADING = true;

    public static double SHADOWING_SIGMA_DB = 3.0;
    public static double MAX_ABS_SHADOWING_DB = 9.0;
    public static double MAX_ABS_FADING_DB = 12.0;

    public static int MATERIAL_RAY_MAX_SAMPLES = 192;
    public static double MATERIAL_RAY_MIN_STEP_BLOCKS = 0.50;

    public static int FADING_TIME_BIN_TICKS = 5;
}
