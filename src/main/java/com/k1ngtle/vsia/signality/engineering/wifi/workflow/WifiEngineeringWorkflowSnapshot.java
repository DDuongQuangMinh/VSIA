package com.k1ngtle.vsia.signality.engineering.wifi.workflow;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiMode;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityState;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiStationState;

import java.util.List;

public record WifiEngineeringWorkflowSnapshot(
        String macAddress,
        WifiMode mode,
        WifiStationState stationState,
        WifiSecurityState securityState,
        List<String> discoveredSsids,
        List<String> associatedStations,
        int pendingDataTransmissions,
        String status
) {
    public WifiEngineeringWorkflowSnapshot {
        macAddress =
                macAddress == null
                        ? ""
                        : macAddress;

        discoveredSsids =
                discoveredSsids == null
                        ? List.of()
                        : List.copyOf(discoveredSsids);

        associatedStations =
                associatedStations == null
                        ? List.of()
                        : List.copyOf(associatedStations);

        status =
                status == null
                        ? ""
                        : status;
    }
}
