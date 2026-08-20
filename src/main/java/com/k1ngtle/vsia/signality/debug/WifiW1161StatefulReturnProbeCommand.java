package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW1161StatefulReturnProbeCommand {
    private WifiW1161StatefulReturnProbeCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewalltransit")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("reply")
                                        .then(positionArguments())
                        )
        );

        event.getDispatcher().register(
                Commands.literal("wifiw1firewallreply")
                        .requires(source -> source.hasPermission(2))
                        .then(positionArguments())
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<
            net.minecraft.commands.CommandSourceStack,
            Integer> positionArguments() {
        return Commands.argument("fx", IntegerArgumentType.integer())
                .then(
                        Commands.argument("fy", IntegerArgumentType.integer())
                                .then(
                                        Commands.argument("fz", IntegerArgumentType.integer())
                                                .executes(context -> reply(
                                                        context.getSource(),
                                                        new BlockPos(
                                                                IntegerArgumentType.getInteger(context, "fx"),
                                                                IntegerArgumentType.getInteger(context, "fy"),
                                                                IntegerArgumentType.getInteger(context, "fz")
                                                        )
                                                ))
                                )
                );
    }

    private static int reply(
            net.minecraft.commands.CommandSourceStack source,
            BlockPos firewallPos
    ) {
        BlockEntity blockEntity =
                source.getLevel().getBlockEntity(firewallPos);

        if (!(blockEntity instanceof FirewallBlockEntity firewall)) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 target is not a FirewallBlockEntity"
                    )
            );
            return 0;
        }

        BlockPos wanPos = firewall.getWanConnection();

        if (wanPos == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 WAN world link is not configured"
                    )
            );
            return 0;
        }

        BlockEntity wanEntity =
                source.getLevel().getBlockEntity(wanPos);

        if (!(wanEntity instanceof NetworkSwitchBlockEntity wanSwitch)) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 WAN linked device is not a NetworkSwitchBlockEntity"
                    )
            );
            return 0;
        }

        OSINetworkPacket reply =
                firewall.osSimulators[0]
                        .w1161BuildLatestNatReplyProbe();

        if (reply == null) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 no active NAT44/PAT mapping. "
                                    + "Run the LAN probe first."
                    )
            );
            return 0;
        }

        String mapping =
                firewall.osSimulators[0]
                        .w1161LatestNatMappingSummary();

        boolean injected =
                wanSwitch.w1161InjectProbeToward(
                        firewallPos,
                        reply
                );

        if (!injected) {
            source.sendFailure(
                    Component.literal(
                            "W1.16.1 stateful return probe could not traverse "
                                    + "WAN switch L2 toward firewall"
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16.1 STATEFUL NAT RETURN PROBE injected"
                                + " | WAN-switch="
                                + wanPos.toShortString()
                                + " | firewall="
                                + firewallPos.toShortString()
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "mapping=" + mapping
                ).withStyle(ChatFormatting.YELLOW),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        "reply="
                                + reply.sourceIp
                                + ":"
                                + reply.sourcePort
                                + " -> "
                                + reply.targetIp
                                + ":"
                                + reply.targetPort
                                + " ttl="
                                + reply.ttl
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.w1161TransitStatus()
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        source.sendSuccess(
                () -> Component.literal(
                        firewall.osSimulators[0].w116Status()
                ),
                false
        );

        return 1;
    }
}
