package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import com.k1ngtle.vsia.signality.api.signal.ISignalReceiver;
import com.k1ngtle.vsia.signality.core.signal.SignalBus;
import com.k1ngtle.vsia.signality.integration.vs.VsRuntimeCompat;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public final class WifiEngineeringDeviceIdentityResolver {
    public static final double WORLD_ACQUIRE_RADIUS_BLOCKS = 1.75D;
    private static final double SHIP_SEARCH_MARGIN_BLOCKS = 3.0D;
    private static final int SHIP_BLOCK_SEARCH_RADIUS = 2;

    private WifiEngineeringDeviceIdentityResolver() {
    }

    public static NetworkDeviceBlockEntity resolve(ServerLevel level, UUID deviceId) {
        if (level == null || deviceId == null) {
            return null;
        }

        for (ISignalReceiver receiver : SignalBus.receiversInLevel(level)) {
            if (!deviceId.equals(receiver.id())) {
                continue;
            }

            if (!(receiver instanceof NetworkDeviceBlockEntity device)) {
                continue;
            }

            if (device.isRemoved()
                    || device.getLevel() != level
                    || !WifiEngineeringProbe.supports(device)) {
                continue;
            }

            return device;
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

        Set<UUID> excluded = excludedIds == null ? Set.of() : excludedIds;

        BlockEntity exact = level.getBlockEntity(requestedWorldPos);

        if (exact instanceof NetworkDeviceBlockEntity device
                && WifiEngineeringProbe.supports(device)
                && registeredAndActive(level, device)
                && !excluded.contains(device.id())) {
            rebind(device);
            return device;
        }

        NetworkDeviceBlockEntity storageMatch =
                SignalBus.receiversInLevel(level)
                        .stream()
                        .filter(NetworkDeviceBlockEntity.class::isInstance)
                        .map(NetworkDeviceBlockEntity.class::cast)
                        .filter(WifiEngineeringProbe::supports)
                        .filter(value -> registeredAndActive(level, value))
                        .filter(value -> !excluded.contains(value.id()))
                        .filter(value -> requestedWorldPos.equals(value.getBlockPos()))
                        .findFirst()
                        .orElse(null);

        if (storageMatch != null) {
            rebind(storageMatch);
            return storageMatch;
        }

        NetworkDeviceBlockEntity shipDevice =
                resolveShipDeviceNearWorld(level, requestedWorldPos, excluded);

        if (shipDevice != null) {
            rebind(shipDevice);
            return shipDevice;
        }

        Vec3 requestedRfPoint =
                Vec3.atCenterOf(requestedWorldPos).add(0.0D, 0.5D, 0.0D);

        double maxDistanceSquared =
                WORLD_ACQUIRE_RADIUS_BLOCKS * WORLD_ACQUIRE_RADIUS_BLOCKS;

        NetworkDeviceBlockEntity candidate =
                SignalBus.receiversInLevel(level)
                        .stream()
                        .filter(NetworkDeviceBlockEntity.class::isInstance)
                        .map(NetworkDeviceBlockEntity.class::cast)
                        .filter(WifiEngineeringProbe::supports)
                        .filter(value -> registeredAndActive(level, value))
                        .filter(value -> !excluded.contains(value.id()))
                        .filter(value -> {
                            Vec3 world = value.positionWorld();
                            return finite(world)
                                    && world.distanceToSqr(requestedRfPoint)
                                    <= maxDistanceSquared;
                        })
                        .min(Comparator.comparingDouble(
                                value -> value.positionWorld()
                                        .distanceToSqr(requestedRfPoint)
                        ))
                        .orElse(null);

        if (candidate != null) {
            rebind(candidate);
        }

        return candidate;
    }

    private static NetworkDeviceBlockEntity resolveShipDeviceNearWorld(
            ServerLevel level,
            BlockPos requestedWorldPos,
            Set<UUID> excluded
    ) {
        Vec3 requestedCenter = Vec3.atCenterOf(requestedWorldPos);
        Vec3 requestedRf = requestedCenter.add(0.0D, 0.5D, 0.0D);

        NetworkDeviceBlockEntity[] best = {null};
        double[] bestDistance = {Double.POSITIVE_INFINITY};

        VsRuntimeCompat.loadedShips(level).forEach(ship -> {
            AABBdc worldAabb = VsRuntimeCompat.worldAabb(ship);

            if (worldAabb != null
                    && !insideExpanded(
                            worldAabb,
                            requestedCenter,
                            SHIP_SEARCH_MARGIN_BLOCKS
                    )) {
                return;
            }

            Matrix4dc worldToShip = VsRuntimeCompat.worldToShip(ship);
            Matrix4dc shipToWorld = VsRuntimeCompat.shipToWorld(ship);

            if (worldToShip == null || shipToWorld == null) {
                return;
            }

            Vector3d shipPoint = new Vector3d(
                    requestedCenter.x,
                    requestedCenter.y,
                    requestedCenter.z
            );

            worldToShip.transformPosition(shipPoint);

            BlockPos base = BlockPos.containing(
                    shipPoint.x,
                    shipPoint.y,
                    shipPoint.z
            );

            for (int dx = -SHIP_BLOCK_SEARCH_RADIUS;
                 dx <= SHIP_BLOCK_SEARCH_RADIUS;
                 dx++) {
                for (int dy = -SHIP_BLOCK_SEARCH_RADIUS;
                     dy <= SHIP_BLOCK_SEARCH_RADIUS;
                     dy++) {
                    for (int dz = -SHIP_BLOCK_SEARCH_RADIUS;
                         dz <= SHIP_BLOCK_SEARCH_RADIUS;
                         dz++) {
                        BlockPos candidatePos = base.offset(dx, dy, dz);
                        BlockEntity blockEntity = level.getBlockEntity(candidatePos);

                        if (!(blockEntity instanceof NetworkDeviceBlockEntity candidate)) {
                            continue;
                        }

                        if (!WifiEngineeringProbe.supports(candidate)
                                || candidate.isRemoved()
                                || excluded.contains(candidate.id())) {
                            continue;
                        }

                        Vec3 localRf = Vec3.atCenterOf(candidatePos)
                                .add(0.0D, 0.5D, 0.0D);

                        Vector3d worldRf = new Vector3d(
                                localRf.x,
                                localRf.y,
                                localRf.z
                        );

                        shipToWorld.transformPosition(worldRf);

                        double distance = requestedRf.distanceToSqr(
                                worldRf.x,
                                worldRf.y,
                                worldRf.z
                        );

                        if (distance <= WORLD_ACQUIRE_RADIUS_BLOCKS
                                * WORLD_ACQUIRE_RADIUS_BLOCKS
                                && distance < bestDistance[0]) {
                            bestDistance[0] = distance;
                            best[0] = candidate;
                        }
                    }
                }
            }
        });

        return best[0];
    }

    private static void rebind(NetworkDeviceBlockEntity device) {
        SignalBus.registerReceiver(device);
        SignalBus.registerTransmitter(device);
    }

    private static boolean registeredAndActive(
            ServerLevel level,
            NetworkDeviceBlockEntity device
    ) {
        return device != null
                && !device.isRemoved()
                && device.getLevel() == level;
    }

    private static boolean insideExpanded(
            AABBdc aabb,
            Vec3 point,
            double margin
    ) {
        return point.x >= aabb.minX() - margin
                && point.x <= aabb.maxX() + margin
                && point.y >= aabb.minY() - margin
                && point.y <= aabb.maxY() + margin
                && point.z >= aabb.minZ() - margin
                && point.z <= aabb.maxZ() + margin;
    }

    private static boolean finite(Vec3 value) {
        return value != null
                && Double.isFinite(value.x)
                && Double.isFinite(value.y)
                && Double.isFinite(value.z);
    }
}
