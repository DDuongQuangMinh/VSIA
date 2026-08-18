package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkResult;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiEngineeringTestLinkPacket {
    private static final double MAX_DISTANCE_SQUARED =
            64.0 * 64.0;

    private final BlockPos pos;
    private final int frameBytes;

    public WifiEngineeringTestLinkPacket(
            BlockPos pos,
            int frameBytes
    ) {
        this.pos =
                pos.immutable();

        this.frameBytes =
                frameBytes;
    }

    public WifiEngineeringTestLinkPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.frameBytes =
                buf.readVarInt();
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeVarInt(
                frameBytes
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
                        VsiaNetwork.sendToPlayer(
                                player,
                                new WifiEngineeringTestLinkResultPacket(
                                        pos,
                                        new WifiEngineeringTestLinkResult(
                                                false,
                                                null,
                                                Double.NaN,
                                                0,
                                                resolution.failureDetail()
                                        )
                                )
                        );

                        return;
                    }

                    WifiEngineeringTestLinkResult result =
                            WifiEngineeringTestLinkService.run(
                                    resolution.target()
                                            .device(),
                                    frameBytes
                            );

                    VsiaNetwork.sendToPlayer(
                            player,
                            new WifiEngineeringTestLinkResultPacket(
                                    pos,
                                    result
                            )
                    );
                }
        );

        context.setPacketHandled(
                true
        );
    }
}
