package com.k1ngtle.vsia.signality.engineering.wifi.dhcp;

public final class DhcpOption {
    public static final int PAD = 0;
    public static final int SUBNET_MASK = 1;
    public static final int ROUTER = 3;
    public static final int DNS = 6;
    public static final int HOST_NAME = 12;
    public static final int REQUESTED_IP = 50;
    public static final int LEASE_TIME = 51;
    public static final int MESSAGE_TYPE = 53;
    public static final int SERVER_IDENTIFIER = 54;
    public static final int PARAMETER_REQUEST_LIST = 55;
    public static final int CLIENT_IDENTIFIER = 61;
    public static final int END = 255;

    private DhcpOption() {
    }
}
