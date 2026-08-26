package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public final class WifiEngineeringDeviceIdentityResolver {
    /*
     * A visible VS block center can differ from the integer world BlockPos by
     * up to roughly one block after rotation/rounding. Keep this deliberately
     * much tighter than WifiEngineeringTargetResolver's 4-block model search,
     * because the W1.23 four-device GUI must never silently bind a neighboring
     * router when the requested device is absent.
     */
    public static final double WORLD_ACQUIRE_RADIUS_BLOCKS =
            1.75D;

    private WifiEngineeringDeviceIdentityResolver() {
    }

    public static NetworkDeviceBlockEntity resolve(
            ServerLevel level,
            UUID deviceId
    ) {
        if (level == null || deviceId == null) {
            return null;
        }

        for (ISignalReceiver receiver
                : SignalBus.receiversInLevel(level)) {
            if (!deviceId.equals(receiver.id())) {
                continue;
            }

            if (receiver instanceof NetworkDeviceBlockEntity device) {
                return device;
            }
        }

        return null;
    }

    public static NetworkDeviceBlockEntity resolveNearWorld(
            ServerLevel level,
            BlockPos requestedWorldPos,
            Set<UUID> excludedIds
    ) {
        if (level == null || requestedWorldPos == null) {
            return null;
        }

        Set<UUID> excluded =
                excludedIds == null
                        ? Set.of()
                        : excludedIds;

        /*
         * Normal Minecraft block: prefer the exact BlockEntity first.
         */
        BlockEntity exact =
                level.getBlockEntity(requestedWorldPos);

        if (exact instanceof NetworkDeviceBlockEntity device
                && WifiEngineeringProbe.supports(device)
                && !excluded.contains(device.id())) {
            return device;
        }

        /*
         * Ship block: its BlockEntity is stored in VS shipyard coordinates,
         * while NetworkDeviceBlockEntity.positionWorld() already transforms
         * that storage-space position back into physical world space.
         */
        Vec3 requestedRfPoint =
                Vec3.atCenterOf(requestedWorldPos)
                        .add(0.0D, 0.5D, 0.0D);

        double maxDistanceSquared =
                WORLD_ACQUIRE_RADIUS_BLOCKS
                        * WORLD_ACQUIRE_RADIUS_BLOCKS;

        return SignalBus.receiversInLevel(level)
                .stream()
                .filter(NetworkDeviceBlockEntity.class::isInstance)
                .map(NetworkDeviceBlockEntity.class::cast)
                .filter(WifiEngineeringProbe::supports)
                .filter(candidate ->
                        !excluded.contains(candidate.id())
                )
                .filter(candidate -> {
                    Vec3 world =
                            candidate.positionWorld();

                    return world != null
                            && Double.isFinite(world.x)
                            && Double.isFinite(world.y)
                            && Double.isFinite(world.z)
                            && world.distanceToSqr(requestedRfPoint)
                            <= maxDistanceSquared;
                })
                .min(
                        Comparator.comparingDouble(
                                candidate ->
                                        candidate.positionWorld()
                                                .distanceToSqr(
                                                        requestedRfPoint
                                                )
                        )
                )
                .orElse(null);
    }
}
