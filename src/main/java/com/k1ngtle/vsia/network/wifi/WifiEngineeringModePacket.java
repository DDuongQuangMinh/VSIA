package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiEngineeringModePacket {
    private static final double MAX_DISTANCE_SQUARED =
            64.0 * 64.0;

    private final BlockPos pos;
    private final WifiLivePhyMode mode;

    public WifiEngineeringModePacket(
            BlockPos pos,
            WifiLivePhyMode mode
    ) {
        this.pos =
                pos.immutable();

        this.mode =
                mode;
    }

    public WifiEngineeringModePacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.mode =
                buf.readEnum(
                        WifiLivePhyMode.class
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeEnum(
                mode
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

                    BlockEntity blockEntity =
                            player.level()
                                    .getBlockEntity(
                                            pos
                                    );

                    if (!(blockEntity
                            instanceof NetworkDeviceBlockEntity device)
                            || !WifiEngineeringProbe.supports(
                            device
                    )) {
                        return;
                    }

                    device.setWifiLivePhyMode(
                            mode
                    );

                    VsiaNetwork.sendToPlayer(
                            player,
                            new WifiEngineeringSnapshotPacket(
                                    pos,
                                    WifiEngineeringProbe.capture(
                                            device
                                    ),
                                    false
                            )
                    );
                }
        );

        context.setPacketHandled(
                true
        );
    }
}
