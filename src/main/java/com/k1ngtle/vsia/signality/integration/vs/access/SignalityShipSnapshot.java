package com.k1ngtle.vsia.signality.integration.vs.access;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4dc;
import org.joml.Vector3dc;

public interface SignalityShipSnapshot {
    @Nullable
    Snapshot signality$readSnapshot();

    record Snapshot(
            Matrix4dc shipToWorld,
            Vector3dc linearVelocity,
            long captureGameTime
    ) {
    }
}
