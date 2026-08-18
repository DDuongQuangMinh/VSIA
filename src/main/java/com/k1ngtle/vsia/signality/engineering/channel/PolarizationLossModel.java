package com.k1ngtle.vsia.signality.engineering.channel;

public final class PolarizationLossModel {
    private PolarizationLossModel() {
    }

    public static double mismatchLossDb(
            RfPolarization tx,
            RfPolarization rx
    ) {
        if (tx == null
                || rx == null
                || tx == RfPolarization.UNKNOWN
                || rx == RfPolarization.UNKNOWN
                || tx == RfPolarization.DUAL
                || rx == RfPolarization.DUAL) {
            return 0.0;
        }

        if (tx == rx) {
            return 0.0;
        }

        boolean txLinear =
                tx == RfPolarization.VERTICAL
                        || tx == RfPolarization.HORIZONTAL;

        boolean rxLinear =
                rx == RfPolarization.VERTICAL
                        || rx == RfPolarization.HORIZONTAL;

        boolean txCircular =
                tx == RfPolarization.RHCP
                        || tx == RfPolarization.LHCP;

        boolean rxCircular =
                rx == RfPolarization.RHCP
                        || rx == RfPolarization.LHCP;

        if (txLinear
                && rxLinear) {
            return 20.0;
        }

        if (txCircular
                && rxCircular) {
            return 20.0;
        }

        if ((txLinear && rxCircular)
                || (txCircular && rxLinear)) {
            return 3.0;
        }

        return 0.0;
    }
}
