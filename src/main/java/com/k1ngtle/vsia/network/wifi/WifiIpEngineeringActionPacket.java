package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpAction;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpEngineeringService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiIpEngineeringActionPacket {
    private static final double MAX_DISTANCE_SQUARED =
            64.0 * 64.0;

    private final BlockPos pos;
    private final WifiIpAction action;

    public WifiIpEngineeringActionPacket(
            BlockPos pos,
            WifiIpAction action
    ) {
        this.pos =
                pos.immutable();

        this.action =
                action;
    }

    public WifiIpEngineeringActionPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.action =
                buf.readEnum(
                        WifiIpAction.class
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeEnum(
                action
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () -> {
                    ServerPlayer player =
                            context.getSender();

                    if (player == null
                            || player.blockPosition()
                            .distSqr(
                                    pos
                            )
                            > MAX_DISTANCE_SQUARED) {
                        return;
                    }

                    WifiEngineeringResolution resolution =
                            WifiEngineeringTargetResolver.resolve(
                                    player.level(),
                                    pos
                            );

                    if (!resolution.resolved()) {
                        return;
                    }

                    VsiaNetwork.sendToPlayer(
                            player,
                            new WifiIpEngineeringSnapshotPacket(
                                    pos,
                                    WifiIpEngineeringService.execute(
                                            resolution.target()
                                                    .device(),
                                            action
                                    )
                            )
                    );
                }
        );

        context.setPacketHandled(
                true
        );
    }
}
