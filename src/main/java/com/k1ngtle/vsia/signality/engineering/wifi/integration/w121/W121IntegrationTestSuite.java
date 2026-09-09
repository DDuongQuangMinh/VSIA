package com.k1ngtle.vsia.signality.engineering.wifi.integration.w121;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiSecurityEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119ApBridgeEngine;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119BridgeAction;
import com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119.W119Mac;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowActions;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowController;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow.WifiRawIpWorkflowState;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class W121IntegrationTestSuite {
    private W121IntegrationTestSuite() {
    }

    public static List<W121IntegrationTestResult> runAll() {
        List<W121IntegrationTestResult> results =
                new ArrayList<>();

        results.add(
                macNormalization()
        );

        results.add(
                securityRoundTrip()
        );

        results.add(
                securityTamperReject()
        );

        results.add(
                securityWrongKeyReject()
        );

        results.add(
                apBridgeBidirectionalDecision()
        );

        results.add(
                rawWorkflowHttp200()
        );

        results.add(
                rawWorkflowTimeout()
        );

        results.add(
                rawWorkflowDnsFailure()
        );

        return List.copyOf(
                results
        );
    }

    private static W121IntegrationTestResult macNormalization() {
        boolean passed =
                W119Mac.equals(
                        "3A:DE:93:B4:6C:AF",
                        "3a:de:93:b4:6c:af"
                )
                        && W119Mac.equals(
                        "3A:DE:93:B4:6C:AF",
                        "3ade93b46caf"
                )
                        && W119Mac.equals(
                        "3A-DE-93-B4-6C-AF",
                        "3ade.93b4.6caf"
                )
                        && W119Mac.isBroadcast(
                        "ff:ff:ff:ff:ff:ff"
                );

        return result(
                "w121-mac-normalization",
                passed,
                "Equivalent MAC text forms must resolve to the same 48-bit identity"
        );
    }

    private static W121IntegrationTestResult securityRoundTrip() {
        try {
            byte[] ptk =
                    ptk(
                            "correct-password"
                    );

            byte[] payload =
                    "VSIA-W121-PROTECTED-DATA"
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            byte[] protectedBytes =
                    WifiSecurityEngine.protect(
                            ptk,
                            payload
                    );

            byte[] recovered =
                    WifiSecurityEngine.unprotect(
                            ptk,
                            protectedBytes
                    );

            return result(
                    "w121-security-roundtrip",
                    Arrays.equals(
                            payload,
                            recovered
                    ),
                    "Protected DATA must decrypt/authenticate with the correct PTK"
            );
        } catch (Exception exception) {
            return result(
                    "w121-security-roundtrip",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    private static W121IntegrationTestResult securityTamperReject() {
        try {
            byte[] ptk =
                    ptk(
                            "correct-password"
                    );

            byte[] protectedBytes =
                    WifiSecurityEngine.protect(
                            ptk,
                            "VSIA-W121-TAMPER"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            protectedBytes[
                    protectedBytes.length - 1
                    ] ^= 0x01;

            try {
                WifiSecurityEngine.unprotect(
                        ptk,
                        protectedBytes
                );

                return result(
                        "w121-security-tamper",
                        false,
                        "Tampered ciphertext was accepted"
                );
            } catch (IllegalArgumentException expected) {
                return result(
                        "w121-security-tamper",
                        true,
                        "Tampered protected DATA is rejected"
                );
            }
        } catch (Exception exception) {
            return result(
                    "w121-security-tamper",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    private static W121IntegrationTestResult securityWrongKeyReject() {
        try {
            byte[] correct =
                    ptk(
                            "correct-password"
                    );

            byte[] wrong =
                    ptk(
                            "wrong-password"
                    );

            byte[] protectedBytes =
                    WifiSecurityEngine.protect(
                            correct,
                            "VSIA-W121-WRONG-KEY"
                                    .getBytes(
                                            StandardCharsets.UTF_8
                                    )
                    );

            try {
                WifiSecurityEngine.unprotect(
                        wrong,
                        protectedBytes
                );

                return result(
                        "w121-security-wrong-key",
                        false,
                        "Ciphertext was accepted with the wrong PTK"
                );
            } catch (IllegalArgumentException expected) {
                return result(
                        "w121-security-wrong-key",
                        true,
                        "Protected DATA is rejected when the PTK is wrong"
                );
            }
        } catch (Exception exception) {
            return result(
                    "w121-security-wrong-key",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    private static W121IntegrationTestResult apBridgeBidirectionalDecision() {
        try {
            String apMac =
                    "02:00:00:00:00:01";

            String stationMac =
                    "02:00:00:00:00:02";

            String serverMac =
                    "02:00:00:00:00:03";

            W119ApBridgeEngine bridge =
                    new W119ApBridgeEngine(
                            apMac
                    );

            OSINetworkPacket uplink =
                    new OSINetworkPacket();

            uplink.sourceMac =
                    stationMac;

            uplink.targetMac =
                    serverMac;

            boolean uplinkOk =
                    bridge.wirelessIngress(
                            uplink,
                            true,
                            false,
                            1L
                    ).action()
                            == W119BridgeAction
                            .TO_DISTRIBUTION_SYSTEM;

            OSINetworkPacket downlink =
                    new OSINetworkPacket();

            downlink.sourceMac =
                    serverMac;

            downlink.targetMac =
                    stationMac;

            boolean downlinkOk =
                    bridge.distributionIngress(
                            downlink,
                            2L
                    ).action()
                            == W119BridgeAction
                            .TO_WIRELESS;

            return result(
                    "w121-ap-bridge-bidirectional",
                    uplinkOk
                            && downlinkOk,
                    "STA->DS and DS->associated-STA bridge decisions must both work"
            );
        } catch (Exception exception) {
            return result(
                    "w121-ap-bridge-bidirectional",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    private static W121IntegrationTestResult rawWorkflowHttp200() {
        try {
            WifiRawIpWorkflowController controller =
                    new WifiRawIpWorkflowController();

            FakeActions actions =
                    new FakeActions();

            long now =
                    1_000_000L;

            boolean started =
                    controller.start(
                            "www.vsia-net.com",
                            "/",
                            now,
                            actions
                    );

            controller.onDhcpAck(
                    "192.168.1.100",
                    "192.168.1.2",
                    now + 100L,
                    actions
            );

            controller.onArpResolved(
                    "192.168.1.2",
                    "02:00:00:00:00:22",
                    now + 200L,
                    actions
            );

            controller.onDnsResponse(
                    "www.vsia-net.com",
                    "192.168.1.2",
                    0,
                    now + 300L,
                    actions
            );

            controller.onHttpResponse(
                    200,
                    now + 400L,
                    actions
            );

            boolean passed =
                    started
                            && actions.dhcpStarted
                            && actions.arpStarted
                            && actions.dnsStarted
                            && actions.tcpStarted
                            && controller.snapshot()
                            .state()
                            == WifiRawIpWorkflowState.COMPLETE
                            && controller.snapshot()
                            .detail()
                            .contains(
                                    "HTTP 200"
                            );

            return result(
                    "w121-raw-workflow-http200",
                    passed,
                    controller.snapshot()
                            .detail()
            );
        } catch (Exception exception) {
            return result(
                    "w121-raw-workflow-http200",
                    false,
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + safe(
                            exception.getMessage()
                    )
            );
        }
    }

    private static W121IntegrationTestResult rawWorkflowTimeout() {
        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        FakeActions actions =
                new FakeActions();

        long now =
                10_000L;

        controller.start(
                "www.vsia-net.com",
                "/",
                now,
                actions
        );

        controller.tick(
                now
                        + WifiRawIpWorkflowController
                        .PHASE_TIMEOUT_MICROS
                        + 1L,
                actions
        );

        boolean passed =
                controller.snapshot()
                        .state()
                        == WifiRawIpWorkflowState.FAILED
                        && controller.snapshot()
                        .detail()
                        .contains(
                                "timeout"
                        );

        return result(
                "w121-raw-workflow-timeout",
                passed,
                controller.snapshot()
                        .detail()
        );
    }

    private static W121IntegrationTestResult rawWorkflowDnsFailure() {
        WifiRawIpWorkflowController controller =
                new WifiRawIpWorkflowController();

        FakeActions actions =
                new FakeActions();

        long now =
                20_000L;

        controller.start(
                "www.vsia-net.com",
                "/",
                now,
                actions
        );

        controller.onDhcpAck(
                "192.168.1.100",
                "192.168.1.2",
                now + 100L,
                actions
        );

        controller.onArpResolved(
                "192.168.1.2",
                "02:00:00:00:00:22",
                now + 200L,
                actions
        );

        controller.onDnsResponse(
                "www.vsia-net.com",
                "",
                3,
                now + 300L,
                actions
        );

        boolean passed =
                controller.snapshot()
                        .state()
                        == WifiRawIpWorkflowState.FAILED
                        && controller.snapshot()
                        .detail()
                        .contains(
                                "NXDOMAIN"
                        );

        return result(
                "w121-raw-workflow-dns-failure",
                passed,
                controller.snapshot()
                        .detail()
        );
    }

    private static byte[] ptk(
            String passphrase
    ) {
        byte[] pmk =
                WifiSecurityEngine.derivePmk(
                        passphrase,
                        "VSIA-W121"
                );

        byte[] anonce =
                new byte[
                        32
                        ];

        byte[] snonce =
                new byte[
                        32
                        ];

        for (int i = 0;
             i < 32;
             i++) {
            anonce[i] =
                    (byte) (
                            i + 1
                    );

            snonce[i] =
                    (byte) (
                            0x7F - i
                    );
        }

        return WifiSecurityEngine.derivePtk(
                pmk,
                "02:00:00:00:00:01",
                "02:00:00:00:00:02",
                anonce,
                snonce
        );
    }

    private static W121IntegrationTestResult result(
            String id,
            boolean passed,
            String detail
    ) {
        return new W121IntegrationTestResult(
                id,
                passed,
                detail
        );
    }

    private static String safe(
            String value
    ) {
        return value == null
                ? ""
                : value;
    }

    private static final class FakeActions
            implements WifiRawIpWorkflowActions {
        private boolean dhcpStarted;
        private boolean arpStarted;
        private boolean dnsStarted;
        private boolean tcpStarted;
        private String status = "";

        @Override
        public boolean startDhcp() {
            dhcpStarted =
                    true;

            return true;
        }

        @Override
        public boolean arp(
                String targetIp
        ) {
            arpStarted =
                    true;

            return true;
        }

        @Override
        public boolean dnsA(
                String hostname,
                String dnsServerIp,
                String dnsServerMac
        ) {
            dnsStarted =
                    true;

            return true;
        }

        @Override
        public boolean tcpHttp(
                String hostname,
                String targetIp,
                String targetMac,
                String path
        ) {
            tcpStarted =
                    true;

            return true;
        }

        @Override
        public void status(
                String status
        ) {
            this.status =
                    status == null
                            ? ""
                            : status;
        }
    }
}
