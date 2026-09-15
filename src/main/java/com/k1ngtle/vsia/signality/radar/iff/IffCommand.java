package com.k1ngtle.vsia.signality.radar.iff;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class IffCommand {
    private IffCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("iffnet")
                        .then(
                                Commands.literal("status")
                                        .executes(ctx -> status(ctx, "default"))
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                ctx -> status(
                                                                        ctx,
                                                                        StringArgumentType.getString(
                                                                                ctx,
                                                                                "network"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("key")
                                        .requires(source -> source.hasPermission(2))
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .then(
                                                                Commands.argument(
                                                                                "key",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(IffCommand::setKey)
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("reset")
                                        .requires(source -> source.hasPermission(2))
                                        .then(
                                                Commands.argument(
                                                                "network",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(IffCommand::resetKey)
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context,
            String networkId
    ) {
        boolean custom = IffNetworkKeyRegistry.hasCustomKey(networkId);
        context.getSource().sendSuccess(
                () -> Component.literal(
                                "IFF network '"
                                        + networkId
                                        + "': "
                                        + (custom
                                        ? "custom authentication key"
                                        : "default test authentication key")
                        )
                        .withStyle(ChatFormatting.AQUA),
                false
        );
        return 1;
    }

    private static int setKey(CommandContext<CommandSourceStack> context) {
        String network = StringArgumentType.getString(context, "network");
        String key = StringArgumentType.getString(context, "key");
        IffNetworkKeyRegistry.setKey(network, key);
        context.getSource().sendSuccess(
                () -> Component.literal(
                                "Updated IFF authentication key for network '"
                                        + network
                                        + "'."
                        )
                        .withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int resetKey(CommandContext<CommandSourceStack> context) {
        String network = StringArgumentType.getString(context, "network");
        IffNetworkKeyRegistry.resetKey(network);
        context.getSource().sendSuccess(
                () -> Component.literal(
                                "Reset IFF authentication key for network '"
                                        + network
                                        + "' to the default test key."
                        )
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
        return 1;
    }
}
