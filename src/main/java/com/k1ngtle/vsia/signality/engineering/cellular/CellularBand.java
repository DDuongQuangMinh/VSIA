package com.k1ngtle.vsia.signality.engineering.cellular;

public record CellularBand(
        String id,
        CellularGeneration generation,
        double downlinkLowHz,
        double downlinkHighHz,
        double uplinkLowHz,
        double uplinkHighHz,
        boolean timeDivisionDuplex
) {
    public CellularBand {
        id = id == null ? "" : id;
        if (generation == null) {
            throw new IllegalArgumentException("generation");
        }
        if (!(downlinkLowHz > 0.0) || downlinkHighHz < downlinkLowHz) {
            throw new IllegalArgumentException("downlink range");
        }
        if (!(uplinkLowHz > 0.0) || uplinkHighHz < uplinkLowHz) {
            throw new IllegalArgumentException("uplink range");
        }
    }

    public boolean containsDownlink(double frequencyHz) {
        return frequencyHz >= downlinkLowHz
                && frequencyHz <= downlinkHighHz;
    }

    public double centerDownlinkHz() {
        return (downlinkLowHz + downlinkHighHz) * 0.5;
    }
}
