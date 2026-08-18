param(
    [string]$ProjectRoot = "."
)

$relative = "src\main\java\com\k1ngtle\vsia\signality\internet\NetworkDeviceBlockEntity.java"
$path = Join-Path $ProjectRoot $relative

if (-not (Test-Path $path)) {
    throw "NetworkDeviceBlockEntity.java not found at $path"
}

$text = [System.IO.File]::ReadAllText($path)
$originalNewline = if ($text.Contains("`r`n")) { "`r`n" } else { "`n" }
$text = $text.Replace("`r`n", "`n").Replace("`r", "`n")

if ($text.Contains("wifiRawArpPeers")) {
    Write-Host "W1.10.1 raw ARP integration already present."
    exit 0
}

$backup = "$path.w1.10.1.bak"

if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path, $backup)
}

function Replace-Exact {
    param(
        [string]$Label,
        [string]$Old,
        [string]$New
    )

    if (-not $script:text.Contains($Old)) {
        throw "W1.10.1 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace(
        $Old,
        $New
    )
}

Replace-Exact "ARP carrier import" @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpFlowSnapshot;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpFlowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.arp.live.ArpRawLiveCarrierCodec;
'@

Replace-Exact "raw ARP peer set" @'
    private final Set<String> wifiRawTcpSessions =
            new HashSet<>();

    private final CellularRanController cellularRan =
'@ @'
    private final Set<String> wifiRawTcpSessions =
            new HashSet<>();

    private final Set<String> wifiRawArpPeers =
            new HashSet<>();

    private final CellularRanController cellularRan =
'@

Replace-Exact "mode clear ARP peers" @'
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();

        wifiIpApplication.setStatus(
'@ @'
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();

        wifiIpApplication.setStatus(
'@

Replace-Exact "clear live ARP peers" @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
    }
'@ @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
    }
'@

Replace-Exact "raw ARP receive" @'
        if (data != null
                && TcpRawLiveCarrierCodec.isRawCarrier(
                data
        )) {
'@ @'
        if (data != null
                && ArpRawLiveCarrierCodec.isRawArpCarrier(
                data
        )) {
            try {
                OSINetworkPacket rawArp =
                        ArpRawLiveCarrierCodec.decode(
                                data
                        );

                if (rawArp.sourceMac != null
                        && !rawArp.sourceMac.isBlank()) {
                    wifiRawArpPeers.add(
                            rawArp.sourceMac
                    );
                }

                processLayer2(
                        rawArp
                );
            } catch (Exception exception) {
                wifiIpApplication.setStatus(
                        "RAW ARP drop: "
                                + exception.getMessage()
                );
            }
        } else if (data != null
                && TcpRawLiveCarrierCodec.isRawCarrier(
                data
        )) {
'@

Replace-Exact "raw ARP transmit before TCP" @'
        if (shouldUseRawWifiTcpCarrier(
                packet
        )) {
'@ @'
        if (shouldUseRawWifiArpCarrier(
                packet
        )) {
            CompoundTag body =
                    ArpRawLiveCarrierCodec.encode(
                            packet
                    );

            if (packet.targetMac != null
                    && !packet.targetMac.isBlank()
                    && !"FF:FF:FF:FF:FF:FF".equalsIgnoreCase(
                    packet.targetMac
            )) {
                wifiRawArpPeers.add(
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

        if (shouldUseRawWifiTcpCarrier(
                packet
        )) {
'@

Replace-Exact "raw ARP helper" @'
    private boolean shouldUseRawWifiTcpCarrier(
            OSINetworkPacket packet
    ) {
'@ @'
    private boolean shouldUseRawWifiArpCarrier(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT
                || !"ARP".equalsIgnoreCase(
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
                && wifiRawArpPeers.contains(
                packet.targetMac
        );
    }

    private boolean shouldUseRawWifiTcpCarrier(
            OSINetworkPacket packet
    ) {
'@

Replace-Exact "load clears ARP peers" @'
        wifiRawTcpSessions.clear();

        if (tag.contains("WifiMac")) {
'@ @'
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();

        if (tag.contains("WifiMac")) {
'@

if ($originalNewline -eq "`r`n") {
    $text = $text.Replace("`n", "`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)

[System.IO.File]::WriteAllText(
    $path,
    $text,
    $utf8
)

Write-Host "W1.10.1 raw ARP patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
