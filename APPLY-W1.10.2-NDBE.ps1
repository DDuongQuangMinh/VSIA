param(
    [string]$ProjectRoot = "."
)

$path = Join-Path $ProjectRoot "src\main\java\com\k1ngtle\vsia\signality\internet\NetworkDeviceBlockEntity.java"

if (-not (Test-Path $path)) {
    throw "NetworkDeviceBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$nl = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n","`n").Replace("`r","`n")

if ($text.Contains("wifiDhcpClientState")) {
    Write-Host "W1.10.2 NDBE integration already present."
    exit 0
}

$backup="$path.w1.10.2.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path,$backup)
}

function Replace-Exact {
    param([string]$Label,[string]$Old,[string]$New)
    if (-not $script:text.Contains($Old)) {
        throw "W1.10.2 NDBE patch anchor not found: $Label"
    }
    $script:text=$script:text.Replace($Old,$New)
}

Replace-Exact "DHCP imports" @'
import com.k1ngtle.vsia.signality.engineering.wifi.arp.live.ArpRawLiveCarrierCodec;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.arp.live.ArpRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.DhcpClientState;
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.live.DhcpRawLiveCarrierCodec;
'@

Replace-Exact "DHCP state fields" @'
    private final Set<String> wifiRawArpPeers =
            new HashSet<>();

    private final CellularRanController cellularRan =
'@ @'
    private final Set<String> wifiRawArpPeers =
            new HashSet<>();

    private final Set<String> wifiRawDhcpPeers =
            new HashSet<>();

    private DhcpClientState wifiDhcpClientState =
            DhcpClientState.IDLE;

    private int wifiDhcpTransactionId;

    private String wifiDhcpOfferedIp = "";

    private String wifiDhcpServerIdentifier = "";

    private final CellularRanController cellularRan =
'@

