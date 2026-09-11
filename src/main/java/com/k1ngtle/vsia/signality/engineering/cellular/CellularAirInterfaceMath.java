package com.k1ngtle.vsia.signality.engineering.cellular;

public final class CellularAirInterfaceMath {
    public static final double GSM_CARRIER_SPACING_HZ =
            200_000.0;

    public static final int GSM_TIMESLOTS_PER_FRAME =
            8;

    public static final double GSM_FRAME_DURATION_SECONDS =
            4.615e-3;

    public static final double WCDMA_CHIP_RATE_CHIPS_PER_SECOND =
            3.84e6;

    private CellularAirInterfaceMath() {
    }

    public static int analogFdmaChannels(
            double allocatedBandwidthHz,
            double channelSpacingHz
    ) {
        if (!(allocatedBandwidthHz > 0.0)
                || !(channelSpacingHz > 0.0)) {
            return 0;
        }

        return Math.max(
                0,
                (int) Math.floor(
                        allocatedBandwidthHz
                                / channelSpacingHz
                )
        );
    }

    public static int gsmCarriers(
            double allocatedBandwidthHz
    ) {
        return analogFdmaChannels(
                allocatedBandwidthHz,
                GSM_CARRIER_SPACING_HZ
        );
    }

    public static int gsmTrafficChannels(
            double allocatedBandwidthHz,
            int reservedTimeslots
    ) {
        int carriers =
                gsmCarriers(
                        allocatedBandwidthHz
                );

        int slots =
                carriers
                        * GSM_TIMESLOTS_PER_FRAME;

        return Math.max(
                0,
                slots
                        - Math.max(
                        0,
                        reservedTimeslots
                )
        );
    }

    public static double gsmTimeslotDurationSeconds() {
        return GSM_FRAME_DURATION_SECONDS
                / GSM_TIMESLOTS_PER_FRAME;
    }

    public static double wcdmaProcessingGainDb(
            double userBitRateBps
    ) {
        if (!(userBitRateBps > 0.0)) {
            return Double.POSITIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                WCDMA_CHIP_RATE_CHIPS_PER_SECOND
                        / userBitRateBps
        );
    }

    public static int nearestWcdmaSpreadingFactor(
            double userBitRateBps
    ) {
        if (!(userBitRateBps > 0.0)) {
            return 512;
        }

        double raw =
                WCDMA_CHIP_RATE_CHIPS_PER_SECOND
                        / userBitRateBps;

        int[] factors = {
                4,
                8,
                16,
                32,
                64,
                128,
                256,
                512
        };

        int best =
                factors[0];

        double error =
                Math.abs(
                        factors[0]
                                - raw
                );

        for (int factor : factors) {
            double candidate =
                    Math.abs(
                            factor
                                    - raw
                    );

            if (candidate < error) {
                best =
                        factor;
                error =
                        candidate;
            }
        }

        return best;
    }

    public static int lteResourceBlocks(
            double bandwidthHz
    ) {
        double mhz =
                bandwidthHz
                        / 1_000_000.0;

        if (mhz <= 1.4) {
            return 6;
        }
        if (mhz <= 3.0) {
            return 15;
        }
        if (mhz <= 5.0) {
            return 25;
        }
        if (mhz <= 10.0) {
            return 50;
        }
        if (mhz <= 15.0) {
            return 75;
        }

        return 100;
    }

    public static double lteResourceBlockBandwidthHz() {
        return 12.0
                * 15_000.0;
    }

    public static double nrSubcarrierSpacingHz(
            int numerology
    ) {
        int mu =
                Math.max(
                        0,
                        Math.min(
                                4,
                                numerology
                        )
                );

        return 15_000.0
                * Math.pow(
                2.0,
                mu
        );
    }

    public static double nrSlotDurationSeconds(
            int numerology
    ) {
        int mu =
                Math.max(
                        0,
                        Math.min(
                                4,
                                numerology
                        )
                );

        return 1.0e-3
                / Math.pow(
                2.0,
                mu
        );
    }

    public static double nrResourceBlockBandwidthHz(
            int numerology
    ) {
        return 12.0
                * nrSubcarrierSpacingHz(
                numerology
        );
    }

    public static int nrResourceBlocks(
            double bandwidthHz,
            int numerology,
            double guardFraction
    ) {
        if (!(bandwidthHz > 0.0)) {
            return 0;
        }

        double usable =
                bandwidthHz
                        * Math.max(
                        0.1,
                        Math.min(
                                1.0,
                                1.0
                                        - Math.max(
                                        0.0,
                                        guardFraction
                                )
                        )
                );

        return Math.max(
                1,
                (int) Math.floor(
                        usable
                                / nrResourceBlockBandwidthHz(
                                numerology
                        )
                )
        );
    }

    public static double estimatedPeakUserRateBps(
            CellularGeneration generation,
            double bandwidthHz,
            int cqi,
            int mimoLayers
    ) {
        CellularGeneration resolved =
                generation == null
                        ? CellularGeneration.G5_NR
                        : generation;

        double efficiency =
                CellularQosScheduler
                        .spectralEfficiencyFromCqi(
                                cqi
                        );

        int layers =
                Math.max(
                        1,
                        Math.min(
                                mimoLayers,
                                CellularGenerationCatalog
                                        .profile(
                                                resolved
                                        )
                                        .maximumMimoLayers()
                        )
                );

        double implementationEfficiency =
                switch (resolved) {
                    case G1_ANALOG -> 0.25;
                    case G2_GSM -> 0.35;
                    case G3_UMTS -> 0.55;
                    case G4_LTE -> 0.78;
                    case G5_NR -> 0.83;
                };

        return Math.max(
                0.0,
                bandwidthHz
                        * efficiency
                        * layers
                        * implementationEfficiency
        );
    }
}
