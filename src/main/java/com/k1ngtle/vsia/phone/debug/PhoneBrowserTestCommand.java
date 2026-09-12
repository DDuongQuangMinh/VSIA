package com.k1ngtle.vsia.phone.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryResult;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryValidators;
import com.k1ngtle.vsia.signality.internet.web.W128WebBuildResult;
import com.k1ngtle.vsia.signality.internet.web.W128WebMode;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PhoneBrowserTestCommand {
    private static final String INDEX_HTML =
            "<!doctype html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                    + "<title>VSIA Phone Test</title>"
                    + "<link rel=\"stylesheet\" href=\"/styles.css\">"
                    + "</head>"
                    + "<body>"
                    + "<header class=\"hero\">"
                    + "<h1>VSIA Phone Web Test</h1>"
                    + "<p>This page is served by the real W1.28 website backend.</p>"
                    + "</header>"
                    + "<section class=\"card\">"
                    + "<h2>Server Rack</h2>"
                    + "<p class=\"online\">ONLINE</p>"
                    + "<p>If you can read this on the Temporary iPhone, HTTP, virtual hosting, page transfer and HTML rendering are working.</p>"
                    + "<a href=\"/about.html\">Open About Page</a>"
                    + "</section>"
                    + "<section class=\"card\">"
                    + "<h2>Cross-player test</h2>"
                    + "<p>Give this domain to another player and let them open it from their phone.</p>"
                    + "</section>"
                    + "</body>"
                    + "</html>";

    private static final String ABOUT_HTML =
            "<!doctype html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset=\"utf-8\">"
                    + "<title>About</title>"
                    + "<link rel=\"stylesheet\" href=\"/styles.css\">"
                    + "</head>"
                    + "<body>"
                    + "<section class=\"card\">"
                    + "<h1>About This Test</h1>"
                    + "<p>This is a second file from the same published W1.28 project.</p>"
                    + "<a href=\"/\">Back Home</a>"
                    + "</section>"
                    + "</body>"
                    + "</html>";

    private static final String STYLES_CSS =
            "body{background:#f2f2f7;color:#1c1c1e;padding:10px;}"
                    + ".hero{background:#0a84ff;color:white;padding:12px;border-radius:12px;margin-bottom:10px;text-align:center;}"
                    + ".card{background:white;padding:10px;border:1px #d1d1d6;border-radius:10px;margin-bottom:10px;}"
                    + ".online{color:green;font-weight:bold;}"
                    + "a{color:#007aff;font-weight:bold;}";

    private PhoneBrowserTestCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher()
                .register(
                        Commands.literal(
                                        "vsia-phone-webtest"
                                )
                                .then(
                                        Commands.argument(
                                                        "rackIp",
                                                        StringArgumentType.word()
                                                )
                                                .executes(
                                                        context ->
                                                                createTestSite(
                                                                        context
                                                                                .getSource()
                                                                                .getPlayerOrException(),
                                                                        StringArgumentType
                                                                                .getString(
                                                                                        context,
                                                                                        "rackIp"
                                                                                )
                                                                )
                                                )
                                )
                );
    }

    private static int createTestSite(
            ServerPlayer player,
            String rackIp
    ) {
        if (!InternetRegistryValidators
                .validIpv4(
                        rackIp
                )) {
            player.sendSystemMessage(
                    Component.literal(
                                    "Invalid IPv4 address: "
                                            + rackIp
                            )
                            .withStyle(
                                    ChatFormatting.RED
                            )
            );

            return 0;
        }

        ServerLevel level =
                player.serverLevel();

        long now =
                System.currentTimeMillis();

        UUID owner =
                player.getUUID();

        String compactUuid =
                owner.toString()
                        .replace(
                                "-",
                                ""
                        );

        String domain =
                "phone-"
                        + compactUuid.substring(
                        0,
                        8
                )
                        + ".com";

        InternetRegistrySavedData internet =
                InternetRegistrySavedData
                        .get(level);

        if (internet
                .domain(domain)
                .isEmpty()) {
            InternetRegistryResult registered =
                    internet.registerDomain(
                            owner,
                            player.getGameProfile()
                                    .getName(),
                            domain,
                            InternetRegistrySavedData
                                    .SYSTEM_PROVIDER,
                            now
                    );

            if (!registered.success()) {
                return fail(
                        player,
                        registered.message()
                );
            }
        } else if (!internet
                .domain(domain)
                .orElseThrow()
                .ownedBy(owner)) {
            return fail(
                    player,
                    "The generated test domain is owned by another player."
            );
        }

        internet.removeDnsRecord(
                owner,
                domain,
                "@",
                "A"
        );

        InternetRegistryResult dns =
                internet.addDnsRecord(
                        owner,
                        domain,
                        "@",
                        "A",
                        rackIp,
                        300
                );

        if (!dns.success()) {
            return fail(
                    player,
                    dns.message()
            );
        }

        W128WebRegistrySavedData web =
                W128WebRegistrySavedData
                        .get(level);

        if (web.project(domain)
                .isEmpty()) {
            W128WebBuildResult created =
                    web.create(
                            level,
                            owner,
                            player.getGameProfile()
                                    .getName(),
                            domain,
                            W128WebMode.STATIC,
                            now
                    );

            if (!created.success()) {
                return fail(
                        player,
                        created.message()
                );
            }
        } else if (!web.project(domain)
                .orElseThrow()
                .ownerUuid()
                .equals(owner)) {
            return fail(
                    player,
                    "The generated web project is owned by another player."
            );
        }

        W128WebBuildResult index =
                web.putFile(
                        owner,
                        domain,
                        "/index.html",
                        INDEX_HTML,
                        now
                );

        if (!index.success()) {
            return fail(
                    player,
                    index.message()
            );
        }

        W128WebBuildResult about =
                web.putFile(
                        owner,
                        domain,
                        "/about.html",
                        ABOUT_HTML,
                        now
                );

        if (!about.success()) {
            return fail(
                    player,
                    about.message()
            );
        }

        W128WebBuildResult style =
                web.putFile(
                        owner,
                        domain,
                        "/styles.css",
                        STYLES_CSS,
                        now
                );

        if (!style.success()) {
            return fail(
                    player,
                    style.message()
            );
        }

        W128WebBuildResult bound =
                web.bind(
                        owner,
                        domain,
                        rackIp,
                        now
                );

        if (!bound.success()) {
            return fail(
                    player,
                    bound.message()
            );
        }

        W128WebBuildResult built =
                web.build(
                        owner,
                        domain,
                        now
                );

        if (!built.success()) {
            return fail(
                    player,
                    built.message()
            );
        }

        W128WebBuildResult published =
                web.publish(
                        level,
                        owner,
                        domain,
                        now
                );

        if (!published.success()) {
            return fail(
                    player,
                    published.message()
            );
        }

        player.sendSystemMessage(
                Component.literal(
                                "VS:IA phone website test READY"
                        )
                        .withStyle(
                                ChatFormatting.GREEN
                        )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Domain: "
                                + domain
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "DNS A: "
                                + domain
                                + " -> "
                                + rackIp
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Phone URL: "
                                + domain
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Direct rack URL: "
                                + domain
                                + "@"
                                + rackIp
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Other players can open the same domain on their phone."
                )
        );

        return 1;
    }

    private static int fail(
            ServerPlayer player,
            String message
    ) {
        player.sendSystemMessage(
                Component.literal(
                                "Phone web test failed: "
                                        + message
                        )
                        .withStyle(
                                ChatFormatting.RED
                        )
        );

        return 0;
    }
}
