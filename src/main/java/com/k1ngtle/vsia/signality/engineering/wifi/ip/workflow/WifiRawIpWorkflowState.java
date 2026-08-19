package com.k1ngtle.vsia.signality.engineering.wifi.ip.workflow;

public enum WifiRawIpWorkflowState {
    IDLE,
    DHCP,
    DNS_ARP,
    DNS_QUERY,
    TARGET_ARP,
    TCP_HTTP,
    COMPLETE,
    FAILED;

    public boolean active() {
        return this != IDLE
                && this != COMPLETE
                && this != FAILED;
    }
}
