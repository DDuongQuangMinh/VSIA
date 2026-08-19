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

if ($text.Contains("wifiIpv4RouteDecision(")) {
    Write-Host "W1.10.5 IPv4 routing integration already present."
    exit 0
}

$backup = "$path.w1.10.5.bak"
if (-not (Test-Path $backup)) {
    [System.IO.File]::Copy($path,$backup)
}

function Replace-Exact {
    param(
        [string]$Label,
        [string]$Old,
        [string]$New
    )

    if (-not $script:text.Contains($Old)) {
        throw "W1.10.5 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace($Old,$New)
}

Replace-Exact "routing imports" @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4Prefix;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4RouteDecision;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4RouteKind;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.routing.Ipv4RoutingTable;
'@

Replace-Exact "routing fields" @'
    protected String macAddress;
    protected String ipAddress = "0.0.0.0";
    protected String defaultGatewayMac = "";

'@ @'
    protected String macAddress;
    protected String ipAddress = "0.0.0.0";
    protected String defaultGatewayMac = "";

    private String wifiSubnetMask =
            "255.255.255.0";

    private String wifiDefaultGatewayIp =
            "";

    private static final int WIFI_PENDING_IPV4_PER_NEXT_HOP =
            8;

    private static final int WIFI_PENDING_IPV4_TOTAL =
            64;

    private final Map<String, java.util.List<OSINetworkPacket>>
            wifiPendingIpv4ByNextHop =
            new java.util.LinkedHashMap<>();

'@

Replace-Exact "routing public API before DNS" @'
    public boolean sendWifiDnsAQuery(
            String domain
    ) {
'@ @'
    public String wifiIpv4SubnetMask() {
        return wifiSubnetMask;
    }

    public String wifiIpv4DefaultGatewayIp() {
        return wifiDefaultGatewayIp;
    }

    public Ipv4RouteDecision wifiIpv4RouteDecision(
            String destinationIp
    ) {
        return wifiIpv4RoutingTable()
                .resolve(
                        destinationIp
                );
    }

    public boolean configureWifiIpv4Routing(
            String subnetMask,
            String defaultGatewayIp
    ) {
        try {
            Ipv4Prefix.prefixLengthFromMask(
                    subnetMask
            );
        } catch (IllegalArgumentException exception) {
            wifiIpApplication.setStatus(
                    "Route config rejected: invalid subnet mask"
            );
            return false;
        }

        String gateway =
                defaultGatewayIp == null
                        ? ""
                        : defaultGatewayIp.trim();

        if (!gateway.isBlank()
                && !Ipv4Prefix.isUsableUnicast(
                gateway
        )) {
            wifiIpApplication.setStatus(
                    "Route config rejected: invalid default gateway"
            );
            return false;
        }

        wifiSubnetMask =
                subnetMask;

        wifiDefaultGatewayIp =
                gateway;

        defaultGatewayMac =
                "";

        wifiPendingIpv4ByNextHop.clear();

        wifiIpApplication.setStatus(
                "IPv4 route config "
                        + ipAddress
                        + "/"
                        + Ipv4Prefix.prefixLengthFromMask(
                        wifiSubnetMask
                )
                        + " gateway "
                        + (
                        wifiDefaultGatewayIp.isBlank()
                                ? "none"
                                : wifiDefaultGatewayIp
                )
        );

        setChanged();
        return true;
    }

    private Ipv4RoutingTable wifiIpv4RoutingTable() {
        try {
            return Ipv4RoutingTable.hostTable(
                    ipAddress,
                    wifiSubnetMask,
                    wifiDefaultGatewayIp
            );
        } catch (IllegalArgumentException ignored) {
            return new Ipv4RoutingTable(
                    java.util.List.of()
            );
        }
    }

    public boolean sendWifiDnsAQuery(
            String domain
    ) {
'@

Replace-Exact "ARP reply route flush" @'
            if ("ARP".equalsIgnoreCase(
                    packet.applicationProtocol
            )
                    && "REPLY".equalsIgnoreCase(
                    packet.payload.getString(
                            "operation"
                    )
            )) {
                wifiRawIpWorkflow.onArpResolved(
'@ @'
            if ("ARP".equalsIgnoreCase(
                    packet.applicationProtocol
            )
                    && "REPLY".equalsIgnoreCase(
                    packet.payload.getString(
                            "operation"
                    )
            )) {
                String resolvedIp =
                        packet.payload.getString(
                                "sender_ip"
                        );

                String resolvedMac =
                        packet.payload.getString(
                                "sender_mac"
                        );

                if (resolvedIp.equals(
                        wifiDefaultGatewayIp
                )) {
                    defaultGatewayMac =
                            resolvedMac;
                }

                flushWifiPendingIpv4(
                        resolvedIp,
                        resolvedMac
                );

                wifiRawIpWorkflow.onArpResolved(
'@

Replace-Exact "DHCP ACK route config" @'
        if ("ACK".equalsIgnoreCase(
                type
        )) {
            ipAddress =
                    packet.payload.getString(
                            "assigned_ip"
                    );

            defaultGatewayMac =
                    packet.sourceMac;

            wifiDhcpDnsServerIp =
                    packet.payload.getString(
                            "dns_server"
                    );

            wifiDhcpClientState =
'@ @'
        if ("ACK".equalsIgnoreCase(
                type
        )) {
            ipAddress =
                    packet.payload.getString(
                            "assigned_ip"
                    );

            String learnedMask =
                    packet.payload.getString(
                            "subnet_mask"
                    );

            if (!learnedMask.isBlank()) {
                try {
                    Ipv4Prefix.prefixLengthFromMask(
                            learnedMask
                    );

                    wifiSubnetMask =
                            learnedMask;
                } catch (IllegalArgumentException ignored) {
                }
            }

            String learnedGateway =
                    packet.payload.getString(
                            "router_ip"
                    );

            wifiDefaultGatewayIp =
                    Ipv4Prefix.isUsableUnicast(
                            learnedGateway
                    )
                            ? learnedGateway
                            : "";

            defaultGatewayMac =
                    wifiDefaultGatewayIp.equals(
                            packet.sourceIp
                    )
                            ? packet.sourceMac
                            : "";

            wifiDhcpDnsServerIp =
                    packet.payload.getString(
                            "dns_server"
                    );

            wifiPendingIpv4ByNextHop.clear();

            wifiDhcpClientState =
'@

Replace-Exact "route before raw carriers" @'
        if (shouldUseRawWifiDnsCarrier(
                packet
        )) {
'@ @'
        if (prepareWifiIpv4NextHop(
                packet
        )) {
            return;
        }

        if (shouldUseRawWifiDnsCarrier(
                packet
        )) {
'@

Replace-Exact "routing helpers before raw DNS helper" @'
    private boolean shouldUseRawWifiDnsCarrier(
            OSINetworkPacket packet
    ) {
'@ @'
    private boolean prepareWifiIpv4NextHop(
            OSINetworkPacket packet
    ) {
        if (packet == null
                || !isWifiProfile()
                || wifiMac.mode()
                == WifiMode.LEGACY_DIRECT
                || "ARP".equalsIgnoreCase(
                packet.applicationProtocol
        )
                || "DHCP".equalsIgnoreCase(
                packet.applicationProtocol
        )
                || "255.255.255.255".equals(
                packet.targetIp
        )
                || !Ipv4Prefix.isUsableUnicast(
                packet.targetIp
        )
                || !Ipv4Prefix.isUsableUnicast(
                ipAddress
        )) {
            return false;
        }

        Ipv4RouteDecision route =
                wifiIpv4RouteDecision(
                        packet.targetIp
                );

        if (!route.reachable()) {
            wifiIpApplication.setStatus(
                    "IPv4 unreachable: "
                            + packet.targetIp
                            + " | "
                            + route.detail()
            );
            return true;
        }

        String nextHopIp =
                route.nextHopIp();

        String nextHopMac =
                wifiIpApplication.neighborMac(
                        nextHopIp
                );

        if (route.kind()
                == Ipv4RouteKind.GATEWAY
                && nextHopIp.equals(
                wifiDefaultGatewayIp
        )
                && !defaultGatewayMac.isBlank()) {
            nextHopMac =
                    defaultGatewayMac;
        }

        if (route.kind()
                == Ipv4RouteKind.ON_LINK
                && nextHopMac.isBlank()
                && packet.targetMac != null
                && !packet.targetMac.isBlank()
                && !"FF:FF:FF:FF:FF:FF".equalsIgnoreCase(
                packet.targetMac
        )) {
            nextHopMac =
                    packet.targetMac;
        }

        packet.payload.putString(
                "ipv4_route_kind",
                route.kind().name()
        );

        packet.payload.putString(
                "ipv4_final_destination",
                packet.targetIp
        );

        packet.payload.putString(
                "ipv4_next_hop",
                nextHopIp
        );

        packet.payload.putInt(
                "ipv4_route_prefix_length",
                route.prefixLength()
        );

        if (!nextHopMac.isBlank()) {
            packet.targetMac =
                    nextHopMac;

            if (route.kind()
                    == Ipv4RouteKind.GATEWAY) {
                defaultGatewayMac =
                        nextHopMac;
            }

            return false;
        }

        if (!queueWifiPendingIpv4(
                nextHopIp,
                packet
        )) {
            wifiIpApplication.setStatus(
                    "IPv4 route queue full for "
                            + nextHopIp
            );
            return true;
        }

        wifiIpApplication.setStatus(
                "IPv4 route pending ARP: final "
                        + packet.targetIp
                        + " | next-hop "
                        + nextHopIp
        );

        sendWifiArpRequest(
                nextHopIp
        );

        return true;
    }

    private boolean queueWifiPendingIpv4(
            String nextHopIp,
            OSINetworkPacket packet
    ) {
        int total =
                wifiPendingIpv4ByNextHop
                        .values()
                        .stream()
                        .mapToInt(
                                java.util.List::size
                        )
                        .sum();

        if (total >= WIFI_PENDING_IPV4_TOTAL) {
            return false;
        }

        java.util.List<OSINetworkPacket> queue =
                wifiPendingIpv4ByNextHop
                        .computeIfAbsent(
                                nextHopIp,
                                ignored ->
                                        new java.util.ArrayList<>()
                        );

        if (queue.size()
                >= WIFI_PENDING_IPV4_PER_NEXT_HOP) {
            return false;
        }

        queue.add(
                packet
        );

        return true;
    }

    private void flushWifiPendingIpv4(
            String resolvedIp,
            String resolvedMac
    ) {
        if (resolvedIp == null
                || resolvedIp.isBlank()
                || resolvedMac == null
                || resolvedMac.isBlank()) {
            return;
        }

        java.util.List<OSINetworkPacket> pending =
                wifiPendingIpv4ByNextHop.remove(
                        resolvedIp
                );

        if (pending == null
                || pending.isEmpty()) {
            return;
        }

        for (OSINetworkPacket packet
                : pending) {
            packet.targetMac =
                    resolvedMac;

            transmitPacket(
                    packet
            );
        }
    }

    private boolean shouldUseRawWifiDnsCarrier(
            OSINetworkPacket packet
    ) {
'@

Replace-Exact "clear route queue with TCP clear" @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
        wifiRawDhcpPeers.clear();
        wifiRawDnsPeers.clear();
    }
'@ @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
        wifiRawDhcpPeers.clear();
        wifiRawDnsPeers.clear();
        wifiPendingIpv4ByNextHop.clear();
    }
'@

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n","`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.5 NDBE routing patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
