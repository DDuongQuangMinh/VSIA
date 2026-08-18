package com.k1ngtle.vsia.signality.engineering.conformance;

import com.k1ngtle.vsia.signality.engineering.math.RfMath;
import com.k1ngtle.vsia.signality.engineering.channel.DopplerModel;
import com.k1ngtle.vsia.signality.engineering.channel.SpectralOverlap;
import com.k1ngtle.vsia.signality.engineering.channel.TemporalOverlap;
import com.k1ngtle.vsia.signality.engineering.channel.AntennaPatternModel;
import com.k1ngtle.vsia.signality.engineering.channel.OfdmDopplerModel;
import com.k1ngtle.vsia.signality.engineering.channel.PolarizationLossModel;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaPattern;
import com.k1ngtle.vsia.signality.engineering.channel.RfAntennaState;
import com.k1ngtle.vsia.signality.engineering.channel.RfPolarization;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class EngineeringLabSuite {
    private EngineeringLabSuite() {
    }

    public static List<LabCheckResult> runDeterministicChecks() {
        List<LabCheckResult> results =
                new ArrayList<>();

        add(
                results,
                "rf-1w-30dbm",
                Math.abs(
                        RfMath.wattsToDbm(
                                1.0
                        ) - 30.0
                ) < 1.0E-9,
                "1 W must equal 30 dBm"
        );

        add(
                results,
                "rf-shannon-20mhz-0db",
                Math.abs(
                        RfMath.shannonCapacityBps(
                                20_000_000.0,
                                0.0
                        ) - 20_000_000.0
                ) < 1.0,
                "20 MHz at 0 dB SNR must be approximately 20 Mbit/s"
        );

        RfLabResult near =
                RfEngineeringLab.evaluate(
                        new RfLabScenario(
                                2.4E9,
                                10.0,
                                20.0E6,
                                20.0,
                                0.0,
                                0.0,
                                7.0,
                                0.0
                        )
                );

        RfLabResult far =
                RfEngineeringLab.evaluate(
                        new RfLabScenario(
                                2.4E9,
                                100.0,
                                20.0E6,
                                20.0,
                                0.0,
                                0.0,
                                7.0,
                                0.0
                        )
                );

        add(
                results,
                "rf-distance-monotonic",
                far.pathLossDb()
                        > near.pathLossDb()
                        && far.snrDb()
                        < near.snrDb(),
                "Increasing distance must increase FSPL and reduce SNR"
        );

        DeterministicLabRandom a =
                new DeterministicLabRandom(
                        123456789L
                );

        DeterministicLabRandom b =
                new DeterministicLabRandom(
                        123456789L
                );

        boolean same =
                true;

        for (int i = 0;
             i < 32;
             i++) {
            if (Double.compare(
                    a.nextDouble(),
                    b.nextDouble()
            ) != 0) {
                same =
                        false;
                break;
            }
        }

        add(
                results,
                "deterministic-rng",
                same,
                "Identical seeds must generate identical lab sequences"
        );

        add(
                results,
                "channel-spectral-overlap-full",
                Math.abs(
                        SpectralOverlap.fractionOfReceiverBandwidth(
                                2.4E9,
                                20.0E6,
                                2.4E9,
                                20.0E6
                        ) - 1.0
                ) < 1.0E-12,
                "Identical 20 MHz channels must overlap by 100%"
        );

        add(
                results,
                "channel-spectral-overlap-none",
                SpectralOverlap.fractionOfReceiverBandwidth(
                        2.4E9,
                        20.0E6,
                        2.5E9,
                        20.0E6
                ) == 0.0,
                "Separated channels must have zero spectral overlap"
        );

        add(
                results,
                "channel-temporal-overlap-full",
                Math.abs(
                        TemporalOverlap.fractionOfDesired(
                                10L,
                                14L,
                                10L,
                                14L
                        ) - 1.0
                ) < 1.0E-12,
                "Identical airtime windows must overlap by 100%"
        );

        add(
                results,
                "channel-temporal-overlap-partial",
                Math.abs(
                        TemporalOverlap.fractionOfDesired(
                                10L,
                                14L,
                                14L,
                                20L
                        ) - 0.2
                ) < 1.0E-12,
                "One overlapping tick out of five desired ticks must equal 20%"
        );

        RfAntennaState directional =
                new RfAntennaState(
                        RfAntennaPattern.YAGI,
                        RfPolarization.VERTICAL,
                        new Vec3(
                                0.0,
                                0.0,
                                1.0
                        ),
                        10.0,
                        60.0,
                        60.0,
                        20.0
                );

        add(
                results,
                "antenna-forward-gain-exceeds-back",
                AntennaPatternModel.gainTowardDbi(
                        directional,
                        new Vec3(
                                0.0,
                                0.0,
                                1.0
                        )
                )
                        > AntennaPatternModel.gainTowardDbi(
                        directional,
                        new Vec3(
                                0.0,
                                0.0,
                                -1.0
                        )
                ),
                "Directional antenna gain must be larger in the boresight direction"
        );

        add(
                results,
                "polarization-linear-orthogonal-loss",
                Math.abs(
                        PolarizationLossModel.mismatchLossDb(
                                RfPolarization.VERTICAL,
                                RfPolarization.HORIZONTAL
                        ) - 20.0
                ) < 1.0E-12,
                "Initial vertical/horizontal mismatch calibration is 20 dB"
        );

        add(
                results,
                "polarization-linear-circular-loss",
                Math.abs(
                        PolarizationLossModel.mismatchLossDb(
                                RfPolarization.VERTICAL,
                                RfPolarization.RHCP
                        ) - 3.0
                ) < 1.0E-12,
                "Idealized linear/circular polarization mismatch is 3 dB"
        );

        add(
                results,
                "ofdm-doppler-zero-offset",
                Math.abs(
                        OfdmDopplerModel.assess(
                                0.0,
                                15_000.0
                        ).desiredSubcarrierPowerFraction()
                                - 1.0
                ) < 1.0E-12,
                "Zero Doppler must preserve full desired OFDM subcarrier power"
        );

        add(
                results,
                "doppler-approach-positive",
                DopplerModel.shiftHz(
                        6.0E9,
                        100.0
                ) > 0.0,
                "Positive radial approach velocity must produce positive frequency shift"
        );

        add(
                results,
                "channel-doppler-zero-velocity",
                DopplerModel.shiftHz(
                        6.0E9,
                        0.0
                ) == 0.0,
                "Zero radial velocity must produce zero Doppler shift"
        );

        for (KnownAnswerResult kat
                : KnownAnswerSuite.runAll()) {
            add(
                    results,
                    "kat-" + kat.id(),
                    kat.passed(),
                    kat.note()
                            + " expected="
                            + kat.expected()
                            + " actual="
                            + kat.actual()
            );
        }

        return List.copyOf(
                results
        );
    }

    private static void add(
            List<LabCheckResult> results,
            String id,
            boolean passed,
            String detail
    ) {
        results.add(
                new LabCheckResult(
                        id,
                        passed,
                        detail
                )
        );
    }
}
