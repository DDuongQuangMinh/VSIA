package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.engineering.channel.RfChannelSettings;
import com.k1ngtle.vsia.signality.engineering.channel.RfMediumState;
import com.k1ngtle.vsia.signality.engineering.phy.PhyResult;
import com.k1ngtle.vsia.signality.engineering.reality.NetworkRealityAssessment;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyDecision;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyPath;
import com.k1ngtle.vsia.signality.engineering.wifi.phy.WifiPhyConfiguration;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;

public final class WifiEngineeringProbe {
    private WifiEngineeringProbe() {
    }

    public static boolean supports(
            NetworkDeviceBlockEntity device
    ) {
        return device != null
                && device.networkProfile()
                .kind()
                == NetworkKind.WIFI;
    }

    public static WifiEngineeringSnapshot capture(
            NetworkDeviceBlockEntity device
    ) {
        if (!supports(
                device
        )) {
            throw new IllegalArgumentException(
                    "Target is not a Wi-Fi network device"
            );
        }

        WifiPhyConfiguration configuration =
                device.wifiPhyConfiguration();

        PhyResult phy =
                device.lastPhyResult();

        NetworkRealityAssessment reality =
                device.lastNetworkRealityAssessment();

        WifiLivePhyDecision live =
                device.lastWifiLivePhyDecision();

        RfMediumState medium =
                device.senseRfMedium(
                        RfChannelSettings
                                .WIFI_ENERGY_DETECT_THRESHOLD_DBM
                );

        double receivedPowerDbm =
                phy == null
                        ? Double.NaN
                        : phy.receivedPowerDbm();

        double snrDb =
                phy == null
                        ? Double.NaN
                        : phy.snrDb();

        double ber =
                phy == null
                        ? Double.NaN
                        : phy.bitErrorRate();

        double fer =
                phy == null
                        ? Double.NaN
                        : phy.frameErrorRate();

        double correctedSinrDb =
                reality == null
                        ? Double.NaN
                        : reality.correctedSinrDb();

        double propagationDelayMicros =
                reality == null
                        ? Double.NaN
                        : reality.propagationDelayMicros();

        long airtimeMicros =
                reality == null
                        ? -1L
                        : reality.desiredAirtimeMicros();

        double temporalInterferenceFactor =
                reality == null
                        ? Double.NaN
                        : reality
                        .microTemporalInterferenceFactor();

        double captureMarginDb =
                reality == null
                        ? Double.NaN
                        : reality.captureMarginDb();

        boolean captured =
                reality == null
                        || reality
                        .receiverCapturedDesiredFrame();

        WifiLivePhyPath livePath =
                live == null
                        ? WifiLivePhyPath.BYPASS
                        : live.path();

        boolean liveEvaluated =
                live != null
                        && live.evaluated();

        boolean liveDelivered =
                live == null
                        || live.delivered();

        int codewords =
                live == null
                        ? 0
                        : live.codewords();

        int iterations =
                live == null
                        ? 0
                        : live.decoderIterations();

        String detail =
                live == null
                        ? "No detailed PHY decision yet"
                        : live.detail();

        return new WifiEngineeringSnapshot(
                device.id(),
                device.networkProfileId()
                        .toString(),
                device.activeFrequencyHz(),
                device.wifiMode()
                        .name(),
                device.wifiStationState()
                        .name(),
                device.wifiSecurityState()
                        .name(),
                device.wifiMcsIndex(),
                configuration == null
                        ? null
                        : configuration.generation(),
                configuration == null
                        ? 0
                        : configuration.channelWidth()
                        .mhz(),
                configuration == null
                        ? Double.NaN
                        : configuration.guardInterval()
                        .microseconds(),
                configuration == null
                        ? 0
                        : configuration.spatialStreams(),
                device.wifiEstimatedPhyRateBps(),
                device.wifiDopplerIciFraction(),
                receivedPowerDbm,
                snrDb,
                ber,
                fer,
                correctedSinrDb,
                propagationDelayMicros,
                airtimeMicros,
                temporalInterferenceFactor,
                captureMarginDb,
                captured,
                medium.busy(),
                medium.totalEnergyDbm(),
                medium.overlappingTransmitters(),
                device.wifiLivePhyMode(),
                livePath,
                liveEvaluated,
                liveDelivered,
                codewords,
                iterations,
                detail
        );
    }
}
