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

if ($text.Contains("startWifiRawHttpWorkflow(")) {
    Write-Host "W1.10.4 unified raw workflow already present."
    exit 0
}

$backup = "$path.w1.10.4.bak"

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
        throw "W1.10.4 NDBE patch anchor not found: $Label"
    }

    $script:text = $script:text.Replace($Old,$New)
}

Replace-Exact "workflow imports" @'
import com.k1ngtle.vsia.signality.engineering.wifi.dns.live.DnsRawLiveCarrierCodec;
'@ @'
import com.k1ngtle.vsia.signality.engineering.wifi.dns.live.DnsRawLiveCarrierCodec;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowActions;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowController;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowSnapshot;
'@

Replace-Exact "workflow fields" @'
    private String wifiDnsLastAnswer = "";

    private DhcpClientState wifiDhcpClientState =
'@ @'
    private String wifiDnsLastAnswer = "";

    private final WifiRawIpWorkflowController wifiRawIpWorkflow =
            new WifiRawIpWorkflowController();

    private final WifiRawIpWorkflowActions wifiRawIpWorkflowActions =
            new WifiRawIpWorkflowActions() {
                @Override
                public boolean startDhcp() {
                    requestDynamicIp();
                    return true;
                }

                @Override
                public boolean arp(
                        String targetIp
                ) {
                    return sendWifiArpRequest(
                            targetIp
                    );
                }

                @Override
                public boolean dnsA(
                        String hostname,
                        String dnsServerIp,
                        String dnsServerMac
                ) {
                    configureWifiIpPeer(
                            dnsServerIp,
                            dnsServerMac
                    );

                    return sendWifiDnsAQuery(
                            hostname
                    );
                }

                @Override
                public boolean tcpHttp(
                        String hostname,
                        String targetIp,
                        String targetMac,
                        String path
                ) {
                    configureWifiIpPeer(
                            targetIp,
                            targetMac
                    );

                    return startWifiTcpHttpGet(
                            targetMac,
                            targetIp,
                            path
                    );
                }

                @Override
                public void status(
                        String status
                ) {
                    wifiIpApplication.setStatus(
                            status
                    );
                }
            };

    private DhcpClientState wifiDhcpClientState =
'@

Replace-Exact "DHCP DNS server field" @'
    private String wifiDhcpServerIdentifier = "";

    private final CellularRanController cellularRan =
'@ @'
    private String wifiDhcpServerIdentifier = "";

    private String wifiDhcpDnsServerIp = "";

    private final CellularRanController cellularRan =
'@

Replace-Exact "workflow tick" @'
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
'@ @'
            TcpLiveScheduler.register(
                    signalId,
                    () -> {
                        if (level instanceof ServerLevel serverLevel) {
                            long nowMicros =
                                    NetworkTimebase.nowMicros(
                                            serverLevel
                                    );

                            wifiTcpLive.tick(
                                    nowMicros,
                                    this::transmitPacket
                            );

                            wifiRawIpWorkflow.tick(
                                    nowMicros,
                                    wifiRawIpWorkflowActions
                            );
                        }
                    }
            );
'@

Replace-Exact "workflow API before DNS response" @'
    public String wifiDnsLastAnswer() {
        return wifiDnsLastAnswer;
    }

    private void handleWifiDnsResponse(
'@ @'
    public String wifiDnsLastAnswer() {
        return wifiDnsLastAnswer;
    }

    public WifiRawIpWorkflowSnapshot wifiRawIpWorkflowSnapshot() {
        return wifiRawIpWorkflow.snapshot();
    }

    public boolean startWifiRawHttpWorkflow(
            String hostname,
            String path
    ) {
        if (!wifiIpReady()) {
            wifiIpApplication.setStatus(
                    "RAW HTTP rejected: Wi-Fi is not associated/secured"
            );
            return false;
        }

        if (wifiTcpExecutionMode
                != ExecutionMode.CONFORMANCE) {
            setWifiTcpExecutionMode(
                    ExecutionMode.CONFORMANCE
            );
        }

        wifiTcpLive.clear();
        wifiRawIpWorkflow.clear(
                null
        );

        long nowMicros =
                level instanceof ServerLevel serverLevel
                        ? NetworkTimebase.nowMicros(
                        serverLevel
                )
                        : 0L;

        return wifiRawIpWorkflow.start(
                hostname,
                path,
                nowMicros,
                wifiRawIpWorkflowActions
        );
    }

    public void clearWifiRawIpWorkflow() {
        wifiRawIpWorkflow.clear(
                wifiRawIpWorkflowActions
        );
    }

    private void handleWifiDnsResponse(
'@

Replace-Exact "DNS NXDOMAIN workflow" @'
        if (rcode == 3) {
            wifiDnsLastAnswer =
                    "";

            wifiIpApplication.setStatus(
                    "DNS NXDOMAIN: "
                            + domain
            );

            return;
        }
'@ @'
        if (rcode == 3) {
            wifiDnsLastAnswer =
                    "";

            wifiIpApplication.setStatus(
                    "DNS NXDOMAIN: "
                            + domain
            );

            wifiRawIpWorkflow.onDnsResponse(
                    domain,
                    "",
                    rcode,
                    level instanceof ServerLevel serverLevel
                            ? NetworkTimebase.nowMicros(
                            serverLevel
                    )
                            : 0L,
                    wifiRawIpWorkflowActions
            );

            return;
        }
'@

Replace-Exact "DNS success workflow" @'
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
'@ @'
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

        wifiRawIpWorkflow.onDnsResponse(
                domain,
                answer,
                rcode,
                level instanceof ServerLevel serverLevel
                        ? NetworkTimebase.nowMicros(
                        serverLevel
                )
                        : 0L,
                wifiRawIpWorkflowActions
        );
    }
'@

Replace-Exact "mode clears all raw state" @'
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();

        wifiIpApplication.setStatus(
'@ @'
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
        wifiRawDhcpPeers.clear();
        wifiRawDnsPeers.clear();
        wifiRawIpWorkflow.clear(
                null
        );

        wifiIpApplication.setStatus(
'@

Replace-Exact "clear live raw peers" @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
    }
'@ @'
    public void clearWifiTcpLive() {
        wifiTcpLive.clear();
        wifiRawTcpSessions.clear();
        wifiRawArpPeers.clear();
        wifiRawDhcpPeers.clear();
        wifiRawDnsPeers.clear();
    }
'@

Replace-Exact "TCP application completion" @'
        processLayer4(
                application
        );
    }
'@ @'
        processLayer4(
                application
        );

        if (application.isResponse
                && "HTTP".equalsIgnoreCase(
                application.applicationProtocol
        )) {
            wifiRawIpWorkflow.onHttpResponse(
                    application.payload.getInt(
                            "status"
                    ),
                    level instanceof ServerLevel serverLevel
                            ? NetworkTimebase.nowMicros(
                            serverLevel
                    )
                            : 0L,
                    wifiRawIpWorkflowActions
            );
        }
    }
