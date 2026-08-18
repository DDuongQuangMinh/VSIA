package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringFormatter;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringProbe;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestResult;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestSuite;
import com.k1ngtle.vsia.signality.engineering.wifi.live.WifiLivePhyMode;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
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
public final class WifiEngineeringCommand {
    private WifiEngineeringCommand() {
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
                                "wifiw1device"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "pos",
                                                                BlockPosArgument
                                                                        .blockPos()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        status(
                                                                                context.getSource(),
                                                                                BlockPosArgument
                                                                                        .getLoadedBlockPos(
                                                                                                context,
                                                                                                "pos"
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "mode"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "pos",
                                                                BlockPosArgument
                                                                        .blockPos()
                                                        )
                                                        .then(
                                                                Commands.literal(
                                                                                "analytical"
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        setMode(
                                                                                                context.getSource(),
                                                                                                BlockPosArgument
                                                                                                        .getLoadedBlockPos(
                                                                                                                context,
                                                                                                                "pos"
                                                                                                        ),
                                                                                                WifiLivePhyMode
                                                                                                        .ANALYTICAL
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal(
                                                                                "bit-level"
                                                                        )
                                                                        .executes(
                                                                                context ->
                                                                                        setMode(
                                                                                                context.getSource(),
                                                                                                BlockPosArgument
                                                                                                        .getLoadedBlockPos(
                                                                                                                context,
                                                                                                                "pos"
                                                                                                        ),
                                                                                                WifiLivePhyMode
                                                                                                        .BIT_LEVEL_AUTO
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )
        );

        dispatcher.register(
                Commands.literal(
                                "wifiw1instrumenttest"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .executes(
                                context ->
                                        runTests(
                                                context.getSource()
                                        )
                        )
        );
    }

    private static int status(
            CommandSourceStack source,
            BlockPos pos
    ) {
        NetworkDeviceBlockEntity device =
                deviceAt(
                        source,
                        pos
                );

        if (device == null) {
            return 0;
        }

        if (!WifiEngineeringProbe.supports(
                device
        )) {
            source.sendFailure(
                    Component.literal(
                            "Target network device is not using a Wi-Fi profile"
                    )
            );

            return 0;
        }

        WifiEngineeringSnapshot snapshot =
                WifiEngineeringProbe.capture(
                        device
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Wi-Fi Engineering Probe @ "
                                        + pos.toShortString()
                        ).withStyle(
                                ChatFormatting.AQUA
                        ),
                false
        );

        for (String line
                : WifiEngineeringFormatter.lines(
                snapshot
        )) {
            source.sendSuccess(
                    () ->
                            Component.literal(
                                    line
                            ).withStyle(
                                    ChatFormatting.GRAY
                            ),
                    false
            );
        }

        return 1;
    }

    private static int setMode(
            CommandSourceStack source,
            BlockPos pos,
            WifiLivePhyMode mode
    ) {
        NetworkDeviceBlockEntity device =
                deviceAt(
                        source,
                        pos
                );

        if (device == null) {
            return 0;
        }

        if (!WifiEngineeringProbe.supports(
                device
        )) {
            source.sendFailure(
                    Component.literal(
                            "Target network device is not using a Wi-Fi profile"
                    )
            );

            return 0;
        }

        device.setWifiLivePhyMode(
                mode
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Wi-Fi live PHY mode at "
                                        + pos.toShortString()
                                        + " -> "
                                        + mode
                        ).withStyle(
                                ChatFormatting.GREEN
                        ),
                true
        );

        return 1;
    }

    private static NetworkDeviceBlockEntity deviceAt(
            CommandSourceStack source,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                source.getLevel()
                        .getBlockEntity(
                                pos
                        );

        if (!(blockEntity
                instanceof NetworkDeviceBlockEntity device)) {
            source.sendFailure(
                    Component.literal(
                            "No VSIA NetworkDeviceBlockEntity at "
                                    + pos.toShortString()
                    )
            );

            return null;
        }

        return device;
    }

    private static int runTests(
            CommandSourceStack source
    ) {
        int passed =
                0;

        int failed =
                0;

        for (WifiEngineeringTestResult result
                : WifiEngineeringTestSuite.runAll()) {
            if (result.passed()) {
                passed++;

                source.sendSuccess(
                        () ->
                                Component.literal(
                                        "[PASS] "
                                                + result.id()
                                ).withStyle(
                                        ChatFormatting.GREEN
                                ),
                        false
                );
            } else {
                failed++;

                source.sendFailure(
                        Component.literal(
                                "[FAIL] "
                                        + result.id()
                        )
                );
            }

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "  "
                                            + result.detail()
                            ).withStyle(
                                    ChatFormatting.DARK_GRAY
                            ),
                    false
            );
        }

        int finalPassed =
                passed;

        int finalFailed =
                failed;

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Result: "
                                        + finalPassed
                                        + " passed, "
                                        + finalFailed
                                        + " failed"
                        ).withStyle(
                                finalFailed == 0
                                        ? ChatFormatting.GREEN
                                        : ChatFormatting.RED
                        ),
                false
        );

        return failed == 0
                ? 1
                : 0;
    }
}
