package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringResolution;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTargetResolver;
import com.k1ngtle.vsia.signality.internet.NetworkDeviceBlockEntity;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
public final class WifiRawFragmentLiveCommand {
    private WifiRawFragmentLiveCommand() {
    }

    @SubscribeEvent
    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "wifiw1rawfraglive"
                                )
                                .requires(
                                        source ->
                                                source.hasPermission(
                                                        2
                                                )
                                )
                                .then(
                                        Commands.argument(
                                                        "x",
                                                        IntegerArgumentType.integer()
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "y",
                                                                        IntegerArgumentType.integer()
                                                                )
                                                                .then(
                                                                        Commands.argument(
                                                                                        "z",
                                                                                        IntegerArgumentType.integer()
                                                                                )
                                                                                .then(
                                                                                        Commands.argument(
                                                                                                        "target",
                                                                                                        StringArgumentType.word()
                                                                                                )
                                                                                                .then(
                                                                                                        Commands.argument(
                                                                                                                        "payloadBytes",
                                                                                                                        IntegerArgumentType.integer(
                                                                                                                                1,
                                                                                                                                60000
                                                                                                                        )
                                                                                                                )
                                                                                                                .then(
                                                                                                                        Commands.argument(
                                                                                                                                        "df",
                                                                                                                                        BoolArgumentType.bool()
                                                                                                                                )
                                                                                                                                .executes(
                                                                                                                                        context -> {
                                                                                                                                            BlockPos requested =
                                                                                                                                                    new BlockPos(
                                                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                                                    context,
                                                                                                                                                                    "x"
                                                                                                                                                            ),
                                                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                                                    context,
                                                                                                                                                                    "y"
                                                                                                                                                            ),
                                                                                                                                                            IntegerArgumentType.getInteger(
                                                                                                                                                                    context,
                                                                                                                                                                    "z"
                                                                                                                                                            )
                                                                                                                                                    );

                                                                                                                                            WifiEngineeringResolution resolution =
                                                                                                                                                    WifiEngineeringTargetResolver.resolve(
                                                                                                                                                            context.getSource()
                                                                                                                                                                    .getLevel(),
                                                                                                                                                            requested
                                                                                                                                                    );

                                                                                                                                            if (!resolution.resolved()) {
                                                                                                                                                context.getSource()
                                                                                                                                                        .sendFailure(
                                                                                                                                                                Component.literal(
                                                                                                                                                                        resolution.failureDetail()
                                                                                                                                                                )
                                                                                                                                                        );

                                                                                                                                                return 0;
                                                                                                                                            }

                                                                                                                                            NetworkDeviceBlockEntity device =
                                                                                                                                                    resolution.target()
                                                                                                                                                            .device();

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

                                                                                                                                            boolean df =
                                                                                                                                                    BoolArgumentType.getBool(
                                                                                                                                                            context,
                                                                                                                                                            "df"
                                                                                                                                                    );

                                                                                                                                            if (!device.sendWifiRawUdpFragmentProbe(
                                                                                                                                                    target,
                                                                                                                                                    bytes,
                                                                                                                                                    df
                                                                                                                                            )) {
                                                                                                                                                context.getSource()
                                                                                                                                                        .sendFailure(
                                                                                                                                                                Component.literal(
                                                                                                                                                                        "Raw UDP fragment probe could not start."
                                                                                                                                                                )
                                                                                                                                                        );

                                                                                                                                                return 0;
                                                                                                                                            }

                                                                                                                                            context.getSource()
                                                                                                                                                    .sendSuccess(
                                                                                                                                                            () ->
                                                                                                                                                                    Component.literal(
                                                                                                                                                                            "Raw IPv4/UDP probe started target="
                                                                                                                                                                                    + target
                                                                                                                                                                                    + " payload="
                                                                                                                                                                                    + bytes
                                                                                                                                                                                    + " DF="
                                                                                                                                                                                    + df
                                                                                                                                                                    ),
                                                                                                                                                            false
                                                                                                                                                    );

                                                                                                                                            return 1;
                                                                                                                                        }
                                                                                                                                )
                                                                                                                )
                                                                                                )
                                                                                )
                                                                )
                                                )
                                )
                );
    }
}
