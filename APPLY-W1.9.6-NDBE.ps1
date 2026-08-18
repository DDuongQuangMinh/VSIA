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

if ($text.Contains("public ExecutionMode wifiTcpExecutionMode()")) {
    Write-Host "W1.9.6 NetworkDeviceBlockEntity integration already present."
    exit 0
}

$backup = "$path.w1.9.6.bak"

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
        throw "W1.9.6 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace(
        $Old,
        $New
    )
}

Replace-Exact "ExecutionMode import" @'
import com.k1ngtle.vsia.signality.engineering.EngineeringPhyEngine;
'@ @'
import com.k1ngtle.vsia.signality.engineering.EngineeringPhyEngine;
import com.k1ngtle.vsia.signality.engineering.ExecutionMode;
'@

Replace-Exact "raw carrier import" @'
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveSnapshot;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.raw.live.TcpRawLiveCarrierCodec;
'@

Replace-Exact "HashSet imports" @'
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
'@ @'
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
'@

Replace-Exact "TCP execution-mode fields" @'
    private final TcpLiveController wifiTcpLive =
            new TcpLiveController();

    private final CellularRanController cellularRan =
'@ @'
    private final TcpLiveController wifiTcpLive =
            new TcpLiveController();

    private ExecutionMode wifiTcpExecutionMode =
            ExecutionMode.SIMULATION;

    private final Set<String> wifiRawTcpSessions =
            new HashSet<>();

    private final CellularRanController cellularRan =
'@

Replace-Exact "TCP execution-mode API" @'
    public TcpLiveSnapshot wifiTcpLiveSnapshot() {
        return wifiTcpLive.snapshot();
    }

    public boolean startWifiTcpHttpGet(
'@ @'
    public TcpLiveSnapshot wifiTcpLiveSnapshot() {
        return wifiTcpLive.snapshot();
    }

    public ExecutionMode wifiTcpExecutionMode() {
        return wifiTcpExecutionMode;
    }

    public void setWifiTcpExecutionMode(
            ExecutionMode mode
    ) {
        ExecutionMode next =
                mode == null
                        ? ExecutionMode.SIMULATION
                        : mode;

        if (wifiTcpExecutionMode == next) {
            return;
        }

        wifiTcpExecutionMode =
                next;

        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();

        wifiIpApplication.setStatus(
                "TCP carrier mode "
                        + wifiTcpExecutionMode
        );

        setChanged();
    }

    public boolean startWifiTcpHttpGet(
'@

Replace-Exact "clear raw sessions" @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
    }
'@ @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
    }
'@

Replace-Exact "raw receive path" @'
        if (data != null
                && data.contains(
                "osi_packet"
        )) {
            processLayer2(
                    OSINetworkPacket.deserializeNBT(
                            data.getCompound(
                                    "osi_packet"
                            )
                    )
            );
        }

        PduSession session =
'@ @'
        if (data != null
                && TcpRawLiveCarrierCodec.isRawCarrier(
                data
        )) {
            try {
                OSINetworkPacket rawTcp =
                        TcpRawLiveCarrierCodec.decode(
                                data
                        );

                if (rawTcp.sessionId != null
                        && !rawTcp.sessionId.isBlank()) {
                    wifiRawTcpSessions.add(
                            rawTcp.sessionId
                    );
                }

                processLayer2(
                        rawTcp
                );
            } catch (Exception exception) {
                wifiIpApplication.setStatus(
                        "RAW IPv4/TCP drop: "
                                + exception.getMessage()
                );
            }
        } else if (data != null
                && data.contains(
                "osi_packet"
        )) {
            processLayer2(
                    OSINetworkPacket.deserializeNBT(
                            data.getCompound(
                                    "osi_packet"
                            )
                    )
            );
        }

        PduSession session =
'@

Replace-Exact "raw transmit path" @'
        if (protocolVm.bound()) {
            CompoundTag hostPayload =
'@ @'
        if (shouldUseRawWifiTcpCarrier(
                packet
        )) {
            CompoundTag body =
                    TcpRawLiveCarrierCodec.encode(
                            packet
                    );

            if (packet.sessionId != null
                    && !packet.sessionId.isBlank()) {
                wifiRawTcpSessions.add(
                        packet.sessionId
                );
            }

            wifiMac.sendData(
                    macAddress,
                    packet.targetMac,
                    body,
                    classifyAccessCategory(
                            packet
                    ),
                    wifiSender()
            );

            return;
        }

        if (protocolVm.bound()) {
            CompoundTag hostPayload =
'@

Replace-Exact "raw carrier helper" @'
    private long estimatePacketBits(
            OSINetworkPacket packet
    ) {
'@ @'
    private boolean shouldUseRawWifiTcpCarrier(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT
                || !"TCP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            return false;
        }

        if (wifiTcpExecutionMode
                == ExecutionMode.CONFORMANCE) {
            return true;
        }

        return packet.sessionId != null
                && !packet.sessionId.isBlank()
                && wifiRawTcpSessions.contains(
                packet.sessionId
        );
    }

    private long estimatePacketBits(
            OSINetworkPacket packet
    ) {
'@

Replace-Exact "save TCP execution mode" @'
        tag.putString(
                "WifiLivePhyMode",
                wifiLivePhyMode.name()
        );

        tag.put(
                "WifiMac",
'@ @'
        tag.putString(
                "WifiLivePhyMode",
                wifiLivePhyMode.name()
        );

        tag.putString(
                "WifiTcpExecutionMode",
                wifiTcpExecutionMode.name()
        );

        tag.put(
                "WifiMac",
'@

Replace-Exact "load TCP execution mode" @'
        if (tag.contains(
                "WifiLivePhyMode"
        )) {
            try {
                wifiLivePhyMode =
                        WifiLivePhyMode.valueOf(
                                tag.getString(
                                        "WifiLivePhyMode"
                                )
                        );
            } catch (Exception ignored) {
                wifiLivePhyMode =
                        WifiLivePhyMode.ANALYTICAL;
            }
        }

        if (tag.contains("WifiMac")) {
'@ @'
        if (tag.contains(
                "WifiLivePhyMode"
        )) {
            try {
                wifiLivePhyMode =
                        WifiLivePhyMode.valueOf(
                                tag.getString(
                                        "WifiLivePhyMode"
                                )
                        );
            } catch (Exception ignored) {
                wifiLivePhyMode =
                        WifiLivePhyMode.ANALYTICAL;
            }
        }

        if (tag.contains(
                "WifiTcpExecutionMode"
        )) {
            try {
                wifiTcpExecutionMode =
                        ExecutionMode.valueOf(
                                tag.getString(
                                        "WifiTcpExecutionMode"
                                )
                        );
            } catch (Exception ignored) {
                wifiTcpExecutionMode =
                        ExecutionMode.SIMULATION;
            }
        } else {
            wifiTcpExecutionMode =
                    ExecutionMode.SIMULATION;
        }

        wifiRawTcpSessions.clear();

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

Write-Host "W1.9.6 patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
