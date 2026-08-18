package com.k1ngtle.vsia.signality.engineering.wifi.live;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMcs;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyOfdmRateProfile;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyOfdmReceiveResult;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyOfdmWaveform;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyOfdmWaveformEncoder;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyOfdmWaveformReceiver;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyWaveformChannel;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.LegacyWaveformImpairment;
import com.k1ngtle.vsia.signality.engineering.wifi.baseband.WifiBitOrder;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.LdpcAwgn;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.LdpcDecodeResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.LdpcRateMatchPlan;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.LdpcRateMatcher;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcLabCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcStandardProfile;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcStandardProfiles;
import com.k1ngtle.vsia.signality.engineering.wifi.ldpc.WifiLdpcTargetRate;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyGeneration;

import java.util.Arrays;

public final class WifiLivePhyEngine {
    private static final int SCRAMBLER_SEED =
            0x5D;

    private WifiLivePhyEngine() {
    }

    public static WifiLivePhyDecision evaluate(
            byte[] psdu,
            WifiPhyGeneration generation,
            WifiMcs mcs,
            double snrDb,
            WifiLivePhyMode mode
    ) {
        if (mode == null
                || mode == WifiLivePhyMode.ANALYTICAL) {
            return WifiLivePhyDecision.bypass(
                    WifiLivePhyMode.ANALYTICAL,
                    snrDb,
                    "Fast analytical Phase-10 PHY path"
            );
        }

        if (psdu == null) {
            return new WifiLivePhyDecision(
                    mode,
                    WifiLivePhyPath.UNSUPPORTED_FALLBACK,
                    false,
                    false,
                    0,
                    0,
                    snrDb,
                    "Null PSDU"
            );
        }

        if (psdu.length > WifiLivePhySettings.MAX_FRAME_BYTES) {
            return WifiLivePhyDecision.fallback(
                    mode,
                    snrDb,
                    "PSDU exceeds configured detailed-PHY frame limit"
            );
        }

        if (generation == WifiPhyGeneration.LEGACY_OFDM) {
            return evaluateLegacy(
                    psdu,
                    mcs,
                    snrDb,
                    mode
            );
        }

        return evaluateLdpc(
                psdu,
                mcs,
                snrDb,
                mode
        );
    }

    private static WifiLivePhyDecision evaluateLegacy(
            byte[] psdu,
            WifiMcs mcs,
            double snrDb,
            WifiLivePhyMode mode
    ) {
        LegacyOfdmRateProfile rate =
                legacyRate(
                        mcs.index()
                );

        if (rate == null) {
            return WifiLivePhyDecision.fallback(
                    mode,
                    snrDb,
                    "No legacy OFDM bit-level profile for MCS "
                            + mcs.index()
            );
        }

        try {
            LegacyOfdmWaveform waveform =
                    LegacyOfdmWaveformEncoder.encode(
                            psdu,
                            rate,
                            SCRAMBLER_SEED
                    );

            long seed =
                    deterministicSeed(
                            psdu,
                            mcs.index(),
                            0
                    );

            LegacyWaveformImpairment impairment =
                    new LegacyWaveformImpairment(
                            LegacyOfdmWaveformReceiver.SAMPLE_RATE_HZ,
                            WifiLivePhySettings.LEGACY_CFO_HZ,
                            sanitizeSnr(
                                    snrDb
                            ),
                            seed,
                            WifiLivePhySettings.LEGACY_LEADING_ZERO_SAMPLES,
                            WifiLivePhySettings
                                    .LEGACY_PHASE_NOISE_STD_RAD_PER_SAMPLE
                    );

            LegacyOfdmReceiveResult received =
                    LegacyOfdmWaveformReceiver.receive(
                            LegacyWaveformChannel.apply(
                                    waveform.samples(),
                                    impairment
                            ),
                            SCRAMBLER_SEED
                    );

            boolean delivered =
                    Arrays.equals(
                            psdu,
                            received.psdu()
                    );

            return new WifiLivePhyDecision(
                    mode,
                    WifiLivePhyPath.LEGACY_OFDM_WAVEFORM,
                    true,
                    delivered,
                    1,
                    0,
                    snrDb,
                    delivered
                            ? "Full W1.2 legacy waveform decoded to the original MAC frame"
                            : "Full W1.2 legacy waveform decoder did not recover the MAC frame"
            );
        } catch (Exception exception) {
            return new WifiLivePhyDecision(
                    mode,
                    WifiLivePhyPath.LEGACY_OFDM_WAVEFORM,
                    true,
                    false,
                    1,
                    0,
                    snrDb,
                    "Legacy detailed PHY decode failed: "
                            + exception.getClass()
                            .getSimpleName()
            );
        }
    }

