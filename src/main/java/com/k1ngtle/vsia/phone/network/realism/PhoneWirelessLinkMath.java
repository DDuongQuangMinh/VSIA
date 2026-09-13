package com.k1ngtle.vsia.phone.network.realism;

import com.k1ngtle.vsia.signality.engineering.channel.FadingModel;
import com.k1ngtle.vsia.signality.engineering.channel.MaterialAttenuationModel;
import com.k1ngtle.vsia.signality.engineering.channel.SmallScaleFading;
import com.k1ngtle.vsia.signality.engineering.channel.StableShadowing;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public final class PhoneWirelessLinkMath {
    private static final double C =
            299_792_458.0;

    private static final double PHONE_WIFI_GAIN_DBI =
            -2.0;

    private static final double PHONE_CELLULAR_GAIN_DBI =
            -3.0;

    private static final double WIFI_NOISE_FIGURE_DB =
            7.0;

    private static final double CELLULAR_NOISE_FIGURE_DB =
            8.0;

    private PhoneWirelessLinkMath() {
    }

    public static RawLink measureWifi(
            ServerLevel level,
            NetworkDeviceBlockEntity device,
            Vec3 receiver,
            UUID phoneRadioId
    ) {
        return measure(
                level,
                device,
                receiver,
                phoneRadioId,
                PHONE_WIFI_GAIN_DBI,
                WIFI_NOISE_FIGURE_DB
        );
    }

    public static RawLink measureCellular(
            ServerLevel level,
            NetworkDeviceBlockEntity device,
            Vec3 receiver,
            UUID phoneRadioId
    ) {
        return measure(
                level,
                device,
                receiver,
                phoneRadioId,
                PHONE_CELLULAR_GAIN_DBI,
                CELLULAR_NOISE_FIGURE_DB
        );
    }

    public static double sinrDb(
            RawLink desired,
            List<RawLink> all
    ) {
        if (desired == null) {
            return Double.NEGATIVE_INFINITY;
        }

        double interferenceMilliwatts =
                0.0;

        if (all != null) {
            for (RawLink candidate
                    : all) {
                if (candidate == null
                        || candidate.deviceId()
                        .equals(
                                desired.deviceId()
                        )) {
                    continue;
                }

                double overlap =
                        spectralOverlap(
                                desired.frequencyHz(),
                                desired.bandwidthHz(),
                                candidate.frequencyHz(),
                                candidate.bandwidthHz()
                        );

                if (overlap <= 0.0) {
                    continue;
                }

                interferenceMilliwatts +=
                        dbmToMilliwatts(
                                candidate.receivedPowerDbm()
                        )
                                * overlap;
            }
        }

        double noiseMilliwatts =
                dbmToMilliwatts(
                        desired.noiseFloorDbm()
                );

        double desiredMilliwatts =
                dbmToMilliwatts(
                        desired.receivedPowerDbm()
                );

        if (desiredMilliwatts <= 0.0) {
            return Double.NEGATIVE_INFINITY;
        }

        double denominator =
                noiseMilliwatts
                        + interferenceMilliwatts;

        if (denominator <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        return 10.0
                * Math.log10(
                desiredMilliwatts
                        / denominator
        );
    }

    public static double rsrpDbm(
            RawLink cellular
    ) {
        if (cellular == null) {
            return -140.0;
        }

        double resourceBlocks =
                Math.max(
                        1.0,
                        cellular.bandwidthHz()
                                / 180_000.0
                );

        double referenceElements =
                Math.max(
                        12.0,
                        resourceBlocks
                                * 12.0
                );

        return cellular.receivedPowerDbm()
                - 10.0
                * Math.log10(
                referenceElements
        );
    }

    public static double rsrqDb(
            RawLink cellular,
            double rsrpDbm
    ) {
        if (cellular == null) {
            return -30.0;
        }

        double resourceBlocks =
                Math.max(
                        1.0,
                        cellular.bandwidthHz()
                                / 180_000.0
                );

        double rsrq =
                10.0
                        * Math.log10(
                        resourceBlocks
                )
                        + rsrpDbm
                        - cellular.receivedPowerDbm();

        return clamp(
                rsrq,
                -30.0,
                -3.0
        );
    }

    public static int wifiChannel(
            double frequencyHz
    ) {
        double mhz =
                frequencyHz
                        / 1_000_000.0;

        if (mhz >= 2412.0
                && mhz <= 2472.0) {
            return (int) Math.round(
                    (
                            mhz - 2407.0
                    )
                            / 5.0
            );
        }

        if (mhz >= 2483.0
                && mhz <= 2485.0) {
            return 14;
        }

        if (mhz >= 5000.0
                && mhz < 5925.0) {
            return (int) Math.round(
                    (
                            mhz - 5000.0
                    )
                            / 5.0
            );
        }

        if (mhz >= 5955.0
                && mhz <= 7115.0) {
            return (int) Math.round(
                    (
                            mhz - 5950.0
                    )
                            / 5.0
            );
        }

        return 0;
    }

    public static String wifiQuality(
            int rssiDbm,
            double sinrDb
    ) {
        if (rssiDbm < -90
                || sinrDb < -5.0) {
            return "UNUSABLE";
        }

        if (rssiDbm < -80
                || sinrDb < 5.0) {
            return "POOR";
        }

        if (rssiDbm < -67
                || sinrDb < 15.0) {
            return "FAIR";
        }

        if (rssiDbm < -55
                || sinrDb < 25.0) {
            return "GOOD";
        }

        return "EXCELLENT";
    }

    public static String cellularQuality(
            int rsrpDbm,
            double sinrDb
    ) {
        if (rsrpDbm < -125
                || sinrDb < -8.0) {
            return "NO SERVICE";
        }

        if (rsrpDbm < -115
                || sinrDb < -3.0) {
            return "VERY POOR";
        }

        if (rsrpDbm < -105
                || sinrDb < 3.0) {
            return "POOR";
        }

        if (rsrpDbm < -95
                || sinrDb < 10.0) {
            return "FAIR";
        }

        if (rsrpDbm < -85
                || sinrDb < 20.0) {
            return "GOOD";
        }

        return "EXCELLENT";
    }

    public static double estimatedDownlinkMbps(
            RawLink link,
            double sinrDb
    ) {
        if (link == null
                || !Double.isFinite(
                sinrDb
        )
                || sinrDb < -8.0) {
            return 0.0;
        }

        double linear =
                Math.pow(
                        10.0,
                        sinrDb
                                / 10.0
                );

        double bitsPerSecond =
                link.bandwidthHz()
                        * (
                        Math.log(
                                1.0 + linear
                        )
                                / Math.log(
                                2.0
                        )
                )
                        * 0.32;

        return Math.max(
                0.0,
                Math.min(
                        2_000.0,
                        bitsPerSecond
                                / 1_000_000.0
                )
        );
    }

    private static RawLink measure(
            ServerLevel level,
            NetworkDeviceBlockEntity device,
            Vec3 receiver,
            UUID phoneRadioId,
            double receiverGainDbi,
            double noiseFigureDb
    ) {
        NetworkProfile profile =
                device.networkProfile();

        Vec3 transmitter =
                device.positionWorld();

        double distance =
                Math.max(
                        1.0,
                        transmitter.distanceTo(
                                receiver
                        )
                );

        double frequencyHz =
                Math.max(
                        1.0,
                        device.activeFrequencyHz()
                );

        double bandwidthHz =
                Math.max(
                        1.0,
                        profile.bandwidthHz()
                );

        double txPowerDbm =
                wattsToDbm(
                        profile.transmitPowerWatts()
                );

        double txGainDbi =
                linearGainToDbi(
                        profile.antennaGain()
                );

        double fsplDb =
                20.0
                        * Math.log10(
                        4.0
                                * Math.PI
                                * distance
                                * frequencyHz
                                / C
                );

        double materialLossDb =
                MaterialAttenuationModel
                        .estimateLossDb(
                                level,
                                transmitter,
                                receiver,
                                frequencyHz
                        );

        double shadowingDb =
                StableShadowing
                        .offsetDb(
                                level.dimension()
                                        .location()
                                        .toString(),
                                transmitter,
                                receiver,
                                frequencyHz
                        );

        FadingModel fadingModel =
                materialLossDb > 0.1
                        ? FadingModel.RAYLEIGH
                        : FadingModel.RICIAN;

        double fadingDb =
                SmallScaleFading
                        .fadingDb(
                                device.id(),
                                phoneRadioId,
                                level.getGameTime(),
                                fadingModel,
                                6.0
                        );

        double receivedPowerDbm =
                txPowerDbm
                        + txGainDbi
                        + receiverGainDbi
                        - fsplDb
                        - materialLossDb
                        + shadowingDb
                        + fadingDb;

        double noiseFloorDbm =
                -174.0
                        + 10.0
                        * Math.log10(
                        bandwidthHz
                )
                        + noiseFigureDb;

        return new RawLink(
                device.id(),
                frequencyHz,
                bandwidthHz,
                distance,
                receivedPowerDbm,
                noiseFloorDbm,
                materialLossDb,
                shadowingDb,
                fadingDb,
                profile.maximumRangeBlocks()
        );
    }

    private static double spectralOverlap(
            double firstCenter,
            double firstBandwidth,
            double secondCenter,
            double secondBandwidth
    ) {
        double firstLow =
                firstCenter
                        - firstBandwidth
                        / 2.0;

        double firstHigh =
                firstCenter
                        + firstBandwidth
                        / 2.0;

        double secondLow =
                secondCenter
                        - secondBandwidth
                        / 2.0;

        double secondHigh =
                secondCenter
                        + secondBandwidth
                        / 2.0;

        double overlap =
                Math.max(
                        0.0,
                        Math.min(
                                firstHigh,
                                secondHigh
                        )
                                - Math.max(
                                firstLow,
                                secondLow
                        )
                );

        return clamp(
                overlap
                        / Math.max(
                        1.0,
                        firstBandwidth
                ),
                0.0,
                1.0
        );
    }

    private static double wattsToDbm(
            double watts
    ) {
        return 10.0
                * Math.log10(
                Math.max(
                        1.0E-15,
                        watts
                )
                        * 1000.0
        );
    }

    private static double linearGainToDbi(
            double gain
    ) {
        return 10.0
                * Math.log10(
                Math.max(
                        1.0E-12,
                        gain
                )
        );
    }

    private static double dbmToMilliwatts(
            double dbm
    ) {
        if (!Double.isFinite(
                dbm
        )) {
            return 0.0;
        }

        return Math.pow(
                10.0,
                dbm
                        / 10.0
        );
    }

    private static double clamp(
            double value,
            double min,
            double max
    ) {
        return Math.max(
                min,
                Math.min(
                        max,
                        value
                )
        );
    }

    public record RawLink(
            UUID deviceId,
            double frequencyHz,
            double bandwidthHz,
            double distanceBlocks,
            double receivedPowerDbm,
            double noiseFloorDbm,
            double materialLossDb,
            double shadowingDb,
            double fadingDb,
            double maximumRangeBlocks
    ) {
    }
}
