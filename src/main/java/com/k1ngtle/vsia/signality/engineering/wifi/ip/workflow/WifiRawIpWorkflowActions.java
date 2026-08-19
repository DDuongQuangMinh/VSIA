package com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow;

public interface WifiRawIpWorkflowActions {
    boolean startDhcp();

    boolean arp(String targetIp);

    boolean dnsA(
            String hostname,
            String dnsServerIp,
            String dnsServerMac
    );

    boolean tcpHttp(
            String hostname,
            String targetIp,
            String targetMac,
            String path
    );

    void status(String status);
}
