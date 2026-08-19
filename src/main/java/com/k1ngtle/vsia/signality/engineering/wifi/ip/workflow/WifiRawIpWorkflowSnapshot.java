package com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow;

public record WifiRawIpWorkflowSnapshot(
        WifiRawIpWorkflowState state,
        String hostname,
        String path,
        String assignedIp,
        String dnsServerIp,
        String dnsServerMac,
        String targetIp,
        String targetMac,
        long deadlineMicros,
        String detail
) {
}