    private static WifiLivePhyDecision evaluateLdpc(
            byte[] psdu,
            WifiMcs mcs,
            double snrDb,
            WifiLivePhyMode mode
    ) {
        int[] payloadBits =
                WifiBitOrder.bytesToLsbFirstBits(
                        psdu
                );

        WifiLdpcTargetRate rate =
                WifiLdpcTargetRate.nearest(
                        mcs.codingRate()
                );

        int payloadCursor =
                0;

        int codewords =
                0;

        int iterations =
                0;

        while (payloadCursor < payloadBits.length
                || (
                payloadBits.length == 0
                        && codewords == 0
        )) {
            int remaining =
                    Math.max(
                            1,
                            payloadBits.length
                                    - payloadCursor
                    );

            WifiLdpcStandardProfile profile =
                    chooseProfile(
                            remaining,
                            rate
                    );

            int k =
                    profile.matrix()
                            .informationBits();

            int chunk =
                    Math.min(
                            remaining,
                            k
                    );

            if (payloadBits.length == 0) {
                chunk =
                        0;
            }

            int shortened =
                    k - chunk;

            int[] information =
                    new int[
                            k
                    ];

            if (chunk > 0) {
                System.arraycopy(
                        payloadBits,
                        payloadCursor,
                        information,
                        shortened,
                        chunk
                );
            }

            WifiLdpcLabCodec codec =
                    new WifiLdpcLabCodec(
                            profile.matrix()
                    );

            int[] motherCodeword =
                    codec.encode(
                            information
                    );

            LdpcRateMatchPlan rateMatchPlan =
                    new LdpcRateMatchPlan(
                            motherCodeword.length,
                            k,
                            shortened,
                            0,
                            motherCodeword.length
                                    - shortened
                    );

            int[] transmitted =
                    LdpcRateMatcher.transmitBits(
                            motherCodeword,
                            rateMatchPlan
                    );

            double[] receivedLlrs =
                    LdpcAwgn.bpskLlrs(
                            transmitted,
                            sanitizeSnr(
                                    snrDb
                            ),
                            deterministicSeed(
                                    psdu,
                                    mcs.index(),
                                    codewords + 1
                            )
                    );

            double[] motherLlrs =
                    LdpcRateMatcher.restoreLlrs(
                            receivedLlrs,
                            rateMatchPlan
                    );

            LdpcDecodeResult decoded =
                    codec.decode(
                            motherLlrs,
                            WifiLivePhySettings.MAX_LDPC_ITERATIONS
                    );

            codewords++;

            iterations +=
                    decoded.iterations();

            if (!decoded.converged()
                    || decoded.syndromeWeight() != 0) {
                return new WifiLivePhyDecision(
                        mode,
                        WifiLivePhyPath.STANDARD_LDPC_FEC,
                        true,
                        false,
                        codewords,
                        iterations,
                        snrDb,
                        "LDPC decoder did not converge to a zero-syndrome codeword"
                );
            }

            int[] decodedInformation =
                    decoded.informationBits();

            for (int bit = 0;
                 bit < chunk;
                 bit++) {
                int expected =
                        payloadBits[
                                payloadCursor + bit
                        ];

                int actual =
                        decodedInformation[
                                shortened + bit
                        ];

                if (expected != actual) {
                    return new WifiLivePhyDecision(
                            mode,
                            WifiLivePhyPath.STANDARD_LDPC_FEC,
                            true,
                            false,
                            codewords,
                            iterations,
                            snrDb,
                            "LDPC decoder converged but recovered different PSDU information bits"
                    );
                }
            }

            payloadCursor +=
                    chunk;

            if (payloadBits.length == 0) {
                break;
            }
        }

        return new WifiLivePhyDecision(
                mode,
                WifiLivePhyPath.STANDARD_LDPC_FEC,
                true,
                true,
                codewords,
                iterations,
                snrDb,
                "Pinned WLAN LDPC codeword chain recovered the MAC frame information bits"
        );
    }

