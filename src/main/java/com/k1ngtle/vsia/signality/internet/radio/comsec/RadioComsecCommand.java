package com.k1ngtle.vsia.signality.internet.radio.comsec;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.radio.device.TemporaryRadioItem;
import com.k1ngtle.vsia.signality.internet.radio.portable.PortableRadioState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadioComsecCommand {
    private RadioComsecCommand() {
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
                                "vsiacomsec"
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                RadioComsecCommand::status
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "selftest"
                                        )
                                        .executes(
                                                RadioComsecCommand::selfTest
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "mismatchtest"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "otherSlot",
                                                                IntegerArgumentType.integer(
                                                                        RadioComsecKeyStoreSavedData.MIN_SLOT,
                                                                        RadioComsecKeyStoreSavedData.MAX_SLOT
                                                                )
                                                        )
                                                        .executes(
                                                                RadioComsecCommand::mismatchTest
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "rotate"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "slot",
                                                                IntegerArgumentType.integer(
                                                                        RadioComsecKeyStoreSavedData.MIN_SLOT,
                                                                        RadioComsecKeyStoreSavedData.MAX_SLOT
                                                                )
                                                        )
                                                        .executes(
                                                                RadioComsecCommand::rotate
                                                        )
                                        )
                        )
        );
    }

    private static int status(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        ItemStack stack =
                heldRadio(
                        player
                );

        if (stack.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                                    "Hold a Temporary Field Radio first."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return 0;
        }

        boolean secure =
                PortableRadioState.comsecEnabled(
                        stack
                );

        int slot =
                PortableRadioState.comsecSlot(
                        stack
                );

        RadioComsecKeyStoreSavedData.KeyMaterial material =
                RadioComsecKeyStoreSavedData
                        .get(
                                player.serverLevel()
                        )
                        .material(
                                slot
                        );

        player.sendSystemMessage(
                Component.literal(
                                "COMSEC "
                                        + (
                                        secure
                                                ? "SECURE"
                                                : "CLEAR"
                                )
                                        + " | "
                                        + material.keyId()
                                        + " | "
                                        + RadioComsecEngine.ALGORITHM
                        )
                        .withStyle(
                                secure
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.GRAY
                        )
        );

        return 1;
    }

    private static int selfTest(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        ItemStack stack =
                heldRadio(
                        player
                );

        if (stack.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                                    "Hold a Temporary Field Radio first."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return 0;
        }

        int slot =
                PortableRadioState.comsecSlot(
                        stack
                );

        boolean ok =
                RadioComsecEngine.selfTest(
                        player.serverLevel(),
                        slot
                );

        player.sendSystemMessage(
                Component.literal(
                                ok
                                        ? "COMSEC self-test PASS: AES-256-GCM encrypt/authenticate/decrypt."
                                        : "COMSEC self-test FAILED."
                        )
                        .withStyle(
                                ok
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        )
        );

        return ok
                ? 1
                : 0;
    }

    private static int mismatchTest(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        ItemStack stack =
                heldRadio(
                        player
                );

        if (stack.isEmpty()) {
            player.sendSystemMessage(
                    Component.literal(
                                    "Hold a Temporary Field Radio first."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return 0;
        }

        int transmitSlot =
                PortableRadioState.comsecSlot(
                        stack
                );

        int receiveSlot =
                IntegerArgumentType.getInteger(
                        context,
                        "otherSlot"
                );

        if (transmitSlot == receiveSlot) {
            player.sendSystemMessage(
                    Component.literal(
                                    "Choose a different slot from the radio's current TEK slot."
                            )
                            .withStyle(
                                    ChatFormatting.YELLOW
                            )
            );

            return 0;
        }

        boolean rejected =
                RadioComsecEngine.mismatchSelfTest(
                        player.serverLevel(),
                        transmitSlot,
                        receiveSlot
                );

        player.sendSystemMessage(
                Component.literal(
                                rejected
                                        ? "COMSEC negative test PASS: mismatched TEK traffic was rejected."
                                        : "COMSEC negative test FAILED: mismatched TEK traffic was accepted."
                        )
                        .withStyle(
                                rejected
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        )
        );

        return rejected
                ? 1
                : 0;
    }

    private static int rotate(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player =
                context.getSource()
                        .getPlayerOrException();

        int slot =
                IntegerArgumentType.getInteger(
                        context,
                        "slot"
                );

        RadioComsecKeyStoreSavedData.KeyMaterial material =
                RadioComsecKeyStoreSavedData
                        .get(
                                player.serverLevel()
                        )
                        .rotate(
                                slot
                        );

        context.getSource()
                .sendSuccess(
                        () ->
                                Component.literal(
                                                "Rotated "
                                                        + material.keyId()
                                                        + ". Radios using an older epoch will no longer authenticate."
                                        )
                                        .withStyle(
                                                ChatFormatting.AQUA
                                        ),
                        true
                );

        return 1;
    }

    private static ItemStack heldRadio(
            ServerPlayer player
    ) {
        ItemStack main =
                player.getMainHandItem();

        if (main.getItem()
                instanceof TemporaryRadioItem) {
            return main;
        }

        ItemStack off =
                player.getOffhandItem();

        if (off.getItem()
                instanceof TemporaryRadioItem) {
            return off;
        }

        return ItemStack.EMPTY;
    }
}
