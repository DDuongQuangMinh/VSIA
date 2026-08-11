package com.k1ngtle.vsia.weapon;

public enum FireMode {
    SINGLE,
    BURST,
    AUTOMATIC;

    /**
     * Cycles to the next fire mode a gun supports, wrapping around.
     * Used when the player presses the fire-mode-select key.
     */
    public static FireMode next(FireMode current, FireMode[] supported) {
        if (supported.length <= 1) return supported[0];
        int idx = -1;
        for (int i = 0; i < supported.length; i++) {
            if (supported[i] == current) {
                idx = i;
                break;
            }
        }
        return supported[(idx + 1) % supported.length];
    }
}