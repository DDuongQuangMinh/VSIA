package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;

public final class WifiEngineeringTestLinkService {
    public static final int DEFAULT_TEST_FRAME_BYTES =
            512;

    private WifiEngineeringTestLinkService() {
    }

    public static WifiEngineeringTestLinkResult run(
            NetworkDeviceBlockEntity target,
            int requestedFrameBytes
    ) {
        if (target == null
                || target.networkProfile()
                .kind()
                != NetworkKind.WIFI) {
            return new WifiEngineeringTestLinkResult(
                    false,
                    null,
                    Double.NaN,
                    0,
                    "Target is not a Wi-Fi NetworkDeviceBlockEntity"
            );
        }

        ServerLevel level =
                target.level();

        int frameBytes =
                Math.max(
                        64,
                        Math.min(
                                4096,
                                requestedFrameBytes
                        )
                );

        NetworkDeviceBlockEntity peer =
                SignalBus.receiversInLevel(
                                level
                        )
                        .stream()
                        .filter(
                                receiver ->
                                        receiver
                                        instanceof NetworkDeviceBlockEntity
                        )
                        .map(
                                receiver ->
                                        (NetworkDeviceBlockEntity) receiver
                        )
                        .filter(
                                candidate ->
                                        !candidate.id()
                                                .equals(
                                                        target.id()
                                                )
                        )
                        .filter(
                                candidate ->
                                        candidate.networkProfile()
                                                .kind()
                                                == NetworkKind.WIFI
                        )
                        .filter(
                                candidate ->
                                        WifiEngineeringTestLinkSelector
                                                .frequenciesOverlap(
                                                        candidate.activeFrequencyHz(),
                                                        candidate.tuningBandwidthHz(),
                                                        target.activeFrequencyHz(),
                                                        target.tuningBandwidthHz()
                                                )
                        )
                        .min(
                                Comparator.comparingDouble(
                                        candidate ->
                                                WifiEngineeringTestLinkSelector
                                                        .candidateScore(
                                                                candidate.positionWorld()
                                                                        .distanceTo(
                                                                                target.positionWorld()
                                                                        ),
                                                                candidate.networkProfileId()
                                                                        .equals(
                                                                                target.networkProfileId()
                                                                        ),
                                                                Double.compare(
                                                                        candidate.activeFrequencyHz(),
                                                                        target.activeFrequencyHz()
                                                                ) == 0
                                                        )
                                )
                        )
                        .orElse(
                                null
                        );

        if (peer == null) {
            return new WifiEngineeringTestLinkResult(
                    false,
                    null,
                    Double.NaN,
                    frameBytes,
                    "No compatible registered Wi-Fi peer is tuned to this RF channel. Place/configure a second Wi-Fi NetworkDeviceBlockEntity in range."
            );
        }

        peer.setWifiLivePhyMode(
                target.wifiLivePhyMode()
        );

        boolean queued =
                peer.sendWifiEngineeringTestFrame(
                        frameBytes
                );

        double distance =
                peer.positionWorld()
                        .distanceTo(
                                target.positionWorld()
                        );

        if (!queued) {
            return new WifiEngineeringTestLinkResult(
                    false,
                    peer.getBlockPos(),
                    distance,
                    frameBytes,
                    "Compatible peer was found but its engineering test frame could not be queued."
            );
        }

        return new WifiEngineeringTestLinkResult(
                true,
                peer.getBlockPos(),
                distance,
                frameBytes,
                "Queued a real Wi-Fi MAC test frame from the selected peer through the existing RF scheduler."
        );
    }
}
