package com.k1ngtle.vsia.signality.radar.network;

public record RadarSensorReport(
        String networkId,
        long deliveryTick,
        RadarMeasurement measurement
) implements Comparable<RadarSensorReport> {
    @Override
    public int compareTo(
            RadarSensorReport other
    ) {
        return Long.compare(
                deliveryTick,
                other.deliveryTick
        );
    }
}
