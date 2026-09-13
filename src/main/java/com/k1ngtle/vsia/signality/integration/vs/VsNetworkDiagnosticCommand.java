package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.Vsia;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VsNetworkDiagnosticCommand {
    private VsNetworkDiagnosticCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        register(
                event.getDispatcher()
        );
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
                Commands.literal(
                                "vsiavspos"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.argument(
                                                "pos",
                                                BlockPosArgument.blockPos()
                                        )
                                        .executes(
                                                VsNetworkDiagnosticCommand::inspect
                                        )
                        )
        );
    }

    private static int inspect(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        BlockPos pos =
                BlockPosArgument.getLoadedBlockPos(
                        context,
                        "pos"
                );

        Vec3 world =
                VsNetworkPosition.blockCenterWorld(
                        context.getSource()
                                .getLevel(),
                        pos
                );

        boolean ship =
                VsNetworkPosition.onVsShip(
                        context.getSource()
                                .getLevel(),
                        pos
                );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                String.format(
                                                        Locale.ROOT,
                                                        "VSIA network pose | local %s | world %.3f %.3f %.3f | %s",
                                                        pos.toShortString(),
                                                        world.x,
                                                        world.y,
                                                        world.z,
                                                        ship
                                                                ? "VS SHIP"
                                                                : "WORLD"
                                                )
                                        )
                                        .withStyle(
                                                ship
                                                        ? ChatFormatting.AQUA
                                                        : ChatFormatting.GRAY
                                        ),
                        false
                );

        return 1;
    }
}
