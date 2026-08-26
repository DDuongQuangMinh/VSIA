package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringDeviceIdentityResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowAction;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowService;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public final class WifiMultiEngineeringDeviceActionPacket {
    private static final double MAX_DISTANCE_SQUARED = 64.0D * 64.0D;

    private final UUID deviceId;
    private final WifiEngineeringWorkflowAction action;

    public WifiMultiEngineeringDeviceActionPacket(
            UUID deviceId,
            WifiEngineeringWorkflowAction action
    ) {
        this.deviceId = deviceId;
        this.action = action;
    }

    public WifiMultiEngineeringDeviceActionPacket(FriendlyByteBuf buf) {
        deviceId = buf.readUUID();
        action = buf.readEnum(WifiEngineeringWorkflowAction.class);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(deviceId);
        buf.writeEnum(action);
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
                                        "Action rejected: UUID target is not loaded"
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
                                        "Action rejected: target is over 64 blocks away"
                                )
                        );
                        return;
                    }

                    WifiEngineeringWorkflowSnapshot result =
                            WifiEngineeringWorkflowService.execute(
                                    device,
                                    action
                            );

                    VsiaNetwork.sendToPlayer(
                            player,
                            WifiMultiEngineeringDeviceSnapshotPacket.capture(
                                    device,
                                    result.status()
                            )
                    );
                }
        );

        context.setPacketHandled(true);
    }
}
