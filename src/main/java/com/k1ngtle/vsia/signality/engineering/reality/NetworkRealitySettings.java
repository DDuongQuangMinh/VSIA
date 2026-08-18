package com.k1ngtle.vsia.signality.engineering.reality;

public final class NetworkRealitySettings {
    public static boolean ENABLE_MICROSECOND_OVERLAP =
            true;

    public static boolean ENABLE_CAPTURE_MODEL =
            true;

    public static double SIMULTANEOUS_CAPTURE_THRESHOLD_DB =
            10.0;

    public static double LOCKED_RECEIVER_CAPTURE_THRESHOLD_DB =
            6.0;

    public static long PREAMBLE_LOCK_MICROS =
            20L;

    private NetworkRealitySettings() {
    }
}
