package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW116FirewallLiveCommand {
    private WifiW116FirewallLiveCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1firewall")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("nat44")
                                        .then(
                                                Commands.argument("x", IntegerArgumentType.integer())
                                                        .then(
                                                                Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(
                                                                                Commands.argument("z", IntegerArgumentType.integer())
                                                                                        .then(
                                                                                                Commands.argument("publicIp", StringArgumentType.word())
                                                                                                        .executes(context ->
                                                                                                                enableNat(
                                                                                                                        context.getSource(),
                                                                                                                        IntegerArgumentType.getInteger(context, "x"),
                                                                                                                        IntegerArgumentType.getInteger(context, "y"),
                                                                                                                        IntegerArgumentType.getInteger(context, "z"),
                                                                                                                        StringArgumentType.getString(context, "publicIp")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("status")
                                        .then(
                                                Commands.argument("x", IntegerArgumentType.integer())
                                                        .then(
                                                                Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(
                                                                                Commands.argument("z", IntegerArgumentType.integer())
                                                                                        .executes(context ->
                                                                                                status(
                                                                                                        context.getSource(),
                                                                                                        IntegerArgumentType.getInteger(context, "x"),
                                                                                                        IntegerArgumentType.getInteger(context, "y"),
                                                                                                        IntegerArgumentType.getInteger(context, "z")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("reset")
                                        .then(
                                                Commands.argument("x", IntegerArgumentType.integer())
                                                        .then(
                                                                Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(
                                                                                Commands.argument("z", IntegerArgumentType.integer())
                                                                                        .executes(context ->
                                                                                                reset(
                                                                                                        context.getSource(),
                                                                                                        IntegerArgumentType.getInteger(context, "x"),
                                                                                                        IntegerArgumentType.getInteger(context, "y"),
                                                                                                        IntegerArgumentType.getInteger(context, "z")
                                                                                                )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static FirewallBlockEntity firewall(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        if (source.getLevel().getBlockEntity(
                new BlockPos(x, y, z)
        ) instanceof FirewallBlockEntity firewall) {
            return firewall;
        }

        return null;
    }

    private static int enableNat(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z,
            String publicIp
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(Component.literal("W1.16 target is not a FirewallBlockEntity"));
            return 0;
        }

        firewall.osSimulators[0].w116EnableNat44(publicIp);
        firewall.setChanged();

        source.sendSuccess(
                () -> Component.literal(
                        "W1.16 NAT44/PAT enabled public="
                                + publicIp
                ),
                false
        );

        return 1;
    }

    private static int status(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(Component.literal("W1.16 target is not a FirewallBlockEntity"));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        firewall.osSimulators[0].w116Status()
                ),
                false
        );

        return 1;
    }

    private static int reset(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
    ) {
        FirewallBlockEntity firewall = firewall(source, x, y, z);

        if (firewall == null) {
            source.sendFailure(Component.literal("W1.16 target is not a FirewallBlockEntity"));
            return 0;
        }

        firewall.osSimulators[0].w116Reset();
        firewall.setChanged();

        source.sendSuccess(
                () -> Component.literal("W1.16 firewall state reset"),
                false
        );

        return 1;
    }
}
