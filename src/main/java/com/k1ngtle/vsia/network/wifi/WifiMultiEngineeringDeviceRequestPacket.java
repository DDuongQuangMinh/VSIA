package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class WifiMultiEngineeringDeviceRequestPacket {
    private static final double MAX_DISTANCE_SQUARED = 64.0D * 64.0D;

    private final UUID deviceId;

    public WifiMultiEngineeringDeviceRequestPacket(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public WifiMultiEngineeringDeviceRequestPacket(FriendlyByteBuf buf) {
        deviceId = buf.readUUID();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(deviceId);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(
                () -> {
                    ServerPlayer player = context.getSender();

                    if (player == null) {
                        return;
                    }

                    NetworkDeviceBlockEntity device =
                            WifiEngineeringDeviceIdentityResolver.resolve(
                                    player.serverLevel(),
                                    deviceId
                            );

                    if (device == null) {
                        VsiaNetwork.sendToPlayer(
                                player,
                                WifiMultiEngineeringDeviceSnapshotPacket.unresolved(
                                        deviceId,
                                        "UUID target is not currently loaded"
                                )
                        );
                        return;
                    }

                    Vec3 world = device.positionWorld();

                    double dx = player.getX() - world.x;
                    double dy = player.getY() - world.y;
                    double dz = player.getZ() - world.z;

                    if (dx * dx + dy * dy + dz * dz > MAX_DISTANCE_SQUARED) {
                        VsiaNetwork.sendToPlayer(
                                player,
                                WifiMultiEngineeringDeviceSnapshotPacket.unresolved(
                                        deviceId,
                                        "UUID target is more than 64 blocks away"
                                )
                        );
                        return;
                    }

                    VsiaNetwork.sendToPlayer(
                            player,
                            WifiMultiEngineeringDeviceSnapshotPacket.capture(
                                    device,
                                    ""
                            )
                    );
                }
        );

        context.setPacketHandled(true);
    }
}