'@

Replace-Exact "ARP workflow progression" @'
        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT
                && wifiIpApplication.handleIncoming(
                macAddress,
                ipAddress,
                packet,
                level instanceof ServerLevel serverLevel
                        ? NetworkTimebase.nowMicros(
                        serverLevel
                )
                        : 0L,
                this::transmitPacket
        )) {
            setChanged();
            return;
        }
'@ @'
        if (isWifiProfile()
                && wifiMac.mode()
                != WifiMode.LEGACY_DIRECT
                && wifiIpApplication.handleIncoming(
                macAddress,
                ipAddress,
                packet,
                level instanceof ServerLevel serverLevel
                        ? NetworkTimebase.nowMicros(
                        serverLevel
                )
                        : 0L,
                this::transmitPacket
        )) {
            if ("ARP".equalsIgnoreCase(
                    packet.applicationProtocol
            )
                    && "REPLY".equalsIgnoreCase(
                    packet.payload.getString(
                            "operation"
                    )
            )) {
                wifiRawIpWorkflow.onArpResolved(
                        packet.payload.getString(
                                "sender_ip"
                        ),
                        packet.payload.getString(
                                "sender_mac"
                        ),
                        level instanceof ServerLevel serverLevel
                                ? NetworkTimebase.nowMicros(
                                serverLevel
                        )
                                : 0L,
                        wifiRawIpWorkflowActions
                );
            }

            setChanged();
            return;
        }
'@

Replace-Exact "DHCP reset DNS" @'
        wifiDhcpOfferedIp = "";
        wifiDhcpServerIdentifier = "";

        OSINetworkPacket packet =
'@ @'
        wifiDhcpOfferedIp = "";
        wifiDhcpServerIdentifier = "";
        wifiDhcpDnsServerIp = "";

        OSINetworkPacket packet =
'@

Replace-Exact "DHCP ACK workflow" @'
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
'@ @'
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
                    DhcpClientState.BOUND;

            wifiIpApplication.setStatus(
                    "DHCP DORA complete: "
                            + ipAddress
            );

            wifiRawIpWorkflow.onDhcpAck(
                    ipAddress,
                    wifiDhcpDnsServerIp,
                    level instanceof ServerLevel serverLevel
                            ? NetworkTimebase.nowMicros(
                            serverLevel
                    )
                            : 0L,
                    wifiRawIpWorkflowActions
            );

            setChanged();
            return;
        }
'@

Replace-Exact "DHCP NAK workflow" @'
        if ("NAK".equalsIgnoreCase(
                type
        )) {
            wifiDhcpClientState =
                    DhcpClientState.FAILED;

            wifiIpApplication.setStatus(
                    "DHCP NAK received"
            );
        }
'@ @'
        if ("NAK".equalsIgnoreCase(
                type
        )) {
            wifiDhcpClientState =
                    DhcpClientState.FAILED;

            wifiIpApplication.setStatus(
                    "DHCP NAK received"
            );

            wifiRawIpWorkflow.onDhcpNak(
                    wifiRawIpWorkflowActions
            );
        }
'@

if ($nl -eq "`r`n") {
    $text = $text.Replace("`n","`r`n")
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($path,$text,$utf8)

Write-Host "W1.10.4 NDBE unified workflow patched:"
Write-Host $path
Write-Host "Backup:"
Write-Host $backup
