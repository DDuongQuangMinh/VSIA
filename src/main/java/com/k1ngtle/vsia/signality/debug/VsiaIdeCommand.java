package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.web.W128IdeServer;
import com.k1ngtle.vsia.signality.internet.web.W128WebBuildResult;
import com.k1ngtle.vsia.signality.internet.web.W128WebProject;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class VsiaIdeCommand {
    private VsiaIdeCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(root("vsiaIDE"));
        dispatcher.register(root("vsiaide"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String literal) {
        return Commands.literal(literal)
                .executes(context -> openDefault(context.getSource()))
                .then(
                        Commands.argument("host", StringArgumentType.word())
                                .executes(context -> openHost(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "host")
                                ))
                );
    }

    private static int openDefault(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<W128WebProject> projects = W128WebRegistrySavedData
                .get(source.getLevel())
                .projectsOwnedBy(player.getUUID());

        if (projects.isEmpty()) {
            source.sendFailure(Component.literal("You do not own a VS:IA IDE project yet."));
            source.sendSuccess(
                    () -> Component.literal("Create one with /web create <host> <static|react>.")
                            .withStyle(ChatFormatting.GRAY),
                    false
            );
            return 0;
        }

        if (projects.size() == 1) {
            return openHost(source, projects.get(0).host());
        }

        source.sendSuccess(
                () -> Component.literal("You own multiple IDE projects. Use /vsiaIDE <host>:")
                        .withStyle(ChatFormatting.AQUA),
                false
        );

        for (W128WebProject project : projects) {
            source.sendSuccess(
                    () -> Component.literal(
                            "  " + project.host()
                                    + " | " + project.mode()
                                    + " | " + (project.published() ? "PUBLISHED" : "DRAFT")
                    ),
                    false
            );
        }

        return projects.size();
    }

    private static int openHost(CommandSourceStack source, String host) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        W128WebBuildResult result = W128IdeServer.open(player, host);

        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(result.message()).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }
}
