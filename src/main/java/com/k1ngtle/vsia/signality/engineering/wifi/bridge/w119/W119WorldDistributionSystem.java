package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class W119WorldDistributionSystem {
    private static final int HORIZONTAL_SEARCH_RADIUS = 16;
    private static final int VERTICAL_SEARCH_RADIUS = 6;

    private static final Map<BlockPos, BlockPos> SWITCH_CACHE =
            new HashMap<>();

    private W119WorldDistributionSystem() {
    }

    public static boolean forwardFromAccessPoint(
            NetworkDeviceBlockEntity accessPoint,
            OSINetworkPacket packet
    ) {
        if (accessPoint == null
                || packet == null) {
            return false;
        }

        Level level =
                accessPoint.getLevel();

        if (level == null
                || level.isClientSide) {
            return false;
        }

        BlockPos apPos =
                accessPoint.getBlockPos();

        NetworkSwitchBlockEntity linkedSwitch =
                cachedOrFindLinkedSwitch(
                        level,
                        apPos
                );

        if (linkedSwitch == null) {
            return false;
        }

        OSINetworkPacket forwarded =
                OSINetworkPacket.deserializeNBT(
                        packet.serializeNBT().copy()
                );

        linkedSwitch.receiveWiredPacket(
                forwarded,
                apPos
        );

        return true;
    }

    private static NetworkSwitchBlockEntity cachedOrFindLinkedSwitch(
            Level level,
            BlockPos apPos
    ) {
        BlockPos cached =
                SWITCH_CACHE.get(apPos);

        if (cached != null) {
            BlockEntity candidate =
                    level.getBlockEntity(cached);

            if (candidate
                    instanceof NetworkSwitchBlockEntity networkSwitch
                    && networkSwitch
                    .getConnectedDevices()
                    .contains(apPos)) {
                return networkSwitch;
            }

            SWITCH_CACHE.remove(apPos);
        }

        NetworkSwitchBlockEntity found =
                findLinkedSwitch(
                        level,
                        apPos
                );

        if (found != null) {
            SWITCH_CACHE.put(
                    apPos.immutable(),
                    found.getBlockPos().immutable()
            );
        }

        return found;
    }

    private static NetworkSwitchBlockEntity findLinkedSwitch(
            Level level,
            BlockPos apPos
    ) {
        BlockPos.MutableBlockPos cursor =
                new BlockPos.MutableBlockPos();

        for (int dy = -VERTICAL_SEARCH_RADIUS;
             dy <= VERTICAL_SEARCH_RADIUS;
             dy++) {
            for (int dx = -HORIZONTAL_SEARCH_RADIUS;
                 dx <= HORIZONTAL_SEARCH_RADIUS;
                 dx++) {
                for (int dz = -HORIZONTAL_SEARCH_RADIUS;
                     dz <= HORIZONTAL_SEARCH_RADIUS;
                     dz++) {
                    cursor.set(
                            apPos.getX() + dx,
                            apPos.getY() + dy,
                            apPos.getZ() + dz
                    );

                    BlockEntity blockEntity =
                            level.getBlockEntity(cursor);

                    if (!(blockEntity
                            instanceof NetworkSwitchBlockEntity networkSwitch)) {
                        continue;
                    }

                    if (networkSwitch
                            .getConnectedDevices()
                            .contains(apPos)) {
                        return networkSwitch;
                    }
                }
            }
        }

        return null;
    }
}