Replace-Exact "DHCP raw receive" @'
        if (data != null
                && ArpRawLiveCarrierCodec.isRawArpCarrier(
                data
        )) {
'@ @'
        if (data != null
                && DhcpRawLiveCarrierCodec.isRawDhcpCarrier(
                data
        )) {
            try {
                OSINetworkPacket rawDhcp =
                        DhcpRawLiveCarrierCodec.decode(
                                data
                        );

                if (rawDhcp.sourceMac != null
                        && !rawDhcp.sourceMac.isBlank()) {
                    wifiRawDhcpPeers.add(
                            rawDhcp.sourceMac
                    );
                }

                processLayer2(
                        rawDhcp
                );
            } catch (Exception exception) {
                wifiIpApplication.setStatus(
                        "RAW DHCP drop: "
                                + exception.getMessage()
                );
            }
        } else if (data != null
                && ArpRawLiveCarrierCodec.isRawArpCarrier(
                data
        )) {
'@

Replace-Exact "DHCP response state machine" @'
    protected void handleDhcpResponse(
            OSINetworkPacket packet
    ) {
        if (packet.applicationProtocol
                .equals("DHCP")
                && packet.payload
                .getString("type")
                .equals("ACK")) {

            ipAddress =
                    packet.payload
                            .getString(
                                    "assigned_ip"
                            );

            defaultGatewayMac =
                    packet.sourceMac;

            wifiIpApplication.setStatus(
                    "DHCP ACK received: "
                            + ipAddress
            );

            setChanged();
        }
    }

    public void requestDynamicIp() {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac = macAddress;
        packet.targetMac =
                "FF:FF:FF:FF:FF:FF";

        packet.sourceIp = "0.0.0.0";
        packet.targetIp =
                "255.255.255.255";

        packet.sourcePort = 68;
        packet.targetPort = 67;

        packet.applicationProtocol =
                "DHCP";

        packet.payload.putString(
                "type",
                "DISCOVER"
        );

        wifiIpApplication.setStatus(
                "DHCP DISCOVER queued"
        );

        transmitPacket(packet);
    }
'@ @'
    protected void handleDhcpResponse(
            OSINetworkPacket packet
    ) {
        if (!"DHCP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            return;
        }

        String type =
                packet.payload.getString(
                        "type"
                );

        int xid =
                packet.payload.getInt(
                        "xid"
                );

        if (xid != wifiDhcpTransactionId) {
            wifiIpApplication.setStatus(
                    "DHCP ignored: transaction ID mismatch"
            );
            return;
        }

        if ("OFFER".equalsIgnoreCase(
                type
        )) {
            wifiDhcpOfferedIp =
                    packet.payload.getString(
                            "assigned_ip"
                    );

            wifiDhcpServerIdentifier =
                    packet.payload.getString(
                            "server_identifier"
                    );

            wifiDhcpClientState =
                    DhcpClientState.REQUESTING;

            OSINetworkPacket request =
                    new OSINetworkPacket();

            request.sourceMac =
                    macAddress;

            request.targetMac =
                    "FF:FF:FF:FF:FF:FF";

            request.sourceIp =
                    "0.0.0.0";

            request.targetIp =
                    "255.255.255.255";

            request.sourcePort = 68;
            request.targetPort = 67;
            request.ipProtocol = 17;
            request.applicationProtocol =
                    "DHCP";

            request.payload.putString(
                    "type",
                    "REQUEST"
            );

            request.payload.putInt(
                    "xid",
                    wifiDhcpTransactionId
            );

            request.payload.putString(
                    "requested_ip",
                    wifiDhcpOfferedIp
            );

            request.payload.putString(
                    "server_identifier",
                    wifiDhcpServerIdentifier
            );

            wifiIpApplication.setStatus(
                    "DHCP OFFER "
                            + wifiDhcpOfferedIp
                            + " -> REQUEST"
            );

            transmitPacket(
                    request
            );

            return;
        }

        if ("ACK".equalsIgnoreCase(
                type
        )) {
            ipAddress =
                    packet.payload.getString(
                            "assigned_ip"
                    );

            defaultGatewayMac =
                    packet.sourceMac;

            wifiDhcpClientState =
                    DhcpClientState.BOUND;

            wifiIpApplication.setStatus(
                    "DHCP DORA complete: "
                            + ipAddress
            );

            setChanged();
            return;
        }

        if ("NAK".equalsIgnoreCase(
                type
        )) {
            wifiDhcpClientState =
                    DhcpClientState.FAILED;

            wifiIpApplication.setStatus(
                    "DHCP NAK received"
            );
        }
    }

    public void requestDynamicIp() {
        wifiDhcpTransactionId =
                (
                        int
                ) (
                System.nanoTime()
                        ^ signalId.getMostSignificantBits()
                        ^ signalId.getLeastSignificantBits()
        );

        wifiDhcpClientState =
                DhcpClientState.SELECTING;

        wifiDhcpOfferedIp = "";
        wifiDhcpServerIdentifier = "";

        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac = macAddress;
        packet.targetMac =
                "FF:FF:FF:FF:FF:FF";

        packet.sourceIp = "0.0.0.0";
        packet.targetIp =
                "255.255.255.255";

        packet.sourcePort = 68;
        packet.targetPort = 67;
        packet.ipProtocol = 17;

        packet.applicationProtocol =
                "DHCP";

        packet.payload.putString(
                "type",
                "DISCOVER"
        );

        packet.payload.putInt(
                "xid",
                wifiDhcpTransactionId
        );

        wifiIpApplication.setStatus(
                "DHCP DISCOVER queued | xid "
                        + Integer.toUnsignedString(
                        wifiDhcpTransactionId
                )
        );

        transmitPacket(packet);
    }
'@

Replace-Exact "DHCP transmit before ARP" @'
        if (shouldUseRawWifiArpCarrier(
                packet
        )) {
'@ @'
        if (shouldUseRawWifiDhcpCarrier(
                packet
        )) {
            CompoundTag body =
                    DhcpRawLiveCarrierCodec.encode(
                            packet
                    );

            if (packet.targetMac != null
                    && !packet.targetMac.isBlank()
                    && !"FF:FF:FF:FF:FF:FF".equalsIgnoreCase(
                    packet.targetMac
            )) {
                wifiRawDhcpPeers.add(
                        packet.targetMac
                );
            }

            wifiMac.sendData(
                    macAddress,
                    packet.targetMac,
                    body,
                    WifiAccessCategory.BEST_EFFORT,
                    wifiSender()
            );

            return;
        }

        if (shouldUseRawWifiArpCarrier(
                packet
        )) {
'@

Replace-Exact "DHCP helper before ARP helper" @'
    private boolean shouldUseRawWifiArpCarrier(
            OSINetworkPacket packet
    ) {
'@ @'
    private boolean shouldUseRawWifiDhcpCarrier(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT
                || !"DHCP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            return false;
        }

        if (wifiTcpExecutionMode
                == ExecutionMode.CONFORMANCE) {
            return true;
        }

        return packet.targetMac != null
                && !packet.targetMac.isBlank()
                && wifiRawDhcpPeers.contains(
                packet.targetMac
        );
    }

    private boolean shouldUseRawWifiArpCarrier(
            OSINetworkPacket packet
    ) {
'@

if ($nl -eq "`r`n") {
    $text=$text.Replace("`n","`r`n")
}

$utf8=New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.2 NDBE patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
