package com.k1ngtle.vsia.phone.network;

public final class PhoneNetworkState {
    private static final PhoneNetworkState INSTANCE = new PhoneNetworkState();

    private final WifiState wifi = new WifiState();
    private final CellularState cellular = new CellularState();
    private int batteryPercent = 92;

    private PhoneNetworkState() {
    }

    public static PhoneNetworkState get() {
        return INSTANCE;
    }

    public WifiState getWifi() {
        return wifi;
    }

    public CellularState getCellular() {
        return cellular;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = Math.max(0, Math.min(100, batteryPercent));
    }

    public boolean isWifiUsable() {
        return wifi.enabled && wifi.connected && wifi.stage == WifiStage.CONNECTED;
    }

    public boolean isCellularUsable() {
        return cellular.enabled
                && cellular.registered
                && "CONNECTED".equals(cellular.rrcState)
                && "REGISTERED".equals(cellular.nasState)
                && "ACTIVE".equals(cellular.pduState);
    }

    public enum WifiStage {
        IDLE("Idle"),
        SCAN("Scan"),
        AUTHENTICATION("Authentication"),
        ASSOCIATION("Association"),
        DHCP("DHCP"),
        GATEWAY("Gateway"),
        DNS("DNS"),
        CONNECTED("CONNECTED"),
        FAILED("FAILED");

        private final String displayName;

