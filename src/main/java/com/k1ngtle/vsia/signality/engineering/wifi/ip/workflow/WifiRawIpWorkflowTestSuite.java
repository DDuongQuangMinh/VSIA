package com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow;

import java.util.ArrayList;
import java.util.List;

public final class WifiRawIpWorkflowTestSuite {
    private WifiRawIpWorkflowTestSuite() {
    }

    public static List<WifiRawIpWorkflowTestResult> runAll() {
        return List.of(
                fullSameServerFlow(),
                separateTargetFlow(),
                nxdomainFailure(),
                timeoutFailure(),
                mismatchedArpIgnored(),
                invalidDhcpRejected(),
                pathNormalization(),
                clearReturnsIdle()
        );
    }

    private static WifiRawIpWorkflowTestResult fullSameServerFlow() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        boolean started =
                controller.start(
                        "www.vsia-net.com",
                        "/",
                        1_000L,
                        actions
                );

        controller.onDhcpAck(
                "192.168.1.101",
                "192.168.1.2",
                2_000L,
                actions
        );

        controller.onArpResolved(
                "192.168.1.2",
                "c72ec34fc58c",
                3_000L,
                actions
        );

        controller.onDnsResponse(
                "www.vsia-net.com",
                "192.168.1.2",
                0,
                4_000L,
                actions
        );

        controller.onHttpResponse(
                200,
                5_000L,
                actions
        );

        WifiRawIpWorkflowSnapshot snapshot =
                controller.snapshot();

