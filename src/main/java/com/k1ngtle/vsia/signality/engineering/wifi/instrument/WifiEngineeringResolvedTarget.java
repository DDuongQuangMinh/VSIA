package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;

public record WifiEngineeringResolvedTarget(
        BlockPos requestedPos,
        BlockPos devicePos,
        NetworkDeviceBlockEntity device,
        int hops,
        String routeDescription
) {
    public WifiEngineeringResolvedTarget {
        requestedPos =
                requestedPos.immutable();

        devicePos =
                devicePos.immutable();

        if (device == null) {
            throw new IllegalArgumentException(
                    "device"
            );
        }

        if (hops < 0) {
            throw new IllegalArgumentException(
                    "hops"
            );
        }

        routeDescription =
                routeDescription == null
                        ? ""
                        : routeDescription;
    }

    public boolean direct() {
        return hops == 0
                && requestedPos.equals(
                devicePos
        );
    }
}
