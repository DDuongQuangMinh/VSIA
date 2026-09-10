package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.internet.provider.InternetDnsRecord;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistryResult;
import com.k1ngtle.vsia.signality.internet.provider.InternetRegistrySavedData;
import com.k1ngtle.vsia.signality.internet.web.W128WebBuildResult;
import com.k1ngtle.vsia.signality.internet.web.W128WebFile;
import com.k1ngtle.vsia.signality.internet.web.W128WebMode;
import com.k1ngtle.vsia.signality.internet.web.W128WebProject;
import com.k1ngtle.vsia.signality.internet.web.W128WebRegistrySavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class W128WebCommand {
    private W128WebCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("web")
                        .executes(context -> help(context.getSource()))
                        .then(
                                Commands.literal("create")
                                        .then(
                                                Commands.argument("args", StringArgumentType.greedyString())
                                                        .executes(context -> create(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "args")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("delete")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> delete(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("my")
                                        .executes(context -> my(context.getSource()))
                        )
                        .then(
                                Commands.literal("info")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> info(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("bind")
                                        .then(
                                                Commands.argument("args", StringArgumentType.greedyString())
                                                        .executes(context -> bind(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "args")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("dns")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> dns(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("build")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> build(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("publish")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> publish(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("unpublish")
                                        .then(
                                                Commands.argument("host", StringArgumentType.word())
                                                        .executes(context -> unpublish(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "host")
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("file")
                                        .then(
                                                Commands.literal("set")
                                                        .then(
                                                                Commands.argument("args", StringArgumentType.greedyString())
                                                                        .executes(context -> fileSet(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "args")
                                                                        ))
                                                        )
                                        )
                                        .then(
                                                Commands.literal("book")
                                                        .then(
                                                                Commands.argument("args", StringArgumentType.greedyString())
                                                                        .executes(context -> fileBook(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "args")
                                                                        ))
                                                        )
                                        )
                                        .then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument("args", StringArgumentType.greedyString())
                                                                        .executes(context -> fileRemove(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "args")
                                                                        ))
                                                        )
                                        )
                                        .then(
                                                Commands.literal("list")
                                                        .then(
                                                                Commands.argument("host", StringArgumentType.word())
                                                                        .executes(context -> fileList(
                                                                                context.getSource(),
                                                                                StringArgumentType.getString(context, "host")
                                                                        ))
                                                        )
                                        )
                        )
        );
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal("VS:IA W1.28 web platform").withStyle(ChatFormatting.AQUA),
                false
        );
        source.sendSuccess(() -> Component.literal("/web create <host> <static|react>"), false);
        source.sendSuccess(() -> Component.literal("/web bind <host> <server-rack-ip> [ttl]"), false);
        source.sendSuccess(() -> Component.literal("/web file set <host> <path> <content>"), false);
        source.sendSuccess(() -> Component.literal("/web file book <host> <path>"), false);
        source.sendSuccess(() -> Component.literal("/web build <host>"), false);
        source.sendSuccess(() -> Component.literal("/web publish <host>"), false);
        source.sendSuccess(() -> Component.literal("/web info <host> | /web my | /web dns <host>"), false);
        return 1;
    }

    private static int create(CommandSourceStack source, String raw) throws CommandSyntaxException {
        String[] args = split(raw, 2);
        if (args.length < 2) {
            return fail(source, "Usage: /web create <host> <static|react>");
        }

        ServerPlayer player = source.getPlayerOrException();
        W128WebMode mode;
        try {
            mode = W128WebMode.parse(args[1]);
        } catch (IllegalArgumentException exception) {
            return fail(source, exception.getMessage());
        }

        return result(
                source,
                data(source).create(
                        source.getLevel(),
                        player.getUUID(),
                        player.getName().getString(),
                        args[0],
                        mode,
                        System.currentTimeMillis()
                )
        );
    }

    private static int delete(CommandSourceStack source, String host) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return result(source, data(source).delete(player.getUUID(), host));
    }

    private static int my(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<W128WebProject> projects = data(source).projectsOwnedBy(player.getUUID());
        if (projects.isEmpty()) {
            source.sendSuccess(() -> Component.literal("You own no W1.28 web projects."), false);
            return 1;
        }

        source.sendSuccess(
                () -> Component.literal("Your W1.28 websites:").withStyle(ChatFormatting.AQUA),
                false
        );
        for (W128WebProject project : projects) {
            source.sendSuccess(
                    () -> Component.literal(
                            project.host()
                                    + " | " + project.mode().name().toLowerCase()
                                    + " | ip=" + (project.boundServerIp().isBlank() ? "unbound" : project.boundServerIp())
                                    + " | " + (project.published() ? "PUBLISHED" : "DRAFT")
                    ),
                    false
            );
        }
        return projects.size();
    }

    private static int info(CommandSourceStack source, String host) {
        W128WebProject project = data(source).project(host).orElse(null);
        if (project == null) {
            return fail(source, "Web project not found.");
        }

        source.sendSuccess(
                () -> Component.literal("WEB " + project.host()).withStyle(ChatFormatting.AQUA),
                false
        );
        source.sendSuccess(() -> Component.literal("Root domain: " + project.rootDomain()), false);
        source.sendSuccess(() -> Component.literal("Owner: " + project.ownerName()), false);
        source.sendSuccess(() -> Component.literal("Mode: " + project.mode()), false);
        source.sendSuccess(() -> Component.literal("Server Rack IPv4: " + (project.boundServerIp().isBlank() ? "UNBOUND" : project.boundServerIp())), false);
        source.sendSuccess(() -> Component.literal("Published: " + project.published()), false);
        source.sendSuccess(() -> Component.literal("Build revision: " + project.buildRevision()), false);
        source.sendSuccess(() -> Component.literal("Files: " + project.files().size() + " | characters=" + project.totalCharacters()), false);
        return 1;
    }

    private static int bind(CommandSourceStack source, String raw) throws CommandSyntaxException {
        String[] args = split(raw, 3);
        if (args.length < 2) {
            return fail(source, "Usage: /web bind <host> <server-rack-ip> [ttl]");
        }

        int ttl = 300;
        if (args.length >= 3) {
            try {
                ttl = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                return fail(source, "TTL must be an integer from 30 to 86400.");
            }
        }
        if (ttl < 30 || ttl > 86400) {
            return fail(source, "TTL must be 30-86400 seconds.");
        }

        ServerPlayer player = source.getPlayerOrException();
        W128WebProject project = data(source).project(args[0]).orElse(null);
        if (project == null) {
            return fail(source, "Web project not found.");
        }
        if (!project.ownerUuid().equals(player.getUUID())) {
            return fail(source, "You do not own this web project.");
        }

        InternetRegistrySavedData registry = InternetRegistrySavedData.get(source.getLevel());
        String owner = relativeOwner(project.host(), project.rootDomain());

        boolean cname = registry.records(project.rootDomain())
                .stream()
                .anyMatch(record -> record.name().equals(project.host()) && "CNAME".equals(record.type()));

        if (cname) {
            return fail(source, "Remove the existing CNAME before binding this host to a Server Rack A record.");
        }

        List<InternetDnsRecord> existingA = registry.records(project.rootDomain())
                .stream()
                .filter(record -> record.name().equals(project.host()) && "A".equals(record.type()))
                .toList();

        boolean exact = existingA.stream().anyMatch(record -> record.value().equals(args[1]));
        if (!exact) {
            if (!existingA.isEmpty()) {
                InternetRegistryResult removed = registry.removeDnsRecord(
                        player.getUUID(),
                        project.rootDomain(),
                        owner,
                        "A"
                );
                if (!removed.success()) {
                    return fail(source, removed.message());
                }
            }

            InternetRegistryResult added = registry.addDnsRecord(
                    player.getUUID(),
                    project.rootDomain(),
                    owner,
                    "A",
                    args[1],
                    ttl
            );
            if (!added.success()) {
                return fail(source, added.message());
            }
        }

        W128WebBuildResult bound = data(source).bind(
                player.getUUID(),
                project.host(),
                args[1],
                System.currentTimeMillis()
        );
        return result(source, bound);
    }

    private static int dns(CommandSourceStack source, String host) {
        var answer = InternetRegistrySavedData.get(source.getLevel())
                .resolveFirst(host, "A", System.currentTimeMillis());
        if (answer.isEmpty()) {
            return fail(source, "DNS A: NXDOMAIN/NODATA for " + host);
        }

        source.sendSuccess(
                () -> Component.literal(
                        "DNS A " + host + " -> " + answer.get().value()
                                + " | TTL " + answer.get().ttlSeconds()
                                + " | zone=" + answer.get().zone()
                ).withStyle(ChatFormatting.GREEN),
                false
        );
        source.sendSuccess(() -> Component.literal(answer.get().trace()).withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int build(CommandSourceStack source, String host) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return result(
                source,
                data(source).build(player.getUUID(), host, System.currentTimeMillis())
        );
    }

    private static int publish(CommandSourceStack source, String host) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return result(
                source,
                data(source).publish(
                        source.getLevel(),
                        player.getUUID(),
                        host,
                        System.currentTimeMillis()
                )
        );
    }

    private static int unpublish(CommandSourceStack source, String host) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return result(
                source,
                data(source).unpublish(player.getUUID(), host, System.currentTimeMillis())
        );
    }

    private static int fileSet(CommandSourceStack source, String raw) throws CommandSyntaxException {
        String[] args = raw == null ? new String[0] : raw.trim().split("\\s+", 3);
        if (args.length < 3) {
            return fail(source, "Usage: /web file set <host> <path> <content>");
        }

        ServerPlayer player = source.getPlayerOrException();
        return result(
                source,
                data(source).putFile(
                        player.getUUID(),
                        args[0],
                        args[1],
                        args[2],
                        System.currentTimeMillis()
                )
        );
    }

    private static int fileBook(CommandSourceStack source, String raw) throws CommandSyntaxException {
        String[] args = split(raw, 2);
        if (args.length < 2) {
            return fail(source, "Usage: /web file book <host> <path>");
        }

        ServerPlayer player = source.getPlayerOrException();
        String content = bookText(player);
        if (content == null) {
            return fail(source, "Hold a Book & Quill or Written Book in either hand.");
        }

        return result(
                source,
                data(source).putFile(
                        player.getUUID(),
                        args[0],
                        args[1],
                        content,
                        System.currentTimeMillis()
                )
        );
    }

    private static int fileRemove(CommandSourceStack source, String raw) throws CommandSyntaxException {
        String[] args = split(raw, 2);
        if (args.length < 2) {
            return fail(source, "Usage: /web file remove <host> <path>");
        }

        ServerPlayer player = source.getPlayerOrException();
        return result(
                source,
                data(source).removeFile(
                        player.getUUID(),
                        args[0],
                        args[1],
                        System.currentTimeMillis()
                )
        );
    }

    private static int fileList(CommandSourceStack source, String host) {
        W128WebProject project = data(source).project(host).orElse(null);
        if (project == null) {
            return fail(source, "Web project not found.");
        }

        source.sendSuccess(
                () -> Component.literal("Files for " + project.host()).withStyle(ChatFormatting.AQUA),
                false
        );
        project.files().values()
                .stream()
                .sorted((a, b) -> a.path().compareTo(b.path()))
                .forEach(file -> source.sendSuccess(
                        () -> Component.literal(
                                file.path()
                                        + " | " + file.contentType()
                                        + " | " + file.content().length() + " chars"
                                        + (file.generated() ? " | generated" : "")
                        ),
                        false
                ));
        return project.files().size();
    }

    private static String bookText(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!isBook(stack)) {
            stack = player.getOffhandItem();
        }
        if (!isBook(stack)) {
            return null;
        }

        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("pages", Tag.TAG_LIST)) {
            return "";
        }

        ListTag pages = tag.getList("pages", Tag.TAG_STRING);
        StringBuilder out = new StringBuilder();
        boolean written = stack.is(Items.WRITTEN_BOOK);

        for (int i = 0; i < pages.size(); i++) {
            String page = pages.getString(i);
            if (written) {
                try {
                    Component component = Component.Serializer.fromJson(page);
                    if (component != null) {
                        page = component.getString();
                    }
                } catch (Exception ignored) {
                }
            }
            if (i > 0) {
                out.append('\n');
            }
            out.append(page);
        }

        return out.toString();
    }

    private static boolean isBook(ItemStack stack) {
        return stack != null && (stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK));
    }

    private static String relativeOwner(String host, String root) {
        if (host.equals(root)) {
            return "@";
        }
        String suffix = "." + root;
        return host.endsWith(suffix)
                ? host.substring(0, host.length() - suffix.length())
                : host;
    }

    private static String[] split(String raw, int limit) {
        if (raw == null || raw.isBlank()) {
            return new String[0];
        }
        return raw.trim().split("\\s+", limit);
    }

    private static W128WebRegistrySavedData data(CommandSourceStack source) {
        return W128WebRegistrySavedData.get(source.getLevel());
    }

    private static int result(CommandSourceStack source, W128WebBuildResult result) {
        if (result.success()) {
            source.sendSuccess(
                    () -> Component.literal(result.message()).withStyle(ChatFormatting.GREEN),
                    false
            );
            return 1;
        }
        return fail(source, result.message());
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
