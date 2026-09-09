package com.k1ngtle.vsia.signality.engineering.wifi.integration.w121;

public enum W121IntegrationStage {
    SETUP,
    SCANNING,
    CONNECTING,
    ASSOCIATED,
    DATA_ACK,
    DHCP,
    DNS_ARP,
    DNS_QUERY,
    TARGET_ARP,
    TCP_HTTP,
    COMPLETE,
    FAILED
}
