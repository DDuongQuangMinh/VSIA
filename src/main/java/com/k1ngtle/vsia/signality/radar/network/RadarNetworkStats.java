package com.k1ngtle.vsia.signality.radar.network;

public record RadarNetworkStats(
        String networkId,
        int queuedReports,
        int trackCount,
        int tentativeTracks,
        int confirmedTracks,
        int coastingTracks,
        long reportsAccepted,
        long reportsDropped,
        long reportsDelivered
) {
}
