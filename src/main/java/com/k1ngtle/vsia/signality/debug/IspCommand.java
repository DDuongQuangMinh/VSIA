package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsAnswer;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryResult;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.OwnedInternetProvider;
import com.k1ngtle.vsia.signality.internet.provider.RegisteredDomain;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class IspCommand {
    private IspCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("isp")
                        .then(
                                Commands.literal("my")
                                        .executes(context -> my(context.getSource()))
                        )
                        .then(
                                Commands.literal("provider")
                                        .then(
                                                Commands.literal("register")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> providerRegister(
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
                                                Commands.literal("list")
                                                        .executes(context -> providerList(context.getSource()))
                                        )
                                        .then(
                                                Commands.literal("info")
                                                        .then(
                                                                Commands.argument(
                                                                                "id",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> providerInfo(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "id"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("registrar")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> providerRegistrar(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "args"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("domain")
                                        .then(
                                                Commands.literal("register")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> domainRegister(
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
                                                Commands.literal("renew")
                                                        .then(
                                                                Commands.argument(
                                                                                "domain",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> domainRenew(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "domain"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("ns")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> domainNs(
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
                                                Commands.literal("registrar")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> domainRegistrar(
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
                                                Commands.literal("authinfo")
                                                        .then(
                                                                Commands.argument(
                                                                                "domain",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> domainAuthInfo(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "domain"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                                        .then(
                                                Commands.literal("claim-transfer")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> domainClaimTransfer(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "args"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("dns")
                                        .then(
                                                Commands.literal("add")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> dnsAdd(
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
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument(
                                                                                "args",
                                                                                StringArgumentType.greedyString()
                                                                        )
                                                                        .executes(
                                                                                context -> dnsRemove(
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
                                                Commands.literal("list")
                                                        .then(
                                                                Commands.argument(
                                                                                "zone",
                                                                                StringArgumentType.word()
                                                                        )
                                                                        .executes(
                                                                                context -> dnsList(
                                                                                        context.getSource(),
                                                                                        StringArgumentType.getString(
                                                                                                context,
                                                                                                "zone"
                                                                                        )
                                                                                )
                                                                        )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("rdap")
                                        .then(
                                                Commands.argument(
                                                                "domain",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context -> rdap(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "domain"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
                        .then(
                                Commands.literal("whois")
                                        .then(
                                                Commands.argument(
                                                                "domain",
                                                                StringArgumentType.word()
                                                        )
                                                        .executes(
                                                                context -> rdap(
                                                                        context.getSource(),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "domain"
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
                                Commands.literal("help")
                                        .executes(context -> help(context.getSource()))
                        )
        );
    }

    private static int providerRegister(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = split(raw, 2);

        if (parts.length != 2) {
            return usage(source, "/isp provider register <provider-id> <display name>");
        }

        return result(
                source,
                data(source).registerProvider(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        parts[0],
                        parts[1],
                        System.currentTimeMillis()
                )
        );
    }

    private static int providerList(CommandSourceStack source) {
        for (OwnedInternetProvider provider : data(source).providers()) {
            line(
                    source,
                    provider.id()
                            + " | "
                            + provider.displayName()
                            + " | AS"
                            + provider.autonomousSystemNumber()
                            + " | owner="
                            + provider.ownerName()
                            + " | registrar="
                            + provider.registrarEnabled()
            );
        }

        return 1;
    }

    private static int providerInfo(CommandSourceStack source, String id) {
        OwnedInternetProvider provider = data(source).provider(id).orElse(null);

        if (provider == null) {
            source.sendFailure(Component.literal("Provider not found."));
            return 0;
        }

        line(source, "Provider: " + provider.displayName());
        line(source, "ID: " + provider.id());
        line(source, "ASN: AS" + provider.autonomousSystemNumber());
        line(source, "Owner: " + provider.ownerName());
        line(source, "Registrar service: " + provider.registrarEnabled());
        line(source, "Active: " + provider.active());

        return 1;
    }

    private static int providerRegistrar(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length != 2
                || !List.of("on", "off").contains(parts[1].toLowerCase(Locale.ROOT))) {
            return usage(source, "/isp provider registrar <provider-id> <on|off>");
        }

        return result(
                source,
                data(source).setRegistrarEnabled(
                        player.getUUID(),
                        parts[0],
                        "on".equalsIgnoreCase(parts[1])
                )
        );
    }

    private static int domainRegister(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length < 1 || parts.length > 2) {
            return usage(
                    source,
                    "/isp domain register <domain> [registrar-provider-id]"
            );
        }

        return result(
                source,
                data(source).registerDomain(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        parts[0],
                        parts.length == 2 ? parts[1] : "",
                        System.currentTimeMillis()
                )
        );
    }

    private static int domainRenew(
            CommandSourceStack source,
            String domain
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        return result(
                source,
                data(source).renewDomain(
                        player.getUUID(),
                        domain,
                        System.currentTimeMillis()
                )
        );
    }

    private static int domainNs(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length < 3) {
            return usage(
                    source,
                    "/isp domain ns <domain> <ns1> <ns2> [ns3...]"
            );
        }

        return result(
                source,
                data(source).setNameServers(
                        player.getUUID(),
                        parts[0],
                        Arrays.asList(
                                Arrays.copyOfRange(parts, 1, parts.length)
                        )
                )
        );
    }

    private static int domainRegistrar(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length != 2) {
            return usage(
                    source,
                    "/isp domain registrar <domain> <provider-id>"
            );
        }

        return result(
                source,
                data(source).changeRegistrar(
                        player.getUUID(),
                        parts[0],
                        parts[1]
                )
        );
    }

    private static int domainAuthInfo(
            CommandSourceStack source,
            String domain
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        return result(
                source,
                data(source).rotateAuthInfo(
                        player.getUUID(),
                        domain
                )
        );
    }

    private static int domainClaimTransfer(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length != 2) {
            return usage(
                    source,
                    "/isp domain claim-transfer <domain> <AUTHINFO>"
            );
        }

        return result(
                source,
                data(source).claimRegistrantTransfer(
                        player.getUUID(),
                        player.getGameProfile().getName(),
                        parts[0],
                        parts[1],
                        System.currentTimeMillis()
                )
        );
    }

    private static int dnsAdd(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = split(raw, 5);

        if (parts.length != 5) {
            return usage(
                    source,
                    "/isp dns add <zone> <name|@> <type> <ttl> <value>"
            );
        }

        int ttl;

        try {
            ttl = Integer.parseInt(parts[3]);
        } catch (NumberFormatException exception) {
            return usage(source, "TTL must be an integer.");
        }

        return result(
                source,
                data(source).addDnsRecord(
                        player.getUUID(),
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[4],
                        ttl
                )
        );
    }

    private static int dnsRemove(
            CommandSourceStack source,
            String raw
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String[] parts = raw.trim().split("\\s+");

        if (parts.length != 3) {
            return usage(
                    source,
                    "/isp dns remove <zone> <name|@> <type>"
            );
        }

        return result(
                source,
                data(source).removeDnsRecord(
                        player.getUUID(),
                        parts[0],
                        parts[1],
                        parts[2]
                )
        );
    }

    private static int dnsList(CommandSourceStack source, String zone) {
        RegisteredDomain domain = data(source).domain(zone).orElse(null);

        if (domain == null) {
            source.sendFailure(Component.literal("Registered zone not found."));
            return 0;
        }

        line(
                source,
                "Zone " + domain.domain()
                        + " | NS=" + String.join(",", domain.nameServers())
        );

        line(
                source,
                "@ 300 IN SOA "
                        + (domain.nameServers().isEmpty()
                        ? "ns1.vsia-net.com"
                        : domain.nameServers().get(0))
                        + " hostmaster." + domain.domain() + " ..."
        );

        for (InternetDnsRecord record : data(source).records(zone)) {
            line(
                    source,
                    record.name()
                            + " "
                            + record.ttlSeconds()
                            + " IN "
                            + record.type()
                            + " "
                            + record.answerValue()
            );
        }

        return 1;
    }

    private static int rdap(CommandSourceStack source, String domain) {
        String output = data(source).rdap(domain, System.currentTimeMillis());

        for (String value : output.split("\\n")) {
            line(source, value);
        }

        return 1;
    }

    private static int resolve(CommandSourceStack source, String raw) {
        String[] parts = raw.trim().split("\\s+");

        if (parts.length < 1 || parts.length > 2) {
            return usage(source, "/isp resolve <name> [type]");
        }

        String type = parts.length == 2 ? parts[1] : "A";

        InternetDnsAnswer answer = data(source)
                .resolveFirst(parts[0], type, System.currentTimeMillis())
                .orElse(null);

        if (answer == null) {
            source.sendFailure(Component.literal("NXDOMAIN/NODATA"));
            line(
                    source,
                    data(source).delegationTrace(
                            parts[0],
                            type,
                            System.currentTimeMillis()
                    )
            );
            return 0;
        }

        line(
                source,
                answer.queryName()
                        + " "
                        + answer.ttlSeconds()
                        + " IN "
                        + answer.type()
                        + " "
                        + answer.value()
        );

        line(source, answer.trace());

        return 1;
    }

    private static int my(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        line(source, "Providers owned:");

        for (OwnedInternetProvider provider :
                data(source).providersOwnedBy(player.getUUID())) {
            line(
                    source,
                    "  " + provider.id()
                            + " | "
                            + provider.displayName()
                            + " | AS"
                            + provider.autonomousSystemNumber()
            );
        }

        line(source, "Domains owned:");

        for (RegisteredDomain domain :
                data(source).domainsOwnedBy(player.getUUID())) {
            line(
                    source,
                    "  " + domain.domain()
                            + " | registrar="
                            + domain.registrarProviderId()
            );
        }

        return 1;
    }

    private static int help(CommandSourceStack source) {
        line(source, "/isp provider register <id> <display name>");
        line(source, "/isp provider list | info <id> | registrar <id> <on|off>");
        line(source, "/isp domain register <domain> [registrar-id]");
        line(source, "/isp domain renew <domain> | ns <domain> <ns1> <ns2>...");
        line(source, "/isp domain registrar <domain> <provider-id>");
        line(source, "/isp domain authinfo <domain> | claim-transfer <domain> <AUTHINFO>");
        line(source, "/isp dns add <zone> <name|@> <type> <ttl> <value>");
        line(source, "/isp dns remove <zone> <name|@> <type> | list <zone>");
        line(source, "/isp rdap <domain> | /isp whois <domain>");
        line(source, "/isp resolve <name> [type] | /isp my");
        return 1;
    }

    private static InternetRegistrySavedData data(CommandSourceStack source) {
        return InternetRegistrySavedData.get(source.getLevel());
    }

    private static int result(
            CommandSourceStack source,
            InternetRegistryResult result
    ) {
        if (result.success()) {
            source.sendSuccess(
                    () -> Component.literal(
                            result.message()
                    ).withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }

        source.sendFailure(Component.literal(result.message()));
        return 0;
    }

    private static int usage(CommandSourceStack source, String value) {
        source.sendFailure(Component.literal(value));
        return 0;
    }

    private static void line(CommandSourceStack source, String value) {
        source.sendSuccess(
                () -> Component.literal(value).withStyle(ChatFormatting.GRAY),
                false
        );
    }

    private static String[] split(String raw, int limit) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }

        return raw.trim().split("\\s+", limit);
    }
}
