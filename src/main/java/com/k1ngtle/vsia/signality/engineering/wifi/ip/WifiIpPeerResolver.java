package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.network.NetworkKind;

import java.util.Comparator;
import java.util.Locale;

public final class WifiIpPeerResolver {
    private WifiIpPeerResolver() {
    }

    public static NetworkDeviceBlockEntity resolve(
            NetworkDeviceBlockEntity device
    ) {
        if (device == null
                || device.networkProfile()
                .kind()
                != NetworkKind.WIFI) {
            return null;
        }

        String ownMac =
                normalizeMac(
                        device.wifiMacAddress()
                );

        return SignalBus.receiversInLevel(
                        device.level()
                )
                .stream()
                .filter(
                        receiver ->
                                receiver
                                instanceof NetworkDeviceBlockEntity
                )
                .map(
                        receiver ->
                                (
                                        NetworkDeviceBlockEntity
                                ) receiver
                )
                .filter(
                        candidate ->
                                !candidate.id()
                                .equals(
                                        device.id()
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
                                Math.abs(
                                        candidate.activeFrequencyHz()
                                                - device.activeFrequencyHz()
                                )
                                        <= Math.max(
                                        candidate.tuningBandwidthHz(),
                                        device.tuningBandwidthHz()
                                )
                                / 2.0
                )
                .filter(
                        candidate ->
                                relationshipMatches(
                                        device,
                                        candidate,
                                        ownMac
                                )
                )
                .min(
                        Comparator.comparingDouble(
                                candidate ->
                                        candidate.positionWorld()
                                                .distanceTo(
                                                        device.positionWorld()
                                                )
                        )
                )
                .orElse(
                        null
                );
    }

    private static boolean relationshipMatches(
            NetworkDeviceBlockEntity device,
            NetworkDeviceBlockEntity candidate,
            String ownMac
    ) {
        String candidateMac =
                normalizeMac(
                        candidate.wifiMacAddress()
                );

        boolean candidateKnowsUs =
                candidate.wifiAssociatedStations()
                        .stream()
                        .map(
                                WifiIpPeerResolver::normalizeMac
                        )
                        .anyMatch(
                                ownMac::equals
                        );

        boolean weKnowCandidate =
                device.wifiAssociatedStations()
                        .stream()
                        .map(
                                WifiIpPeerResolver::normalizeMac
                        )
                        .anyMatch(
                                candidateMac::equals
                        );

        return candidateKnowsUs
                || weKnowCandidate;
    }

    private static String normalizeMac(
            String value
    ) {
        return value == null
                ? ""
                : value.replace(
                                ":",
                                ""
                        )
                        .replace(
                                "-",
                                ""
                        )
                        .toUpperCase(
                                Locale.ROOT
                        );
    }
}
