package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
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
public final class WifiRawIpv4HeaderValidationLiveCommand {
    private WifiRawIpv4HeaderValidationLiveCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("wifiw1headervalidation")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("inject")
                                        .then(
                                                Commands.argument(
                                                        "x",
                                                        IntegerArgumentType.integer()
                                                ).then(
                                                        Commands.argument(
                                                                "y",
                                                                IntegerArgumentType.integer()
                                                        ).then(
                                                                Commands.argument(
                                                                        "z",
                                                                        IntegerArgumentType.integer()
                                                                ).then(
                                                                        Commands.argument(
                                                                                "nextHopMac",
                                                                                StringArgumentType.word()
                                                                        ).then(
                                                                                Commands.argument(
                                                                                        "targetIp",
                                                                                        StringArgumentType.word()
                                                                                ).then(
                                                                                        Commands.argument(
                                                                                                "fault",
                                                                                                StringArgumentType.word()
                                                                                        ).executes(context ->
                                                                                                runInject(
                                                                                                        context.getSource(),
                                                                                                        IntegerArgumentType.getInteger(context, "x"),
                                                                                                        IntegerArgumentType.getInteger(context, "y"),
                                                                                                        IntegerArgumentType.getInteger(context, "z"),
                                                                                                        StringArgumentType.getString(context, "nextHopMac"),
                                                                                                        StringArgumentType.getString(context, "targetIp"),
                                                                                                        StringArgumentType.getString(context, "fault")
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                        )
        );
    }

    private static int runInject(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z,
            String nextHopMac,
            String targetIp,
            String fault
    ) {
        WifiEngineeringResolution resolution =
                WifiEngineeringTargetResolver.resolve(
                        source.getLevel(),
                        new BlockPos(x, y, z)
                );

        if (!resolution.resolved()) {
            source.sendFailure(
                    Component.literal(
                            resolution.failureDetail()
                    )
            );
            return 0;
        }

        NetworkDeviceBlockEntity device =
                resolution.target()
                        .device();

        try {
            if (!device.sendWifiRawIpv4HeaderFaultProbe(
                    nextHopMac,
                    targetIp,
                    fault
            )) {
                source.sendFailure(
                        Component.literal(
                                "W1.14 header fault probe could not start."
                        )
                );
                return 0;
            }
        } catch (IllegalArgumentException exception) {
            source.sendFailure(
                    Component.literal(
                            exception.getMessage()
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "W1.14 raw IPv4 header fault sent"
                                + " target="
                                + targetIp
                                + " nextHopMac="
                                + nextHopMac
                                + " fault="
                                + fault
                ),
                false
        );

        return 1;
    }
}
