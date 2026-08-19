package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WifiRouterInterfaceCommand {
    private WifiRouterInterfaceCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1routeriface")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                        .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("ip", StringArgumentType.word())
                        .then(Commands.argument("prefix", IntegerArgumentType.integer(0, 32))
                        .executes(context -> run(
                                context.getSource(),
                                new BlockPos(
                                        IntegerArgumentType.getInteger(context, "x"),
                                        IntegerArgumentType.getInteger(context, "y"),
                                        IntegerArgumentType.getInteger(context, "z")
                                ),
                                StringArgumentType.getString(context, "name"),
                                StringArgumentType.getString(context, "ip"),
                                IntegerArgumentType.getInteger(context, "prefix")
                        ))))))))
        );
    }

    private static int run(
            CommandSourceStack source,
            BlockPos pos,
            String name,
            String ip,
            int prefix
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(source.getLevel(), pos);

        if (!resolution.resolved()) {
            source.sendFailure(Component.literal(resolution.failureDetail()));
            return 0;
        }

        boolean ok =
                resolution.target().device()
                        .configureWifiLiveRouterInterface(
                                name,
                                ip,
                                prefix
                        );

        if (!ok) {
            source.sendFailure(Component.literal("Router interface rejected."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Router interface "
                                + name
                                + " = "
                                + ip
                                + "/"
                                + prefix
                ),
                false
        );

        return 1;
    }
}
