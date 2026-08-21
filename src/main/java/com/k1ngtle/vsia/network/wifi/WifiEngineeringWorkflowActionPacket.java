package com.k1ngtle.vsia.network.wifi;




import com.k1ngtle.vsia.signality.engineering.wifi.instrument.W1191LocalTargetAuthorization;
import net.minecraft.network.chat.Component;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowAction;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class WifiEngineeringWorkflowActionPacket {
    private static final double MAX_DISTANCE_SQUARED =
            64.0 * 64.0;

    private final BlockPos pos;
    private final WifiEngineeringWorkflowAction action;

    public WifiEngineeringWorkflowActionPacket(
            BlockPos pos,
            WifiEngineeringWorkflowAction action
    ) {
        this.pos =
                pos.immutable();

        this.action =
                action;
    }

    public WifiEngineeringWorkflowActionPacket(
            FriendlyByteBuf buf
    ) {
        this.pos =
                buf.readBlockPos();

        this.action =
                buf.readEnum(
                        WifiEngineeringWorkflowAction.class
                );
    }

    public void toBytes(
            FriendlyByteBuf buf
    ) {
        buf.writeBlockPos(pos);
        buf.writeEnum(action);
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

            // W1.19.1 V4 LOCAL ENDPOINT AUTH BEGIN
            BlockPos w1191Anchor =
                    this.pos;

            if (!W1191LocalTargetAuthorization
                    .isLocalWifiEndpoint(
                            player.serverLevel(),
                            w1191Anchor
                    )) {
                player.sendSystemMessage(
                        Component.literal(
                                "W1.19.1 REMOTE ENGINEERING VIEW: "
                                        + "configuration blocked at "
                                        + w1191Anchor.getX()
                                        + " "
                                        + w1191Anchor.getY()
                                        + " "
                                        + w1191Anchor.getZ()
                                        + ". The engineering resolver reached "
                                        + "a different Wi-Fi endpoint. "
                                        + "Open/configure that actual endpoint "
                                        + "to change Wi-Fi role/workflow state."
                        )
                );
                return;
            }
            // W1.19.1 V4 LOCAL ENDPOINT AUTH END


                    if (player == null
                            || player.blockPosition()
                            .distSqr(pos)
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
                            new WifiEngineeringWorkflowSnapshotPacket(
                                    pos,
                                    WifiEngineeringWorkflowService.execute(
                                            resolution.target()
                                                .device(),
                                            action
                                    )
                            )
                    );
                }
        );

        context.setPacketHandled(true);
    }
}
