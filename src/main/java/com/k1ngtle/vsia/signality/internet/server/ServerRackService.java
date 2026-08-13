package com.k1ngtle.vsia.signality.internet.server;

public enum ServerRackService {
    HTTP("HTTP", 80, true),
    DHCP("DHCP", 67, true),
    DHCPV6("DHCPv6", 547, false),
    TFTP("TFTP", 69, false),
    DNS("DNS", 53, true),
    SYSLOG("SYSLOG", 514, true),
    AAA("AAA", 0, true),
    NTP("NTP", 123, false),
    EMAIL("EMAIL", 25, true),
    FTP("FTP", 21, false),
    IOT("IoT", 1883, true),
    VM_MANAGEMENT("VM Management", 16509, true),
    RADIUS_EAP("Radius EAP", 1812, true),
    PRP("PRP", 0, true);

    private final String displayName;
    private final int defaultPort;
    private final boolean protocolReady;

    ServerRackService(String displayName, int defaultPort, boolean protocolReady) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.protocolReady = protocolReady;
    }

    public String displayName() { return displayName; }
    public int defaultPort() { return defaultPort; }
    public boolean protocolReady() { return protocolReady; }

    public static ServerRackService byDisplayName(String name) {
        for (ServerRackService service : values())
            if (service.displayName.equalsIgnoreCase(name)) return service;
        return HTTP;
    }
}
