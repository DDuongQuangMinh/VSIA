package com.k1ngtle.vsia.phone.network.realism;

import java.util.List;

public record PhoneWirelessSnapshot(
        List<WifiNetwork> wifiNetworks,
        WifiStatus wifi,
        CellularStatus cellular
) {
    public PhoneWirelessSnapshot {
        wifiNetworks =
                wifiNetworks == null
                        ? List.of()
                        : List.copyOf(
                        wifiNetworks
                );

        wifi =
                wifi == null
                        ? WifiStatus.off()
                        : wifi;

        cellular =
                cellular == null
                        ? CellularStatus.off()
                        : cellular;
    }

    public record WifiNetwork(
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
        public WifiNetwork {
            ssid =
                    safe(
                            ssid
                    );

            bssid =
                    safe(
                            bssid
                    );

            security =
                    safe(
                            security
                    );

            phy =
                    safe(
                            phy
                    );

            quality =
                    safe(
                            quality
                    );
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
            stage =
                    safe(
                            stage
                    );

            ssid =
                    safe(
                            ssid
                    );

            bssid =
                    safe(
                            bssid
                    );

            security =
                    safe(
                            security
                    );

            phy =
                    safe(
                            phy
                    );

            quality =
                    safe(
                            quality
                    );

            ipAddress =
                    safe(
                            ipAddress
                    );

            subnetMask =
                    safe(
                            subnetMask
                    );

            gateway =
                    safe(
                            gateway
                    );

            dns =
                    safe(
                            dns
                    );

            status =
                    safe(
                            status
                    );
        }

        public static WifiStatus off() {
            return new WifiStatus(
                    false,
                    false,
                    "IDLE",
                    "",
                    "",
                    "",
                    -127,
                    Double.NEGATIVE_INFINITY,
                    0,
                    0.0,
                    "",
                    Double.POSITIVE_INFINITY,
                    "OFF",
                    "",
                    "",
                    "",
                    "",
                    "Wi-Fi is off"
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
            carrier =
                    safe(
                            carrier
                    );

            radioLabel =
                    safe(
                            radioLabel
                    );

            architecture =
                    safe(
                            architecture
                    );

            plmn =
                    safe(
                            plmn
                    );

            band =
                    safe(
                            band
                    );

            ipAddress =
                    safe(
                            ipAddress
                    );

            dnn =
                    safe(
                            dnn
                    );

            rrcState =
                    safe(
                            rrcState
                    );

            nasState =
                    safe(
                            nasState
                    );

            pduState =
                    safe(
                            pduState
                    );

            quality =
                    safe(
                            quality
                    );

            satelliteNtnStatus =
                    safe(
                            satelliteNtnStatus
                    );

            status =
                    safe(
                            status
                    );
        }

        public static CellularStatus off() {
            return new CellularStatus(
                    false,
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
                    "OFF",
                    0.0,
                    "Not provisioned",
                    "Cellular data is off"
            );
        }
    }

    private static String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }
}
