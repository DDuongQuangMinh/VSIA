package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w127.W127FaultAction;
import com.k1ngtle.vsia.signality.engineering.wifi.integration.w127.W127FaultController;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW127FaultCommand {
    private WifiW127FaultCommand() {
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
                                "wifiw127fault"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(
                                                2
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "clear"
                                        )
                                        .executes(
                                                context ->
                                                        clear(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "status"
                                        )
                                        .executes(
                                                context ->
                                                        status(
                                                                context.getSource()
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal(
                                                "arm"
                                        )
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        arm(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "args"
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int clear(
            CommandSourceStack source
    ) {
        W127FaultController.clearAll();

        source.sendSuccess(
                () ->
                        Component.literal(
                                        "W1.27 fault rules and delayed packets cleared."
                                )
                                .withStyle(
                                        ChatFormatting.GREEN
                                ),
                false
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source
    ) {
        var snapshot =
                W127FaultController.snapshot();

        line(
                source,
                snapshot.compact()
        );

        for (String rule :
                W127FaultController.rules()) {
            line(
                    source,
                    rule
            );
        }

        return 1;
    }

    private static int arm(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts =
                raw == null
                        || raw.isBlank()
                        ? new String[0]
                        : raw.trim()
                        .split(
                                "\\s+"
                        );

        if (parts.length != 11) {
            source.sendFailure(
                    Component.literal(
                            "Usage: /wifiw127fault arm "
                                    + "<action> <src|*> <dst|*> <app|*> <pdnsKind|*> <xfrKind|*> "
                                    + "<sequence|-1> <request|response|any> <matches|-1> <delayTicks> <forcedSerial>"
                    )
            );
            return 0;
        }

        try {
            W127FaultAction action =
                    W127FaultAction.valueOf(
                            parts[0].toUpperCase(
                                    Locale.ROOT
                            )
                    );

            Integer sequence =
                    Integer.parseInt(
                            parts[6]
                    ) < 0
                            ? null
                            : Integer.parseInt(
                            parts[6]
                    );

            Boolean response =
                    switch (
                            parts[7].toLowerCase(
                                    Locale.ROOT
                            )
                    ) {
                        case "request" -> false;
                        case "response" -> true;
                        case "any" -> null;
                        default -> throw new IllegalArgumentException(
                                "response selector must be request, response, or any"
                        );
                    };

            int matches =
                    Integer.parseInt(
                            parts[8]
                    );

            long delay =
                    Long.parseLong(
                            parts[9]
                    );

            long forcedSerial =
                    Long.parseLong(
                            parts[10]
                    );

            var rule =
                    W127FaultController.arm(
                            action,
                            parts[1],
                            parts[2],
                            parts[3],
                            parts[4],
                            parts[5],
                            sequence,
                            response,
                            matches,
                            delay,
                            forcedSerial
                    );

            source.sendSuccess(
                    () ->
                            Component.literal(
                                            "W1.27 fault armed: "
                                                    + rule.compact()
                                    )
                                    .withStyle(
                                            ChatFormatting.GREEN
                                    ),
                    false
            );

            return 1;
        } catch (Exception exception) {
            source.sendFailure(
                    Component.literal(
                            "Could not arm W1.27 fault: "
                                    + (
                                    exception.getMessage() == null
                                            ? exception.getClass().getSimpleName()
                                            : exception.getMessage()
                            )
                    )
            );

            return 0;
        }
    }

    private static void line(
            CommandSourceStack source,
            String value
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                        value
                                )
                                .withStyle(
                                        ChatFormatting.GRAY
                                ),
                false
        );
    }
}
