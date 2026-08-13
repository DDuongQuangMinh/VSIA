package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.network.chat.Component;

public enum ServerRackProfile {
    INTRA_DATA_CENTER("Intra-Data Center", 2_000.0, false,
            "2 km / 2,000 blocks"),
    CAMPUS_INTERCONNECT("Campus Interconnect", 10_000.0, false,
            "10 km / 10,000 blocks"),
    METRO_DCI("Metro DCI", 100_000.0, true,
            "100 km / 100,000 blocks"),
    LONG_HAUL_GLOBAL("Long-Haul & Global", 5_000_000.0, true,
            "5,000 km / 5,000,000 blocks");

    private final String displayName;
    private final double maximumRangeBlocks;
    private final boolean wiredBeyondCampus;
    private final String rangeText;

    ServerRackProfile(String displayName, double maximumRangeBlocks,
                      boolean wiredBeyondCampus, String rangeText) {
        this.displayName = displayName;
        this.maximumRangeBlocks = maximumRangeBlocks;
        this.wiredBeyondCampus = wiredBeyondCampus;
        this.rangeText = rangeText;
    }

    public String displayName() { return displayName; }
    public double maximumRangeBlocks() { return maximumRangeBlocks; }
    public boolean wiredBeyondCampus() { return wiredBeyondCampus; }
    public String rangeText() { return rangeText; }
    public ServerRackProfile next() { return values()[(ordinal() + 1) % values().length]; }

    public static ServerRackProfile byName(String name) {
        try { return valueOf(name); }
        catch (IllegalArgumentException | NullPointerException ignored) { return INTRA_DATA_CENTER; }
    }

    public Component description() {
        return Component.literal(displayName + " - " + rangeText
                + (wiredBeyondCampus ? " (wire required beyond 10 km)" : ""));
    }
}
