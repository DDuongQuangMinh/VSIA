package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.dns.physical.DnsAuthorityDelegation;
import com.k1ngtle.vsia.signality.internet.dns.physical.DnsZoneSnapshot;
import com.k1ngtle.vsia.signality.internet.dns.physical.DnssecEngine;
import com.k1ngtle.vsia.signality.internet.dns.physical.DnssecZoneKey;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsBootstrap;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsRackConfig;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsRole;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsService;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsStateSavedData;
import com.k1ngtle.vsia.signality.internet.dns.physical.PhysicalDnsUnitTestSuite;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class DnsInfraCommand {
    private DnsInfraCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dnsinfra")
                        .then(
                                Commands.literal("unit")
                                        .executes(context -> unit(context.getSource()))
                        )
                        .then(
                                Commands.literal("bootstrap")
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> bootstrap(
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
                                Commands.literal("sync")
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> sync(
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
                                Commands.literal("resolve")
                                        .then(
                                                Commands.argument(
                                                                "args",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> resolve(
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
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> status(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "coords"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("trace")
                                        .then(
                                                Commands.argument(
                                                                "coords",
                                                                StringArgumentType.greedyString()
                                                        )
                                                        .executes(
                                                                context -> trace(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "coords"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("registry")
                                        .executes(context -> registry(context.getSource()))
                        )
                        .then(
                                Commands.literal("help")
                                        .executes(context -> help(context.getSource()))
                        )
        );
    }

    private static int unit(CommandSourceStack source) {
        List<PhysicalDnsUnitTestSuite.Result> results =
                PhysicalDnsUnitTestSuite.runAll();

        int passed = 0;

        for (PhysicalDnsUnitTestSuite.Result result : results) {
            if (result.passed()) {
                passed++;
            }

            source.sendSuccess(
                    () -> Component.literal(
                            "[" + (result.passed() ? "PASS" : "FAIL") + "] " + result.name()
                    ).withStyle(
                            result.passed()
                                    ? ChatFormatting.GREEN
                                    : ChatFormatting.RED
                    ),
                    false
            );

            line(source, "  " + result.detail());
        }

        int failed = results.size() - passed;
        int finalPassed = passed;
        int finalFailed = failed;

        source.sendSuccess(
                () -> Component.literal(
                        "ISP1 physical DNS unit result: "
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

        return failed == 0 ? 1 : 0;
    }

    private static int bootstrap(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts = split(raw);

        if (parts.length != 16) {
            return usage(
                    source,
                    "/dnsinfra bootstrap "
                            + "<Resolver x y z> <Root x y z> <TLD x y z> "
                            + "<Primary x y z> <Secondary x y z> <zone>"
            );
        }

        ParseResult parsed = parseFiveRacks(source, parts);

        if (!parsed.success) {
            return 0;
        }

        String zone = parts[15];

        PhysicalDnsBootstrap.Result result =
                PhysicalDnsBootstrap.configure(
                        source.getLevel(),
                        parsed.racks.get(0),
                        parsed.racks.get(1),
                        parsed.racks.get(2),
                        parsed.racks.get(3),
                        parsed.racks.get(4),
                        zone
                );

        if (!result.success()) {
            source.sendFailure(Component.literal(result.detail()));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Physical DNS hierarchy configured."
                ).withStyle(ChatFormatting.GREEN),
                false
        );

        line(source, result.detail());
        line(source, "Next: /dnsinfra sync <Secondary x y z> AXFR");

        return 1;
    }

    private static int sync(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts = split(raw);

        if (parts.length != 4) {
            return usage(
                    source,
                    "/dnsinfra sync <Secondary x y z> <AXFR|IXFR>"
            );
        }

        ServerRackBlockEntity rack =
                rack(
                        source,
                        parts,
                        0
                );

        if (rack == null) {
            return 0;
        }

        String result =
                PhysicalDnsService.startTransfer(
                        rack,
                        parts[3]
                );

        source.sendSuccess(
                () -> Component.literal(result)
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        return result.startsWith("Selected")
                || result.startsWith("Secondary")
                ? 0
                : 1;
    }

    private static int resolve(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts = split(raw);

        if (parts.length != 5) {
            return usage(
                    source,
                    "/dnsinfra resolve <Resolver x y z> <name> <type>"
            );
        }

        ServerRackBlockEntity resolver =
                rack(
                        source,
                        parts,
                        0
                );

        if (resolver == null) {
            return 0;
        }

        String result =
                PhysicalDnsService.startDiagnostic(
                        resolver,
                        parts[3],
                        parts[4]
                );

        source.sendSuccess(
                () -> Component.literal(result)
                        .withStyle(ChatFormatting.GREEN),
                false
        );

        return result.startsWith("Physical")
                ? 1
                : 0;
    }

    private static int status(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts = split(raw);

        if (parts.length != 3) {
            return usage(
                    source,
                    "/dnsinfra status <ServerRack x y z>"
            );
        }

        ServerRackBlockEntity rack =
                rack(
                        source,
                        parts,
                        0
                );

        if (rack == null) {
            return 0;
        }

        PhysicalDnsRackConfig config =
                PhysicalDnsRackConfig.read(rack);

        line(source, "Rack: " + rack.displayName() + " | " + rack.ipAddress());
        line(source, "Role: " + config.role());
        line(source, "Zone: " + config.zone());
        line(source, "Root hint: " + config.rootHintIp());
        line(source, "Master: " + config.masterIp());
        line(source, "DNSSEC validation: " + config.dnssecValidation());

        if (config.role() == PhysicalDnsRole.RECURSIVE) {
            line(
                    source,
                    "Resolution: " + PhysicalDnsService.resolutionStatus(rack.ipAddress())
            );
        }

        if (config.role() == PhysicalDnsRole.AUTHORITATIVE_SECONDARY) {
            line(
                    source,
                    "Transfer: " + PhysicalDnsService.transferStatus(rack.ipAddress())
            );

            PhysicalDnsStateSavedData.get(source.getLevel())
                    .replica(
                            rack.ipAddress(),
                            config.zone()
                    )
                    .ifPresent(snapshot -> {
                        line(
                                source,
                                "Replica serial="
                                        + Long.toUnsignedString(snapshot.serial())
                                        + " records="
                                        + snapshot.records().size()
                        );
                    });
        }

        return 1;
    }

    private static int trace(
            CommandSourceStack source,
            String raw
    ) {
        String[] parts = split(raw);

        if (parts.length != 3) {
            return usage(
                    source,
                    "/dnsinfra trace <Resolver x y z>"
            );
        }

        ServerRackBlockEntity rack =
                rack(
                        source,
                        parts,
                        0
                );

        if (rack == null) {
            return 0;
        }

        String trace =
                PhysicalDnsService.resolutionTrace(
                        rack.ipAddress()
                );

        for (String value : trace.split("\\n")) {
            line(source, value);
        }

        return 1;
    }

    private static int registry(CommandSourceStack source) {
        PhysicalDnsStateSavedData state =
                PhysicalDnsStateSavedData.get(
                        source.getLevel()
                );

        line(source, "Physical TLD servers:");

        for (Map.Entry<String, String> entry :
                state.tldServers().entrySet()) {
            line(
                    source,
                    "  ." + entry.getKey() + " -> " + entry.getValue()
            );
        }

        line(source, "Authoritative delegations:");

        for (DnsAuthorityDelegation delegation :
                state.delegations()) {
            line(
                    source,
                    "  "
                            + delegation.zone()
                            + " | ."
                            + delegation.tld()
                            + " | primary="
                            + delegation.primaryIp()
                            + " | secondary="
                            + delegation.secondaryIp()
            );
        }

        return 1;
    }

    private static int help(CommandSourceStack source) {
        line(source, "/dnsinfra unit");
        line(
                source,
                "/dnsinfra bootstrap "
                        + "<Resolver xyz> <Root xyz> <TLD xyz> "
                        + "<Primary xyz> <Secondary xyz> <zone>"
        );
        line(source, "/dnsinfra sync <Secondary xyz> <AXFR|IXFR>");
        line(source, "/dnsinfra resolve <Resolver xyz> <name> <type>");
        line(source, "/dnsinfra status <ServerRack xyz>");
        line(source, "/dnsinfra trace <Resolver xyz>");
        line(source, "/dnsinfra registry");
        return 1;
    }

    private static ParseResult parseFiveRacks(
            CommandSourceStack source,
            String[] parts
    ) {
        List<ServerRackBlockEntity> racks =
                new ArrayList<>();

        for (int index = 0; index < 15; index += 3) {
            ServerRackBlockEntity rack =
                    rack(
                            source,
                            parts,
                            index
                    );

            if (rack == null) {
                return new ParseResult(
                        false,
                        List.of()
                );
            }

            racks.add(rack);
        }

        return new ParseResult(
                true,
                racks
        );
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

    private static String[] split(String raw) {
        return raw == null || raw.isBlank()
                ? new String[0]
                : raw.trim().split("\\s+");
    }

    private static int usage(
            CommandSourceStack source,
            String value
    ) {
        source.sendFailure(
                Component.literal(value)
        );
        return 0;
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

    private record ParseResult(
            boolean success,
            List<ServerRackBlockEntity> racks
    ) {
    }
}
