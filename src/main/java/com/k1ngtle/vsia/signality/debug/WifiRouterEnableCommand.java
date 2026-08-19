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

import com.mojang.brigadier.arguments.BoolArgumentType;

@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WifiRouterEnableCommand {
    private WifiRouterEnableCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1routerenable")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                        .then(Commands.argument("z", IntegerArgumentType.integer())
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> run(
                                context.getSource(),
                                new BlockPos(
                                        IntegerArgumentType.getInteger(context, "x"),
                                        IntegerArgumentType.getInteger(context, "y"),
                                        IntegerArgumentType.getInteger(context, "z")
                                ),
                                BoolArgumentType.getBool(context, "enabled")
                        ))))))
        );
    }

    private static int run(CommandSourceStack source, BlockPos pos, boolean enabled) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(source.getLevel(), pos);

        if (!resolution.resolved()) {
            source.sendFailure(Component.literal(resolution.failureDetail()));
            return 0;
        }

        resolution.target().device().setWifiLiveRouterEnabled(enabled);

        source.sendSuccess(
                () -> Component.literal(
                        "Live router " + (enabled ? "enabled" : "disabled")
                ),
                false
        );

        return 1;
    }
}