        return result(
                "wifi-w1104-full-same-server",
                started
                        && snapshot.state()
                        == WifiRawIpWorkflowState.COMPLETE
                        && actions.events.equals(
                        List.of(
                                "DHCP",
                                "ARP:192.168.1.2",
                                "DNS:www.vsia-net.com@192.168.1.2/c72ec34fc58c",
                                "TCP:www.vsia-net.com@192.168.1.2/c72ec34fc58c/"
                        )
                ),
                "DHCP -> DNS ARP -> DNS -> TCP HTTP must complete without a redundant target ARP when DNS and HTTP share one host"
        );
    }

    private static WifiRawIpWorkflowTestResult separateTargetFlow() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "web.vsia-net.com",
                "index.html",
                1_000L,
                actions
        );

        controller.onDhcpAck(
                "192.168.1.101",
                "192.168.1.2",
                2_000L,
                actions
        );

        controller.onArpResolved(
                "192.168.1.2",
                "dns000000001",
                3_000L,
                actions
        );

        controller.onDnsResponse(
                "web.vsia-net.com",
                "192.168.1.50",
                0,
                4_000L,
                actions
        );

        boolean targetArp =
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.TARGET_ARP
                        && actions.events.contains(
                        "ARP:192.168.1.50"
                );

        controller.onArpResolved(
                "192.168.1.50",
                "web000000001",
                5_000L,
                actions
        );

        boolean tcp =
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.TCP_HTTP
                        && actions.events.contains(
                        "TCP:web.vsia-net.com@192.168.1.50/web000000001/index.html"
                );

        return result(
                "wifi-w1104-separate-target",
                targetArp && tcp,
                "A DNS answer on a different host must trigger target ARP before TCP"
        );
    }

    private static WifiRawIpWorkflowTestResult nxdomainFailure() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                readyForDns(
                        actions
                );

        controller.onDnsResponse(
                "missing.vsia-net.com",
                "",
                3,
                4_000L,
                actions
        );

        return result(
                "wifi-w1104-nxdomain",
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.FAILED
                        && controller.snapshot().detail()
                        .contains(
                                "NXDOMAIN"
                        ),
                "NXDOMAIN must fail the unified workflow before ARP/TCP"
        );
    }

    private static WifiRawIpWorkflowTestResult timeoutFailure() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "www.vsia-net.com",
                "/",
                100L,
                actions
        );

        controller.tick(
                100L
                        + WifiRawIpWorkflowController.PHASE_TIMEOUT_MICROS
                        + 1L,
                actions
        );

        return result(
                "wifi-w1104-timeout",
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.FAILED
                        && controller.snapshot().detail()
                        .contains(
                                "DHCP"
                        ),
                "Each asynchronous stage must have a deterministic timeout"
        );
    }

    private static WifiRawIpWorkflowTestResult mismatchedArpIgnored() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "www.vsia-net.com",
                "/",
                1_000L,
                actions
        );

        controller.onDhcpAck(
                "192.168.1.101",
                "192.168.1.2",
                2_000L,
                actions
        );

        controller.onArpResolved(
                "192.168.1.99",
                "wrong",
                3_000L,
                actions
        );

        return result(
                "wifi-w1104-ignore-wrong-arp",
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.DNS_ARP,
                "An unrelated ARP reply must not advance the workflow"
        );
    }

    private static WifiRawIpWorkflowTestResult invalidDhcpRejected() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "www.vsia-net.com",
                "/",
                1_000L,
                actions
        );

        controller.onDhcpAck(
                "0.0.0.0",
                "192.168.1.2",
                2_000L,
                actions
        );

        return result(
                "wifi-w1104-invalid-dhcp",
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.FAILED,
                "The workflow must reject an unusable DHCP lease"
        );
    }

    private static WifiRawIpWorkflowTestResult pathNormalization() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "WWW.VSIA-NET.COM.",
                "index.html",
                1_000L,
                actions
        );

        WifiRawIpWorkflowSnapshot snapshot =
                controller.snapshot();

        return result(
                "wifi-w1104-normalization",
                snapshot.hostname()
                        .equals(
                                "www.vsia-net.com"
                        )
                        && snapshot.path()
                        .equals(
                                "/index.html"
                        ),
                "Hostname case/trailing-dot and HTTP path must normalize deterministically"
        );
    }

    private static WifiRawIpWorkflowTestResult clearReturnsIdle() {
        FakeActions actions =
                new FakeActions();

        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "www.vsia-net.com",
                "/",
                1_000L,
                actions
        );

        controller.clear(
                actions
        );

        return result(
                "wifi-w1104-clear",
                controller.snapshot().state()
                        == WifiRawIpWorkflowState.IDLE
                        && controller.snapshot().hostname()
                        .isBlank(),
                "Clear must discard all pending raw workflow state"
        );
    }

    private static WifiRawIpWorkflowController readyForDns(
            FakeActions actions
    ) {
        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        controller.start(
                "missing.vsia-net.com",
                "/",
                1_000L,
                actions
        );

        controller.onDhcpAck(
                "192.168.1.101",
                "192.168.1.2",
                2_000L,
                actions
        );

        controller.onArpResolved(
                "192.168.1.2",
                "dns000000001",
                3_000L,
                actions
        );

        return controller;
    }

    private static WifiRawIpWorkflowTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new WifiRawIpWorkflowTestResult(
                id,
                passed,
                detail
        );
    }

    private static final class FakeActions
            implements WifiRawIpWorkflowActions {
        private final List<String> events =
                new ArrayList<>();

        @Override
        public boolean startDhcp() {
            events.add(
                    "DHCP"
            );
            return true;
        }

        @Override
        public boolean arp(
                String targetIp
        ) {
            events.add(
                    "ARP:"
                            + targetIp
            );
            return true;
        }

        @Override
        public boolean dnsA(
                String hostname,
                String dnsServerIp,
                String dnsServerMac
        ) {
            events.add(
                    "DNS:"
                            + hostname
                            + "@"
                            + dnsServerIp
                            + "/"
                            + dnsServerMac
            );
            return true;
        }

        @Override
        public boolean tcpHttp(
                String hostname,
                String targetIp,
                String targetMac,
                String path
        ) {
            events.add(
                    "TCP:"
                            + hostname
                            + "@"
                            + targetIp
                            + "/"
                            + targetMac
                            + path
            );
            return true;
        }

        @Override
        public void status(
                String status
        ) {
        }
    }
}
