package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.web.W128HttpResponse;
import com.k1ngtle.vsia.signality.internet.web.W128WebBuildResult;
import com.k1ngtle.vsia.signality.internet.web.W128WebHostService;
import com.k1ngtle.vsia.signality.internet.web.W128WebProject;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.web.W128WebTestResult;
import com.k1ngtle.vsia.signality.internet.web.W128WebUnitTestSuite;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class WifiW128TestCommand {
    private WifiW128TestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wifiw128test")
                        .requires(source -> source.hasPermission(2))
                        .then(
                                Commands.literal("unit")
                                        .executes(context -> unit(context.getSource()))
                        )
                        .then(
                                Commands.literal("closure")
                                        .then(
                                                Commands.argument("args", StringArgumentType.greedyString())
                                                        .executes(context -> closure(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "args")
                                                        ))
                                        )
                        )
        );
    }

    private static int unit(CommandSourceStack source) {
        List<W128WebTestResult> results = W128WebUnitTestSuite.runAll();
        int passed = 0;
        int failed = 0;

        for (W128WebTestResult result : results) {
            if (result.passed()) {
                passed++;
                source.sendSuccess(
                        () -> Component.literal("[PASS] " + result.id()).withStyle(ChatFormatting.GREEN),
                        false
                );
            } else {
                failed++;
                source.sendFailure(Component.literal("[FAIL] " + result.id() + " | " + result.detail()));
            }
        }

        int finalPassed = passed;
        int finalFailed = failed;
        source.sendSuccess(
                () -> Component.literal(
                        "W1.28 unit result: " + finalPassed + " passed, " + finalFailed + " failed"
                ).withStyle(finalFailed == 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                false
        );
        return failed == 0 ? 1 : 0;
    }

    private static int closure(CommandSourceStack source, String raw) {
        String[] args = raw == null ? new String[0] : raw.trim().split("\\s+");
        if (args.length != 2) {
            source.sendFailure(Component.literal(
                    "Usage: /wifiw128test closure <host> <server-rack-ip>"
            ));
            return 0;
        }

        long now = System.currentTimeMillis();
        W128WebRegistrySavedData web = W128WebRegistrySavedData.get(source.getLevel());
        W128WebProject project = web.project(args[0]).orElse(null);
        if (project == null) {
            source.sendFailure(Component.literal("Closure FAIL: web project not found."));
            return 0;
        }

        W128WebBuildResult authority = web.validateAuthority(source.getLevel(), project, now);
        if (!authority.success()) {
            source.sendFailure(Component.literal("Closure FAIL: " + authority.message()));
            return 0;
        }

        if (!project.published()) {
            source.sendFailure(Component.literal("Closure FAIL: project is not published."));
            return 0;
        }

        if (!args[1].equals(project.boundServerIp())) {
            source.sendFailure(Component.literal(
                    "Closure FAIL: bound IP is " + project.boundServerIp() + ", expected " + args[1]
            ));
            return 0;
        }

        var dns = InternetRegistrySavedData.get(source.getLevel())
                .resolveFirst(project.host(), "A", now);
        if (dns.isEmpty() || !args[1].equals(dns.get().value())) {
            source.sendFailure(Component.literal("Closure FAIL: ISP1 DNS does not resolve the host to the Server Rack."));
            return 0;
        }

        W128HttpResponse response = W128WebHostService.serve(
                source.getLevel(),
                args[1],
                project.host(),
                "/",
                now
        );

        if (response == null || response.status() != 200 || response.body().isBlank()) {
            source.sendFailure(Component.literal(
                    "Closure FAIL: virtual-host HTTP did not return a non-empty HTTP 200 response."
            ));
            return 0;
        }

        byte[] wire = W128WebHostService.responseWire(response);
        if (wire.length == 0) {
            source.sendFailure(Component.literal("Closure FAIL: HTTP wire response is empty."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "PASS: W1.28 web closure | host=" + project.host()
                                + " | DNS=" + dns.get().value()
                                + " | HTTP 200"
                                + " | content-type=" + response.contentType()
                                + " | revision=" + project.buildRevision()
                                + " | wire=" + wire.length + "B"
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }
}
