package com.k1ngtle.vsia.signality.engineering.cellular.userplane;

// CELLULAR_USER_PLANE_V1
public record CellularUserPlaneHop(
        CellularUserPlaneLayer layer,
        CellularUserPlaneDirection direction,
        long processingMicros,
        int wireBytes,
        String note
) {
    public CellularUserPlaneHop {
        if (layer == null) {
            throw new IllegalArgumentException("layer cannot be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("direction cannot be null");
        }
        if (processingMicros < 0L) {
            throw new IllegalArgumentException("processingMicros cannot be negative");
        }
        if (wireBytes < 0) {
            throw new IllegalArgumentException("wireBytes cannot be negative");
        }
        note = note == null ? "" : note;
    }
}
