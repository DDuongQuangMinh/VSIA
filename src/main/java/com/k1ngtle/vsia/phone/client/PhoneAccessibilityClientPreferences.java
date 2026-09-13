package com.k1ngtle.vsia.phone.client;

public final class PhoneAccessibilityClientPreferences {
    private static boolean boldText = false;
    private static boolean largerText = true;
    private static boolean buttonShapes = false;
    private static boolean onOffLabels = false;
    private static boolean reduceTransparency = false;
    private static boolean increaseContrast = false;
    private static boolean differentiateWithoutColor = false;
    private static boolean preferHorizontalText = false;

    private PhoneAccessibilityClientPreferences() {
    }

    public static boolean boldText() {
        return boldText;
    }

    public static void toggleBoldText() {
        boldText = !boldText;
    }

    public static boolean largerText() {
        return largerText;
    }

    public static void toggleLargerText() {
        largerText = !largerText;
    }

    public static boolean buttonShapes() {
        return buttonShapes;
    }

    public static void toggleButtonShapes() {
        buttonShapes = !buttonShapes;
    }

    public static boolean onOffLabels() {
        return onOffLabels;
    }

    public static void toggleOnOffLabels() {
        onOffLabels = !onOffLabels;
    }

    public static boolean reduceTransparency() {
        return reduceTransparency;
    }

    public static void toggleReduceTransparency() {
        reduceTransparency = !reduceTransparency;
    }

    public static boolean increaseContrast() {
        return increaseContrast;
    }

    public static void toggleIncreaseContrast() {
        increaseContrast = !increaseContrast;
    }

    public static boolean differentiateWithoutColor() {
        return differentiateWithoutColor;
    }

    public static void toggleDifferentiateWithoutColor() {
        differentiateWithoutColor = !differentiateWithoutColor;
    }

    public static boolean preferHorizontalText() {
        return preferHorizontalText;
    }

    public static void togglePreferHorizontalText() {
        preferHorizontalText = !preferHorizontalText;
    }

    public static void resetToDefaults() {
        boldText = false;
        largerText = true;
        buttonShapes = false;
        onOffLabels = false;
        reduceTransparency = false;
        increaseContrast = false;
        differentiateWithoutColor = false;
        preferHorizontalText = false;
    }
}
