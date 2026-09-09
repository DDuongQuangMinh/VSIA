package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsClosureTestManager;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class Isp1PhysicalDnsTestCommand {
    private Isp1PhysicalDnsTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("isp1dnstest")
                        .then(
                                Commands.literal("full")
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> full(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "args"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("status")
                                        .executes(context -> status(context.getSource()))
                        )
                        .then(
                                Commands.literal("reset")
                                        .executes(context -> reset(context.getSource()))
                        )
        );
    }

    private static int full(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        String[] parts =
                raw == null || raw.isBlank()
                        ? new String[0]
                        : raw.trim().split("\\s+");

        if (parts.length != 17) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /isp1dnstest full "
                                    + "<Resolver xyz> <Root xyz> <TLD xyz> "
                                    + "<Primary xyz> <Secondary xyz> "
                                    + "<zone> <hostname>"
                    )
            );
            return 0;
        }

        List<ServerRackBlockEntity> racks =
                new ArrayList<>();

        for (int offset = 0; offset < 15; offset += 3) {
            ServerRackBlockEntity rack =
                    rack(
                            source,
                            parts,
                            offset
                    );

            if (rack == null) {
                return 0;
            }

            racks.add(rack);
        }

        String result =
                PhysicalDnsClosureTestManager.start(
                        source.getLevel(),
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        racks.get(0),
                        racks.get(1),
                        racks.get(2),
                        racks.get(3),
                        racks.get(4),
                        parts[15],
                        parts[16]
                );

        if (result.startsWith("FAIL")
                || result.startsWith("A physical")) {
            source.sendFailure(
                    Component.literal(result)
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(result)
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static int status(CommandSourceStack source) {
        PhysicalDnsClosureTestManager.Snapshot snapshot =
                PhysicalDnsClosureTestManager.snapshot();

        if (snapshot == null) {
            source.sendFailure(
                    Component.literal(
                            "No physical DNS closure test active."
                    )
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "[ISP1 PHYSICAL DNS] "
                                + (snapshot.finished()
                                ? snapshot.passed()
                                ? "COMPLETE | PASS"
                                : "FAILED | FAIL"
                                : snapshot.stage() + " | RUNNING")
                ).withStyle(
                        snapshot.finished()
                                ? snapshot.passed()
                                ? ChatFormatting.GREEN
                                : ChatFormatting.RED
                                : ChatFormatting.YELLOW
                ),
                false
        );

        line(source, "Detail: " + snapshot.detail());
        line(
                source,
                "Resolver="
                        + snapshot.resolverIp()
                        + " Primary="
                        + snapshot.primaryIp()
                        + " Secondary="
                        + snapshot.secondaryIp()
        );
        line(
                source,
                "Zone="
                        + snapshot.zone()
                        + " Host="
                        + snapshot.hostname()
        );
        line(
                source,
                "Transfer: "
                        + snapshot.transferStatus()
        );
        line(
                source,
                "Resolution: "
                        + snapshot.resolutionStatus()
        );
        line(
                source,
                "Elapsed="
                        + snapshot.elapsedTicks()
                        + " ticks"
        );

        return snapshot.finished()
                ? snapshot.passed() ? 1 : 0
                : 1;
    }

    private static int reset(CommandSourceStack source) {
        PhysicalDnsClosureTestManager.reset();

        source.sendSuccess(
                () -> Component.literal(
                        "ISP1 physical DNS closure test state reset."
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        return 1;
    }

    private static ServerRackBlockEntity rack(
            CommandSourceStack source,
            String[] parts,
            int offset
    ) {
        try {
            BlockPos pos =
                    new BlockPos(
                            Integer.parseInt(parts[offset]),
                            Integer.parseInt(parts[offset + 1]),
                            Integer.parseInt(parts[offset + 2])
                    );

            BlockEntity entity =
                    source.getLevel()
                            .getBlockEntity(pos);

            if (!(entity instanceof ServerRackBlockEntity rack)) {
                source.sendFailure(
                        Component.literal(
                                "Expected ServerRack at "
                                        + pos.toShortString()
                        )
                );
                return null;
            }

            return rack;
        } catch (NumberFormatException exception) {
            source.sendFailure(
                    Component.literal(
                            "Coordinates must be integers."
                    )
            );
            return null;
        }
    }

    private static void line(
            CommandSourceStack source,
            String value
    ) {
        source.sendSuccess(
                () -> Component.literal(value)
                        .withStyle(ChatFormatting.GRAY),
                false
        );
    }
}
