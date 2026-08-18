package com.k1ngtle.vsia.signality.engineering.reality;

public final class ReceiverCaptureModel {
    public static final double DEFAULT_CAPTURE_THRESHOLD_DB =
            10.0;

    private ReceiverCaptureModel() {
    }

    public static boolean desiredCanCapture(
            double desiredPowerWatts,
            double strongestInterfererPowerWatts,
            double thresholdDb
    ) {
        if (strongestInterfererPowerWatts <= 0.0) {
            return true;
        }

        if (desiredPowerWatts <= 0.0) {
            return false;
        }

        double marginDb =
                10.0
                        * Math.log10(
                        desiredPowerWatts
                                / strongestInterfererPowerWatts
                );

        return marginDb
                >= thresholdDb;
    }

    public static double captureMarginDb(
            double desiredPowerWatts,
            double strongestInterfererPowerWatts
    ) {
        if (strongestInterfererPowerWatts <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        if (desiredPowerWatts <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                desiredPowerWatts
                        / strongestInterfererPowerWatts
        );
    }
}