    private static WifiLdpcStandardProfile chooseProfile(
            int requiredInformationBits,
            WifiLdpcTargetRate rate
    ) {
        WifiLdpcStandardProfile n648 =
                profile(
                        648,
                        rate
                );

        if (requiredInformationBits
                <= n648.matrix()
                .informationBits()) {
            return n648;
        }

        WifiLdpcStandardProfile n1296 =
                profile(
                        1296,
                        rate
                );

        if (requiredInformationBits
                <= n1296.matrix()
                .informationBits()) {
            return n1296;
        }

        return profile(
                1944,
                rate
        );
    }

    private static WifiLdpcStandardProfile profile(
            int n,
            WifiLdpcTargetRate rate
    ) {
        if (n == 648) {
            return switch (rate) {
                case RATE_1_2 ->
                        WifiLdpcStandardProfiles
                                .n648RateOneHalf();

                case RATE_2_3 ->
                        WifiLdpcStandardProfiles
                                .n648RateTwoThirds();

                case RATE_3_4 ->
                        WifiLdpcStandardProfiles
                                .n648RateThreeQuarter();

                case RATE_5_6 ->
                        WifiLdpcStandardProfiles
                                .n648RateFiveSixths();
            };
        }

        if (n == 1296) {
            return switch (rate) {
                case RATE_1_2 ->
                        WifiLdpcStandardProfiles
                                .n1296RateOneHalf();

                case RATE_2_3 ->
                        WifiLdpcStandardProfiles
                                .n1296RateTwoThirds();

                case RATE_3_4 ->
                        WifiLdpcStandardProfiles
                                .n1296RateThreeQuarter();

                case RATE_5_6 ->
                        WifiLdpcStandardProfiles
                                .n1296RateFiveSixths();
            };
        }

        return switch (rate) {
            case RATE_1_2 ->
                    WifiLdpcStandardProfiles
                            .n1944RateOneHalf();

            case RATE_2_3 ->
                    WifiLdpcStandardProfiles
                            .n1944RateTwoThirds();

            case RATE_3_4 ->
                    WifiLdpcStandardProfiles
                            .n1944RateThreeQuarter();

            case RATE_5_6 ->
                    WifiLdpcStandardProfiles
                            .n1944RateFiveSixths();
        };
    }

    private static LegacyOfdmRateProfile legacyRate(
            int mcsIndex
    ) {
        return switch (mcsIndex) {
            case 0 ->
                    LegacyOfdmRateProfile.ofMbps(
                            6
                    );

            case 1 ->
                    LegacyOfdmRateProfile.ofMbps(
                            12
                    );

            case 2 ->
                    LegacyOfdmRateProfile.ofMbps(
                            18
                    );

            case 3 ->
                    LegacyOfdmRateProfile.ofMbps(
                            24
                    );

            case 4 ->
                    LegacyOfdmRateProfile.ofMbps(
                            36
                    );

            case 5 ->
                    LegacyOfdmRateProfile.ofMbps(
                            48
                    );

            case 6 ->
                    LegacyOfdmRateProfile.ofMbps(
                            54
                    );

            default ->
                    null;
        };
    }

    private static double sanitizeSnr(
            double snrDb
    ) {
        if (!Double.isFinite(
                snrDb
        )) {
            return 30.0;
        }

        return Math.max(
                -20.0,
                Math.min(
                        60.0,
                        snrDb
                )
        );
    }

    private static long deterministicSeed(
            byte[] psdu,
            int mcsIndex,
            int codewordIndex
    ) {
        long hash =
                Arrays.hashCode(
                        psdu
                );

        hash =
                hash * 0x9E3779B97F4A7C15L
                        + mcsIndex;

        hash =
                hash * 0xBF58476D1CE4E5B9L
                        + codewordIndex;

        return hash;
    }
}
