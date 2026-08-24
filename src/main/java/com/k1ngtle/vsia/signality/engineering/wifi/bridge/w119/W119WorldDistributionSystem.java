package com.k1ngtle.vsia.signality.engineering.wifi.bridge.w119;

import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.router.RtAc68uRouterBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

public final class W119WorldDistributionSystem {
    private static final int HORIZONTAL_SEARCH_RADIUS = 16;
    private static final int VERTICAL_SEARCH_RADIUS = 6;

    private static final Map<AttachmentKey, BlockPos> SWITCH_CACHE =
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

        if (!(level instanceof ServerLevel)) {
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

        return linkedSwitch.w119AcceptAccessPointFrame(
                forwarded,
                apPos
        );
    }

    public static String status(
            NetworkDeviceBlockEntity accessPoint
    ) {
        if (accessPoint == null) {
            return "W1.19 DS link=INVALID_AP";
        }

        Level level =
                accessPoint.getLevel();

        if (!(level instanceof ServerLevel)) {
            return "W1.19 DS link=NO_SERVER_LEVEL";
        }

        BlockPos apPos =
                accessPoint.getBlockPos();

        NetworkSwitchBlockEntity linkedSwitch =
                cachedOrFindLinkedSwitch(
                        level,
                        apPos
                );

        if (linkedSwitch == null) {
            return "W1.19 DS link=UNRESOLVED ap="
                    + apPos.toShortString();
        }

        return "W1.19 DS "
                + linkedSwitch.w119DescribeConnectedDevice(
                        apPos
                );
    }

    public static void invalidate(
            NetworkDeviceBlockEntity accessPoint
    ) {
        if (accessPoint == null
                || accessPoint.getLevel() == null) {
            return;
        }

        SWITCH_CACHE.remove(
                key(
                        accessPoint.getLevel(),
                        accessPoint.getBlockPos()
                )
        );
    }

    public static void invalidate(
            Level level,
            BlockPos accessPointPos
    ) {
        if (level == null
                || accessPointPos == null) {
            return;
        }

        SWITCH_CACHE.remove(
                key(
                        level,
                        accessPointPos
                )
        );
    }

    private static NetworkSwitchBlockEntity cachedOrFindLinkedSwitch(
            Level level,
            BlockPos apPos
    ) {
        AttachmentKey attachmentKey =
                key(
                        level,
                        apPos
                );

        NetworkSwitchBlockEntity direct =
                directlyBoundRouterSwitch(
                        level,
                        apPos
                );

        if (direct != null) {
            if (!direct.w119EnsureAccessPointAttachment(
                    apPos
            )) {
                SWITCH_CACHE.remove(
                        attachmentKey
                );
                return null;
            }

            SWITCH_CACHE.put(
                    attachmentKey,
                    direct.getBlockPos()
                            .immutable()
            );

            return direct;
        }

        BlockPos cached =
                SWITCH_CACHE.get(
                        attachmentKey
                );

        if (cached != null) {
            BlockEntity candidate =
                    level.getBlockEntity(cached);

            if (candidate
                    instanceof NetworkSwitchBlockEntity networkSwitch
                    && isPhysicallyLinked(
                    level,
                    apPos,
                    networkSwitch
            )
                    && networkSwitch
                    .w119EnsureAccessPointAttachment(
                            apPos
                    )) {
                return networkSwitch;
            }

            SWITCH_CACHE.remove(
                    attachmentKey
            );
        }

        NetworkSwitchBlockEntity found =
                findLinkedSwitch(
                        level,
                        apPos
                );

        if (found != null
                && !found.w119EnsureAccessPointAttachment(
                apPos
        )) {
            found = null;
        }

        if (found != null) {
            SWITCH_CACHE.put(
                    attachmentKey,
                    found.getBlockPos().immutable()
            );
        }

        return found;
    }

    private static NetworkSwitchBlockEntity directlyBoundRouterSwitch(
            Level level,
            BlockPos apPos
    ) {
        if (level == null
                || apPos == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        apPos
                );

        if (!(blockEntity
                instanceof RtAc68uRouterBlockEntity router)) {
            return null;
        }

        for (String interfaceName
                : new String[]{
                "lan0",
                "lan1"
        }) {
            BlockPos peer =
                    router.w121EthernetPeer(
                            interfaceName
                    );

            if (peer == null) {
                continue;
            }

            BlockEntity peerEntity =
                    level.getBlockEntity(
                            peer
                    );

            if (peerEntity
                    instanceof NetworkSwitchBlockEntity networkSwitch) {
                return networkSwitch;
            }
        }

        return null;
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

                    if (isPhysicallyLinked(
                            level,
                            apPos,
                            networkSwitch
                    )) {
                        return networkSwitch;
                    }
                }
            }
        }

        return null;
    }

    private static boolean isPhysicallyLinked(
            Level level,
            BlockPos apPos,
            NetworkSwitchBlockEntity networkSwitch
    ) {
        if (level == null
                || apPos == null
                || networkSwitch == null) {
            return false;
        }

        if (networkSwitch
                .getConnectedDevices()
                .contains(apPos)) {
            return true;
        }

        BlockEntity accessPoint =
                level.getBlockEntity(apPos);

        if (!(accessPoint instanceof RtAc68uRouterBlockEntity router)) {
            return false;
        }

        return !router
                .w121InterfaceForPeer(
                        networkSwitch.getBlockPos()
                )
                .isBlank();
    }

    private static AttachmentKey key(
            Level level,
            BlockPos apPos
    ) {
        return new AttachmentKey(
                level.dimension()
                        .location()
                        .toString(),
                apPos.asLong()
        );
    }

    private record AttachmentKey(
            String dimension,
            long accessPointPos
    ) {
    }
}
