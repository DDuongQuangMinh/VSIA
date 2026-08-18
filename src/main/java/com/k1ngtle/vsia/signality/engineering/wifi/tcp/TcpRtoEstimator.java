package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

public final class TcpRtoEstimator {
    public static final double ALPHA =
            1.0 / 8.0;

    public static final double BETA =
            1.0 / 4.0;

    public static final double K =
            4.0;

    public static final double INITIAL_RTO_SECONDS =
            1.0;

    public static final double MIN_RTO_SECONDS =
            1.0;

    public static final double MAX_RTO_SECONDS =
            60.0;

    private final double clockGranularitySeconds;

    private boolean initialized;

    private double srttSeconds;

    private double rttvarSeconds;

    private double rtoSeconds =
            INITIAL_RTO_SECONDS;

    public TcpRtoEstimator() {
        this(
                0.001
        );
    }

    public TcpRtoEstimator(
            double clockGranularitySeconds
    ) {
        if (!Double.isFinite(
                clockGranularitySeconds
        )
                || clockGranularitySeconds <= 0.0) {
            throw new IllegalArgumentException(
                    "clockGranularitySeconds"
            );
        }

        this.clockGranularitySeconds =
                clockGranularitySeconds;
    }

    public void observeRttSeconds(
            double sampleSeconds
    ) {
        if (!Double.isFinite(
                sampleSeconds
        )
                || sampleSeconds <= 0.0) {
            throw new IllegalArgumentException(
                    "sampleSeconds"
            );
        }

        if (!initialized) {
            srttSeconds =
                    sampleSeconds;

            rttvarSeconds =
                    sampleSeconds
                            / 2.0;

            initialized =
                    true;
        } else {
            rttvarSeconds =
                    (
                            1.0
                                    - BETA
                    )
                            * rttvarSeconds
                            + BETA
                            * Math.abs(
                            srttSeconds
                                    - sampleSeconds
                    );

            srttSeconds =
                    (
                            1.0
                                    - ALPHA
                    )
                            * srttSeconds
                            + ALPHA
                            * sampleSeconds;
        }

        rtoSeconds =
                clamp(
                        srttSeconds
                                + Math.max(
                                clockGranularitySeconds,
                                K
                                        * rttvarSeconds
                        )
                );
    }

    public void backoff() {
        rtoSeconds =
                clamp(
                        rtoSeconds
                                * 2.0
                );
    }

    public boolean initialized() {
        return initialized;
    }

    public double srttSeconds() {
        return initialized
                ? srttSeconds
                : Double.NaN;
    }

    public double rttvarSeconds() {
        return initialized
                ? rttvarSeconds
                : Double.NaN;
    }

    public double rtoSeconds() {
        return rtoSeconds;
    }

    public long rtoMicros() {
        return Math.max(
                1L,
                Math.round(
                        rtoSeconds
                                * 1_000_000.0
                )
        );
    }

    private double clamp(
            double value
    ) {
        return Math.max(
                MIN_RTO_SECONDS,
                Math.min(
                        MAX_RTO_SECONDS,
                        value
                )
        );
    }
}
