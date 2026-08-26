package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

public final class W119ApBridgeEngine {
    private final String apMacAddress;

    private final W119StationTable stations =
            new W119StationTable();

    private final W119BridgeTable bridgeTable =
            new W119BridgeTable();

    private boolean clientIsolation = false;

    private long wirelessRx = 0L;
    private long wirelessTx = 0L;
    private long distributionRx = 0L;
    private long distributionTx = 0L;
    private long localDeliveries = 0L;
    private long groupFloods = 0L;
    private long wirelessSwitches = 0L;
    private long unknownUnicastDrops = 0L;
    private long invalidDirectionDrops = 0L;
    private long isolationDrops = 0L;
    private long distributionFailures = 0L;
    private String lastDecision = "READY";

    public W119ApBridgeEngine(
            String apMacAddress
    ) {
        this.apMacAddress =
                W119Mac.normalize(
                        apMacAddress
                );
    }

    public W119BridgeDecision wirelessIngress(
            OSINetworkPacket packet,
            boolean toDs,
            boolean fromDs,
            long nowMillis
    ) {
        wirelessRx++;

        if (packet == null) {
            return dropInvalid(
                    "",
                    "",
                    "NULL_WIRELESS_PACKET"
            );
        }

        String source =
                W119Mac.normalize(
                        packet.sourceMac
                );

        String destination =
                W119Mac.normalize(
                        packet.targetMac
                );

        if (toDs && fromDs) {
            return dropInvalid(
                    source,
                    destination,
                    "WDS_FOUR_ADDRESS_UNSUPPORTED"
            );
        }

        if (!toDs || fromDs) {
            return dropInvalid(
                    source,
                    destination,
                    "INVALID_STA_TO_AP_DIRECTION"
            );
        }

        if (source.isBlank()
                || W119Mac.isGroup(source)) {
            return dropInvalid(
                    source,
                    destination,
                    "INVALID_STATION_SOURCE"
            );
        }

        stations.learn(
                source,
                nowMillis
        );

        bridgeTable.learn(
                source,
                W119BridgePort.WIRELESS,
                nowMillis
        );

        if (W119Mac.equals(
                destination,
                apMacAddress
        )) {
            localDeliveries++;

            return decision(
                    W119BridgeAction.LOCAL,
                    "AP_LOCAL_DESTINATION",
                    source,
                    destination
            );
        }

        if (W119Mac.isGroup(destination)) {
            groupFloods++;

            return decision(
                    W119BridgeAction.TO_DISTRIBUTION_SYSTEM,
                    "WIRELESS_GROUP_TO_DS",
                    source,
                    destination
            );
        }

        if (stations.contains(
                destination,
                nowMillis
        )) {
            if (clientIsolation) {
                isolationDrops++;

                return decision(
                        W119BridgeAction.DROP,
                        "CLIENT_ISOLATION",
                        source,
                        destination
                );
            }

            wirelessSwitches++;

            return decision(
                    W119BridgeAction.CONTROLLER_HANDLED,
                    "INTRA_BSS_MAC_CONTROLLER",
                    source,
                    destination
            );
        }

        return decision(
                W119BridgeAction.TO_DISTRIBUTION_SYSTEM,
                "WIRELESS_TO_DS",
                source,
                destination
        );
    }

    public W119BridgeDecision distributionIngress(
            OSINetworkPacket packet,
            long nowMillis
    ) {
        distributionRx++;

        if (packet == null) {
            return dropInvalid(
                    "",
                    "",
                    "NULL_DS_PACKET"
            );
        }

        String source =
                W119Mac.normalize(
                        packet.sourceMac
                );

        String destination =
                W119Mac.normalize(
                        packet.targetMac
                );

        if (!source.isBlank()
                && !W119Mac.isGroup(source)) {
            bridgeTable.learn(
                    source,
                    W119BridgePort.DISTRIBUTION_SYSTEM,
                    nowMillis
            );
        }

        if (W119Mac.equals(
                destination,
                apMacAddress
        )) {
            localDeliveries++;

            return decision(
                    W119BridgeAction.LOCAL,
                    "AP_LOCAL_DESTINATION",
                    source,
                    destination
            );
        }

        if (W119Mac.isGroup(destination)) {
            groupFloods++;

            return decision(
                    W119BridgeAction.TO_WIRELESS,
                    "DS_GROUP_TO_BSS",
                    source,
                    destination
            );
        }

        if (stations.contains(
                destination,
                nowMillis
        )) {
            return decision(
                    W119BridgeAction.TO_WIRELESS,
                    "DS_TO_ASSOCIATED_STATION",
                    source,
                    destination
            );
        }

        unknownUnicastDrops++;

        return decision(
                W119BridgeAction.DROP,
                "DS_UNKNOWN_UNICAST_NOT_REFLECTED",
                source,
                destination
        );
    }

