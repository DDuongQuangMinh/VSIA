package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.network.realism.PhoneWirelessSnapshot;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;

import java.util.List;

public final class PhoneNetworkState {
    private static final PhoneNetworkState INSTANCE =
            new PhoneNetworkState();

    private volatile WifiStatus wifi =
            WifiStatus.initial();

    private volatile CellularStatus cellular =
            CellularStatus.initial();

    private volatile List<VisibleWifiNetwork> visibleWifiNetworks =
            List.of();

    private int batteryPercent = 87;

    private PhoneNetworkState() {
    }

    public static PhoneNetworkState get() {
        return INSTANCE;
    }

    public WifiStatus getWifi() {
        return wifi;
    }

    public CellularStatus getCellular() {
        return cellular;
    }

    public List<VisibleWifiNetwork> getVisibleWifiNetworks() {
        return visibleWifiNetworks;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = Math.max(0, Math.min(100, batteryPercent));
    }

    public boolean isWifiUsable() {
        WifiStatus value = wifi;
        return value.enabled()
                && value.connected()
                && value.rssiDbm() >= -90
                && value.sinrDb() >= -5.0
                && "CONNECTED".equalsIgnoreCase(value.stage());
    }

    public boolean isCellularUsable() {
        CellularStatus value = cellular;
        return PhoneSubscriberClientState.get().canUseCellularData()
                && value.enabled()
                && value.registered()
                && "ACTIVE".equalsIgnoreCase(value.pduState())
                && value.rsrpDbm() >= -120
                && value.sinrDb() >= -6.0;
    }

    public boolean hasCellularRadioCoverage() {
        CellularStatus value = cellular;
        return value.enabled()
                && value.registered()
                && value.rsrpDbm() >= -125
                && value.sinrDb() >= -8.0;
    }

    public void applyWirelessSnapshot(PhoneWirelessSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        visibleWifiNetworks = snapshot.wifiNetworks()
                .stream()
                .map(value -> new VisibleWifiNetwork(
                        value.ssid(),
                        value.bssid(),
                        value.security(),
                        value.locked(),
                        value.rssiDbm(),
                        value.sinrDb(),
                        value.channel(),
                        value.frequencyHz(),
                        value.phy(),
                        value.distanceBlocks(),
                        value.quality()
                ))
                .toList();

        PhoneWirelessSnapshot.WifiStatus serverWifi = snapshot.wifi();
        wifi = new WifiStatus(
                serverWifi.enabled(),
                serverWifi.connected(),
                serverWifi.stage(),
                serverWifi.ssid(),
                serverWifi.bssid(),
                serverWifi.security(),
                serverWifi.rssiDbm(),
                serverWifi.sinrDb(),
                serverWifi.channel(),
                serverWifi.frequencyHz(),
                serverWifi.phy(),
                serverWifi.distanceBlocks(),
                serverWifi.quality(),
                serverWifi.ipAddress(),
                serverWifi.subnetMask(),
                serverWifi.gateway(),
                serverWifi.dns(),
                serverWifi.status()
        );

        PhoneWirelessSnapshot.CellularStatus serverCellular = snapshot.cellular();
        cellular = new CellularStatus(
                serverCellular.enabled(),
                serverCellular.registered(),
                serverCellular.carrier(),
                serverCellular.radioLabel(),
                serverCellular.architecture(),
                serverCellular.gnbId(),
                serverCellular.cellId(),
                serverCellular.tac(),
                serverCellular.plmn(),
                serverCellular.band(),
                serverCellular.ipAddress(),
                serverCellular.dnn(),
                serverCellular.fiveQi(),
                serverCellular.rrcState(),
                serverCellular.nasState(),
                serverCellular.pduState(),
                serverCellular.rsrpDbm(),
                serverCellular.rsrqDb(),
                serverCellular.sinrDb(),
                serverCellular.distanceBlocks(),
                serverCellular.quality(),
                serverCellular.estimatedDownlinkMbps(),
                serverCellular.satelliteNtnStatus(),
                serverCellular.status()
        );
    }

    public record VisibleWifiNetwork(
            String ssid,
            String bssid,
            String security,
            boolean locked,
            int rssiDbm,
            double sinrDb,
            int channel,
            double frequencyHz,
            String phy,
            double distanceBlocks,
            String quality
    ) {
        public VisibleWifiNetwork {
            ssid = safe(ssid);
            bssid = safe(bssid);
            security = safe(security);
            phy = safe(phy);
            quality = safe(quality);
        }
    }

    public record WifiStatus(
            boolean enabled,
            boolean connected,
            String stage,
            String ssid,
            String bssid,
            String security,
            int rssiDbm,
            double sinrDb,
            int channel,
            double frequencyHz,
            String phy,
            double distanceBlocks,
            String quality,
            String ipAddress,
            String subnetMask,
            String gateway,
            String dns,
            String status
    ) {
        public WifiStatus {
            stage = safe(stage);
            ssid = safe(ssid);
            bssid = safe(bssid);
            security = safe(security);
            phy = safe(phy);
            quality = safe(quality);
            ipAddress = safe(ipAddress);
            subnetMask = safe(subnetMask);
            gateway = safe(gateway);
            dns = safe(dns);
            status = safe(status);
        }

        private static WifiStatus initial() {
            return new WifiStatus(
                    true,
                    false,
                    "SCANNING",
                    "",
                    "",
                    "",
                    -127,
                    Double.NEGATIVE_INFINITY,
                    0,
                    0.0,
                    "",
                    Double.POSITIVE_INFINITY,
                    "NO SIGNAL",
                    "",
                    "",
                    "",
                    "",
                    "Waiting for server RF scan"
            );
        }
    }

    public record CellularStatus(
            boolean enabled,
            boolean registered,
            String carrier,
            String radioLabel,
            String architecture,
            int gnbId,
            int cellId,
            int tac,
            String plmn,
            String band,
            String ipAddress,
            String dnn,
            int fiveQi,
            String rrcState,
            String nasState,
            String pduState,
            int rsrpDbm,
            double rsrqDb,
            double sinrDb,
            double distanceBlocks,
            String quality,
            double estimatedDownlinkMbps,
            String satelliteNtnStatus,
            String status
    ) {
        public CellularStatus {
            carrier = safe(carrier);
            radioLabel = safe(radioLabel);
            architecture = safe(architecture);
            plmn = safe(plmn);
            band = safe(band);
            ipAddress = safe(ipAddress);
            dnn = safe(dnn);
            rrcState = safe(rrcState);
            nasState = safe(nasState);
            pduState = safe(pduState);
            quality = safe(quality);
            satelliteNtnStatus = safe(satelliteNtnStatus);
            status = safe(status);
        }

        private static CellularStatus initial() {
            return new CellularStatus(
                    true,
                    false,
                    "No Service",
                    "",
                    "",
                    0,
                    0,
                    0,
                    "",
                    "",
                    "",
                    "",
                    0,
                    "IDLE",
                    "DEREGISTERED",
                    "INACTIVE",
                    -140,
                    -30.0,
                    -30.0,
                    Double.POSITIVE_INFINITY,
                    "NO SERVICE",
                    0.0,
                    "Not provisioned - ground-terminal SATCOM backhaul only",
                    "Waiting for cellular scan"
            );
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
