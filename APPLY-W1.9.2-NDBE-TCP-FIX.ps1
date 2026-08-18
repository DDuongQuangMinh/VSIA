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

if ($text.Contains("public TcpLiveSnapshot wifiTcpLiveSnapshot()")) {
    Write-Host "W1.9.2 NDBE TCP integration already present. No change needed."
    exit 0
}

$backup = "$path.w1.9.2-tcp.bak"

if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy(
        $path,
        $backup
    )
}

function Replace-Exact {
    param(
        [string]$Label,
        [string]$Old,
        [string]$New
    )

    if (-not $script:text.Contains($Old)) {
        throw "W1.9.2 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace(
        $Old,
        $New
    )
}

Replace-Exact "TCP imports" @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpFlowSnapshot;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpFlowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveController;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveScheduler;
import com.k1ngtle.vsia.signality.engineering.wifi.tcp.live.TcpLiveSnapshot;
'@

Replace-Exact "TCP controller field" @'
    private final WifiIpApplicationEngine wifiIpApplication =
            new WifiIpApplicationEngine();
'@ @'
    private final WifiIpApplicationEngine wifiIpApplication =
            new WifiIpApplicationEngine();

    private final TcpLiveController wifiTcpLive =
            new TcpLiveController();
'@

Replace-Exact "TCP scheduler registration" @'
            ProtocolVmScheduler.register(
                    signalId,
                    protocolVm
            );
'@ @'
            ProtocolVmScheduler.register(
                    signalId,
                    protocolVm
            );

            TcpLiveScheduler.register(
                    signalId,
                    () -> {
                        if (level instanceof ServerLevel serverLevel) {
                            wifiTcpLive.tick(
                                    NetworkTimebase.nowMicros(
                                            serverLevel
                                    ),
                                    this::transmitPacket
                            );
                        }
                    }
            );
'@

Replace-Exact "TCP scheduler unregister" @'
        ProtocolVmScheduler.unregister(
                signalId
        );
'@ @'
        ProtocolVmScheduler.unregister(
                signalId
        );

        TcpLiveScheduler.unregister(
                signalId
        );
'@

Replace-Exact "TCP public API" @'
    public String wifiSecurityDiagnostic() {
        return wifiMac.lastSecurityDiagnostic();
    }
'@ @'
    public String wifiSecurityDiagnostic() {
        return wifiMac.lastSecurityDiagnostic();
    }

    public TcpLiveSnapshot wifiTcpLiveSnapshot() {
        return wifiTcpLive.snapshot();
    }

    public boolean startWifiTcpHttpGet(
            String targetMac,
            String targetIp,
            String path
    ) {
        if (!wifiIpReady()) {
            wifiIpApplication.setStatus(
                    "TCP HTTP rejected: Wi-Fi is not associated/secured"
            );

            return false;
        }

        if (!com.k1ngtle.vsia.signality.engineering.wifi.ip.Ipv4Address
                .isUsableUnicast(
                        targetIp
                )) {
            wifiIpApplication.setStatus(
                    "TCP HTTP rejected: peer IPv4 is invalid"
            );

            return false;
        }

        long nowMicros =
                NetworkTimebase.nowMicros(
                        level()
                );

        OSINetworkPacket application =
                wifiIpApplication.createHttpGet(
                        macAddress,
                        ipAddress,
                        targetMac,
                        targetIp,
                        path,
                        nowMicros
                );

        boolean started =
                wifiTcpLive.startApplication(
                        application,
                        nowMicros,
                        this::transmitPacket
                );

        if (started) {
            wifiIpApplication.setStatus(
                    "TCP HTTP connection started"
            );
        }

        return started;
    }

    public boolean closeWifiTcpLive() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        return wifiTcpLive.closeActive(
                NetworkTimebase.nowMicros(
                        serverLevel
                ),
                this::transmitPacket
        );
    }

    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
    }
'@

Replace-Exact "TCP Layer 4 dispatch" @'
    protected void processLayer4(
            OSINetworkPacket packet
    ) {
        if (isWifiProfile()
'@ @'
    protected void processLayer4(
            OSINetworkPacket packet
    ) {
        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT
                && "TCP".equalsIgnoreCase(
                packet.applicationProtocol
        )) {
            wifiTcpLive.handleIncoming(
                    macAddress,
                    ipAddress,
                    packet,
                    level instanceof ServerLevel serverLevel
                            ? NetworkTimebase.nowMicros(
                            serverLevel
                    )
                            : 0L,
                    this::transmitPacket,
                    this::deliverTcpApplication
            );

            setChanged();
            return;
        }

        if (isWifiProfile()
'@

Replace-Exact "TCP application delivery" @'
    protected void handleWebRequest(
            OSINetworkPacket packet
    ) {
'@ @'
    private void deliverTcpApplication(
            OSINetworkPacket application
    ) {
        if (application == null) {
            return;
        }

        processLayer4(
                application
        );
    }

    protected void handleWebRequest(
            OSINetworkPacket packet
    ) {
'@

Replace-Exact "TCP response interception" @'
    protected void transmitPacket(
            OSINetworkPacket packet
    ) {
        if (protocolVm.bound()) {
'@ @'
    protected void transmitPacket(
            OSINetworkPacket packet
    ) {
        if (packet != null
                && !"TCP".equalsIgnoreCase(
                packet.applicationProtocol
        )
                && packet.isResponse
                && packet.sessionId != null
                && !packet.sessionId.isBlank()
                && level instanceof ServerLevel serverLevel
                && wifiTcpLive.interceptApplicationResponse(
                packet,
                NetworkTimebase.nowMicros(
                        serverLevel
                ),
                this::transmitPacket
        )) {
            return;
        }

        if (protocolVm.bound()) {
'@

if ($originalNewline -eq "`r`n") {
    $text = $text.Replace(
        "`n",
        "`r`n"
    )
}

$utf8 = New-Object System.Text.UTF8Encoding($false)

[System.IO.File]::WriteAllText(
    $path,
    $text,
    $utf8
)

Write-Host "W1.9.2 NDBE TCP integration patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
