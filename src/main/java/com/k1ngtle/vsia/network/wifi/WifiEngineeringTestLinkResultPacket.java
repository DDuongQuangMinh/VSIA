package com.k1ngtle.vsia.network.wifi;

import com.k1ngtle.vsia.client.wifi.WifiEngineeringClientPacketHandler;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiEngineeringTestLinkResultPacket {
    private final BlockPos pos;
    private final WifiEngineeringTestLinkResult result;

    public WifiEngineeringTestLinkResultPacket(
            BlockPos pos,
            WifiEngineeringTestLinkResult result
    ) {
        this.pos =
                pos.immutable();

        this.result =
                result;
    }

    public WifiEngineeringTestLinkResultPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        boolean success =
                buf.readBoolean();

        BlockPos peer =
                buf.readBlockPos();

        double distance =
                buf.readDouble();

        int frameBytes =
                buf.readVarInt();

        String detail =
                buf.readUtf(
                        1024
                );

        this.result =
                new WifiEngineeringTestLinkResult(
                        success,
                        peer,
                        distance,
                        frameBytes,
                        detail
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(
                pos
        );

        buf.writeBoolean(
                result.success()
        );

        buf.writeBlockPos(
                result.peerPos()
        );

        buf.writeDouble(
                result.distanceBlocks()
        );

        buf.writeVarInt(
                result.frameBytes()
        );

        buf.writeUtf(
                result.detail(),
                1024
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        context.enqueueWork(
                () ->
                        DistExecutor.unsafeRunWhenOn(
                                Dist.CLIENT,
                                () ->
                                        () ->
                                                WifiEngineeringClientPacketHandler
                                                        .handleTestLinkResult(
                                                                pos,
                                                                result
                                                        )
                        )
        );

        context.setPacketHandled(
                true
        );
    }
}
