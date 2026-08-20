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
public final class WifiRawIpv4ReassemblyLiveCommand {
    private WifiRawIpv4ReassemblyLiveCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wifiw1reassembly")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("timeout")
                                        .then(posArgumentsTimeout())
                        )
                        .then(
                                Commands.literal("partial")
                                        .then(posArgumentsPartial())
                        )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder
    <net.minecraft.commands.CommandSourceStack, Integer> posArgumentsTimeout() {
        return Commands.argument(
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
                                        "millis",
                                        IntegerArgumentType.integer(
                                                100,
                                                120000
                                        )
                                ).executes(context -> {
                                    NetworkDeviceBlockEntity device =
                                            resolve(
                                                    context.getSource(),
                                                    IntegerArgumentType.getInteger(context, "x"),
                                                    IntegerArgumentType.getInteger(context, "y"),
                                                    IntegerArgumentType.getInteger(context, "z")
                                            );

                                    if (device == null) {
                                        return 0;
                                    }

                                    int millis =
                                            IntegerArgumentType.getInteger(
                                                    context,
                                                    "millis"
                                            );

                                    device.setWifiRawIpv4ReassemblyTimeoutMillis(
                                            millis
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Raw IPv4 reassembly timeout="
                                                            + millis
                                                            + " ms"
                                            ),
                                            false
                                    );

                                    return 1;
                                })
                        )
                )
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder
    <net.minecraft.commands.CommandSourceStack, Integer> posArgumentsPartial() {
        return Commands.argument(
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
                                        "target",
                                        StringArgumentType.word()
                                ).then(
                                        Commands.argument(
                                                "payloadBytes",
                                                IntegerArgumentType.integer(
                                                        1,
                                                        60000
                                                )
                                        ).then(
                                                Commands.argument(
                                                        "dropIndex",
                                                        IntegerArgumentType.integer(
                                                                0,
                                                                63
                                                        )
                                                ).executes(context -> {
                                                    NetworkDeviceBlockEntity device =
                                                            resolve(
                                                                    context.getSource(),
                                                                    IntegerArgumentType.getInteger(context, "x"),
                                                                    IntegerArgumentType.getInteger(context, "y"),
                                                                    IntegerArgumentType.getInteger(context, "z")
                                                            );

                                                    if (device == null) {
                                                        return 0;
                                                    }

                                                    String target =
                                                            StringArgumentType.getString(
                                                                    context,
                                                                    "target"
                                                            );

                                                    int bytes =
                                                            IntegerArgumentType.getInteger(
                                                                    context,
                                                                    "payloadBytes"
                                                            );

                                                    int dropIndex =
                                                            IntegerArgumentType.getInteger(
                                                                    context,
                                                                    "dropIndex"
                                                            );

                                                    if (!device.sendWifiRawUdpFragmentProbeWithDrop(
                                                            target,
                                                            bytes,
                                                            dropIndex
                                                    )) {
                                                        context.getSource().sendFailure(
                                                                Component.literal(
                                                                        "Partial raw IPv4 probe could not start."
                                                                )
                                                        );
                                                        return 0;
                                                    }

                                                    context.getSource().sendSuccess(
                                                            () -> Component.literal(
                                                                    "Partial raw IPv4 probe started target="
                                                                            + target
                                                                            + " payload="
                                                                            + bytes
                                                                            + " dropIndex="
                                                                            + dropIndex
                                                            ),
                                                            false
                                                    );

                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static NetworkDeviceBlockEntity resolve(
            net.minecraft.commands.CommandSourceStack source,
            int x,
            int y,
            int z
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
            return null;
        }

        return resolution.target()
                .device();
    }
}