    public boolean forgetStation(
            String stationMac
    ) {
        if (stationMac == null
                || stationMac.isBlank()) {
            return false;
        }

        boolean stationRemoved =
                stations.remove(
                        stationMac
                );

        boolean bridgeRemoved =
                bridgeTable.remove(
                        stationMac
                );

        if (stationRemoved
                || bridgeRemoved) {
            lastDecision =
                    "ROAM_RELEASE:"
                            + W119Mac.normalize(
                            stationMac
                    );
        }

        return stationRemoved
                || bridgeRemoved;
    }

    public void noteWirelessTransmit() {
        wirelessTx++;
    }

    public void noteDistributionTransmit() {
        distributionTx++;
    }

    public void noteDistributionFailure() {
        distributionFailures++;
        lastDecision =
                "DS_FORWARD_FAILURE";
    }

    public void setClientIsolation(
            boolean clientIsolation
    ) {
        this.clientIsolation =
                clientIsolation;
    }

    public boolean clientIsolation() {
        return clientIsolation;
    }

    public boolean associated(
            String stationMac,
            long nowMillis
    ) {
        return stations.contains(
                stationMac,
                nowMillis
        );
    }

    public int stationCount(long nowMillis) {
        return stations.size(nowMillis);
    }

    public int bridgeEntryCount(
            long nowMillis
    ) {
        return bridgeTable.size(nowMillis);
    }

    public W119StationTable stationTable() {
        return stations;
    }

    public W119BridgeTable bridgeTable() {
        return bridgeTable;
    }

    public void clearDynamic() {
        stations.clear();
        bridgeTable.clear();

        wirelessRx = 0L;
        wirelessTx = 0L;
        distributionRx = 0L;
        distributionTx = 0L;
        localDeliveries = 0L;
        groupFloods = 0L;
        wirelessSwitches = 0L;
        unknownUnicastDrops = 0L;
        invalidDirectionDrops = 0L;
        isolationDrops = 0L;
        distributionFailures = 0L;
        lastDecision = "READY";
    }

    public String status(long nowMillis) {
        return "W1.19 AP BRIDGE"
                + " ap="
                + apMacAddress
                + " stations="
                + stationCount(nowMillis)
                + " fdb="
                + bridgeEntryCount(nowMillis)
                + " clientIsolation="
                + clientIsolation
                + " wirelessRx="
                + wirelessRx
                + " wirelessTx="
                + wirelessTx
                + " dsRx="
                + distributionRx
                + " dsTx="
                + distributionTx
                + " local="
                + localDeliveries
                + " groupFloods="
                + groupFloods
                + " intraBss="
                + wirelessSwitches
                + " unknownDrops="
                + unknownUnicastDrops
                + " directionDrops="
                + invalidDirectionDrops
                + " isolationDrops="
                + isolationDrops
                + " dsFailures="
                + distributionFailures
                + " last="
                + lastDecision;
    }

    private W119BridgeDecision dropInvalid(
            String source,
            String destination,
            String reason
    ) {
        invalidDirectionDrops++;

        return decision(
                W119BridgeAction.DROP,
                reason,
                source,
                destination
        );
    }

    private W119BridgeDecision decision(
            W119BridgeAction action,
            String reason,
            String source,
            String destination
    ) {
        lastDecision =
                action
                        + ":"
                        + reason;

        return W119BridgeDecision.of(
                action,
                reason,
                source,
                destination,
                W119Mac.isGroup(destination)
        );
    }
}
