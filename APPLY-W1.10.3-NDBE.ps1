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

if ($text.Contains("sendWifiDnsAQuery(")) {
    Write-Host "W1.10.3 NDBE DNS integration already present."
    exit 0
}

$backup = "$path.w1.10.3.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path,$backup)
}

function Replace-Exact {
    param([string]$Label,[string]$Old,[string]$New)

    if (-not $script:text.Contains($Old)) {
        throw "W1.10.3 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace($Old,$New)
}

Replace-Exact "DNS import" @'
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.live.DhcpRawLiveCarrierCodec;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.dhcp.live.DhcpRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.dns.live.DnsRawLiveCarrierCodec;
'@

Replace-Exact "DNS peer fields" @'
    private final Set<String> wifiRawDhcpPeers =
            new HashSet<>();
'@ @'
    private final Set<String> wifiRawDhcpPeers =
            new HashSet<>();

    private final Set<String> wifiRawDnsPeers =
            new HashSet<>();

    private int wifiDnsTransactionId;

    private String wifiDnsPendingDomain = "";

    private String wifiDnsLastAnswer = "";
'@

Replace-Exact "DNS send API" @'
    public boolean sendWifiHttpGet(
            String targetMac,
            String targetIp,
            String path
    ) {
'@ @'
    public boolean sendWifiDnsAQuery(
            String domain
    ) {
        if (!wifiIpReady()) {
            wifiIpApplication.setStatus(
                    "DNS rejected: Wi-Fi is not associated/secured"
            );

            return false;
        }

        String targetIp =
                wifiIpApplication.peerIp();

        String targetMac =
                wifiIpApplication.peerMac();

        if (targetIp == null
                || targetIp.isBlank()
                || targetMac == null
                || targetMac.isBlank()) {
            wifiIpApplication.setStatus(
                    "DNS rejected: no configured DNS peer"
            );

            return false;
        }

        String normalized =
                domain == null
                        ? ""
                        : domain.trim()
                        .toLowerCase(
                                java.util.Locale.ROOT
                        );

        if (normalized.isBlank()) {
            wifiIpApplication.setStatus(
                    "DNS rejected: empty domain"
            );

            return false;
        }

        wifiDnsTransactionId =
                (
                        int
                ) (
                System.nanoTime()
                        ^ signalId.getLeastSignificantBits()
        )
                        & 0xFFFF;

        wifiDnsPendingDomain =
                normalized;

        wifiDnsLastAnswer =
                "";

        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.sourceMac =
                macAddress;

        packet.targetMac =
                targetMac;

        packet.sourceIp =
                ipAddress;

        packet.targetIp =
                targetIp;

        packet.sourcePort =
                53000
                        + (
                        wifiDnsTransactionId
                                % 1000
                );

        packet.targetPort =
                53;

        packet.ipProtocol =
                17;

        packet.applicationProtocol =
                "DNS";

        packet.payload.putInt(
                "dns_id",
                wifiDnsTransactionId
        );

        packet.payload.putString(
                "domain",
                normalized
        );

        packet.payload.putString(
                "query_type",
                "A"
        );

        wifiIpApplication.setStatus(
                "DNS A query queued: "
                        + normalized
                        + " | id "
                        + wifiDnsTransactionId
        );

        transmitPacket(
                packet
        );

        return true;
    }

    public String wifiDnsLastAnswer() {
        return wifiDnsLastAnswer;
    }

    private void handleWifiDnsResponse(
            OSINetworkPacket packet
    ) {
        int id =
                packet.payload.getInt(
                        "dns_id"
                );

        if (id != wifiDnsTransactionId) {
            wifiIpApplication.setStatus(
                    "DNS ignored: transaction ID mismatch"
            );

            return;
        }

        int rcode =
                packet.payload.getInt(
                        "rcode"
                );

        String domain =
                packet.payload.getString(
                        "domain"
                );

        if (rcode == 3) {
            wifiDnsLastAnswer =
                    "";

            wifiIpApplication.setStatus(
                    "DNS NXDOMAIN: "
                            + domain
            );

            return;
        }

        String answer =
                packet.payload.contains(
                        "answer"
                )
                        ? packet.payload.getString(
                        "answer"
                )
                        : packet.payload.getString(
                        "resolved_ip"
                );

        wifiDnsLastAnswer =
                answer;

        wifiIpApplication.setStatus(
                "DNS resolved "
                        + domain
                        + " -> "
                        + answer
                        + " | TTL "
                        + packet.payload.getInt(
                        "ttl"
                )
                        + "s"
        );
    }

    public boolean sendWifiHttpGet(
            String targetMac,
            String targetIp,
            String path
    ) {
'@

Replace-Exact "DNS response dispatch" @'
        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT
                && wifiIpApplication.handleIncoming(
'@ @'
        if ("DNS".equalsIgnoreCase(
                packet.applicationProtocol
        )
                && packet.isResponse) {
            handleWifiDnsResponse(
                    packet
            );

            setChanged();
            return;
        }

        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT
                && wifiIpApplication.handleIncoming(
'@

Replace-Exact "DNS raw receive" @'
        if (data != null
                && DhcpRawLiveCarrierCodec.isRawDhcpCarrier(
                data
        )) {
'@ @'
        if (data != null
                && DnsRawLiveCarrierCodec.isRawDnsCarrier(
                data
        )) {
            try {
                OSINetworkPacket rawDns =
                        DnsRawLiveCarrierCodec.decode(
                                data
                        );

                if (rawDns.sourceMac != null
                        && !rawDns.sourceMac.isBlank()) {
                    wifiRawDnsPeers.add(
                            rawDns.sourceMac
                    );
                }

                processLayer2(
                        rawDns
                );
            } catch (Exception exception) {
                wifiIpApplication.setStatus(
                        "RAW DNS drop: "
                                + exception.getMessage()
                );
            }
        } else if (data != null
                && DhcpRawLiveCarrierCodec.isRawDhcpCarrier(
                data
        )) {
'@

Replace-Exact "DNS raw transmit" @'
        if (shouldUseRawWifiDhcpCarrier(
                packet
        )) {
'@ @'
        if (shouldUseRawWifiDnsCarrier(
                packet
        )) {
            CompoundTag body =
                    DnsRawLiveCarrierCodec.encode(
                            packet
                    );

            if (packet.targetMac != null
                    && !packet.targetMac.isBlank()) {
                wifiRawDnsPeers.add(
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

        if (shouldUseRawWifiDhcpCarrier(
                packet
        )) {
'@

Replace-Exact "DNS raw helper" @'
    private boolean shouldUseRawWifiDhcpCarrier(
            OSINetworkPacket packet
    ) {
'@ @'
    private boolean shouldUseRawWifiDnsCarrier(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT
                || !"DNS".equalsIgnoreCase(
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
                && wifiRawDnsPeers.contains(
                packet.targetMac
        );
    }

    private boolean shouldUseRawWifiDhcpCarrier(
            OSINetworkPacket packet
    ) {
'@

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n","`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.3 NDBE DNS patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