        WifiStage(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    public static final class WifiState {
        private boolean enabled = true;
        private boolean connected = false;
        private boolean locked = false;
        private String ssid = "";
        private String ipAddress = "—";
        private String subnetMask = "—";
        private String gateway = "—";
        private String dns = "—";
        private String bssid = "—";
        private int channel = 0;
        private int rssiDbm = -100;
        private String phy = "—";
        private WifiStage stage = WifiStage.IDLE;

        public boolean enabled() { return enabled; }
        public boolean connected() { return connected; }
        public boolean locked() { return locked; }
        public String ssid() { return ssid; }
        public String ipAddress() { return ipAddress; }
        public String subnetMask() { return subnetMask; }
        public String gateway() { return gateway; }
        public String dns() { return dns; }
        public String bssid() { return bssid; }
        public int channel() { return channel; }
        public int rssiDbm() { return rssiDbm; }
        public String phy() { return phy; }
        public WifiStage stage() { return stage; }
    }

    public static final class CellularState {
        private boolean enabled = true;
        private boolean registered = true;
        private String carrier = "VSIA Mobile";
        private String radioLabel = "5G";
        private String architecture = "SA";
        private int cellId = 201;
        private String band = "n78";
        private String ipAddress = "10.44.0.18";
        private String dnn = "internet";
        private int fiveQi = 9;

        private String rrcState = "CONNECTED";
        private String nasState = "REGISTERED";
        private String pduState = "ACTIVE";

        private int gnbId = 12001;
        private int tac = 100;
        private String plmn = "001-01";

        private int rsrpDbm = -86;
        private int rsrqDb = -10;
        private double sinrDb = 21.0;

        public boolean enabled() { return enabled; }
        public boolean registered() { return registered; }
        public String carrier() { return carrier; }
        public String radioLabel() { return radioLabel; }
        public String architecture() { return architecture; }
        public int cellId() { return cellId; }
        public String band() { return band; }
        public String ipAddress() { return ipAddress; }
        public String dnn() { return dnn; }
        public int fiveQi() { return fiveQi; }
        public String rrcState() { return rrcState; }
        public String nasState() { return nasState; }
        public String pduState() { return pduState; }
        public int gnbId() { return gnbId; }
        public int tac() { return tac; }
        public String plmn() { return plmn; }
        public int rsrpDbm() { return rsrpDbm; }
        public int rsrqDb() { return rsrqDb; }
        public double sinrDb() { return sinrDb; }
    }

    void setWifiEnabled(boolean enabled) {
        wifi.enabled = enabled;
        if (!enabled) {
            wifi.connected = false;
            wifi.stage = WifiStage.IDLE;
        }
    }

    void beginWifiConnection(String ssid, int rssiDbm, boolean locked) {
        wifi.enabled = true;
        wifi.connected = false;
        wifi.ssid = ssid;
        wifi.rssiDbm = rssiDbm;
        wifi.locked = locked;
        wifi.stage = WifiStage.SCAN;
        wifi.ipAddress = "—";
        wifi.subnetMask = "—";
        wifi.gateway = "—";
        wifi.dns = "—";
        wifi.bssid = "—";
        wifi.channel = 0;
        wifi.phy = "—";
    }

    void setWifiStage(WifiStage stage) {
        wifi.stage = stage;
    }

    void completeWifiDhcp() {
        wifi.ipAddress = "10.0.1.27";
        wifi.subnetMask = "255.255.255.0";
    }

    void completeWifiGateway() {
        wifi.gateway = "10.0.1.1";
    }

    void completeWifiDns() {
        wifi.dns = "10.0.1.1";
    }

    void completeWifiAssociation() {
        wifi.bssid = "A4:32:11:8C:4F:20";
        wifi.channel = 36;
        wifi.phy = "802.11ax";
    }

    void completeWifiConnection() {
        wifi.connected = true;
        wifi.stage = WifiStage.CONNECTED;
    }

    void setCellularEnabled(boolean enabled) {
        cellular.enabled = enabled;
        if (!enabled) {
            cellular.rrcState = "IDLE";
            cellular.nasState = "DEREGISTERED";
            cellular.pduState = "INACTIVE";
        } else {
            cellular.registered = true;
            cellular.rrcState = "CONNECTED";
            cellular.nasState = "REGISTERED";
            cellular.pduState = "ACTIVE";
        }
    }

    public void applyExternalWifiSnapshot(
            boolean enabled,
            boolean connected,
            String ssid,
            String ipAddress,
            String subnetMask,
            String gateway,
            String dns,
            String bssid,
            int channel,
            int rssiDbm,
            String phy,
            WifiStage stage
    ) {
        wifi.enabled = enabled;
        wifi.connected = connected;
        wifi.ssid = ssid == null ? "" : ssid;
        wifi.ipAddress = safe(ipAddress);
        wifi.subnetMask = safe(subnetMask);
        wifi.gateway = safe(gateway);
        wifi.dns = safe(dns);
        wifi.bssid = safe(bssid);
        wifi.channel = channel;
        wifi.rssiDbm = rssiDbm;
        wifi.phy = safe(phy);
        wifi.stage = stage == null ? WifiStage.IDLE : stage;
    }

    public void applyExternalCellularSnapshot(
            boolean enabled,
            boolean registered,
            String carrier,
            String radioLabel,
            String architecture,
            int gnbId,
            int cellId,
            int tac,
            String plmn,
            String band,
            String ipAddress,
            String dnn,
            int fiveQi,
            String rrcState,
            String nasState,
            String pduState,
            int rsrpDbm,
            int rsrqDb,
            double sinrDb
    ) {
        cellular.enabled = enabled;
        cellular.registered = registered;
        cellular.carrier = safe(carrier);
        cellular.radioLabel = safe(radioLabel);
        cellular.architecture = safe(architecture);
        cellular.gnbId = gnbId;
        cellular.cellId = cellId;
        cellular.tac = tac;
        cellular.plmn = safe(plmn);
        cellular.band = safe(band);
        cellular.ipAddress = safe(ipAddress);
        cellular.dnn = safe(dnn);
        cellular.fiveQi = fiveQi;
        cellular.rrcState = safe(rrcState);
        cellular.nasState = safe(nasState);
        cellular.pduState = safe(pduState);
        cellular.rsrpDbm = rsrpDbm;
        cellular.rsrqDb = rsrqDb;
        cellular.sinrDb = sinrDb;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
