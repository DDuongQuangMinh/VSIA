package com.k1ngtle.vsia.signality.internet.web;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W128IdeRequestPacket;
import com.k1ngtle.vsia.network.web.W128IdeSnapshotPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public final class W128IdeServer {
    private W128IdeServer() {
    }

    public static W128WebBuildResult open(ServerPlayer player, String requestedHost) {
        W128WebRegistrySavedData data = W128WebRegistrySavedData.get(player.serverLevel());
        W128WebProject project = data.project(requestedHost).orElse(null);

        if (project == null) {
            return W128WebBuildResult.fail("Web project not found.");
        }
        if (!project.ownerUuid().equals(player.getUUID())) {
            return W128WebBuildResult.fail("You do not own this web project.");
        }

        String selected = defaultPath(project);
        send(player, project, selected, "VS:IA Web IDE ready.", true);

        return W128WebBuildResult.ok("Opened Web IDE for " + project.host());
    }

    public static void handle(ServerPlayer player, W128IdeRequestPacket request) {
        W128WebRegistrySavedData data = W128WebRegistrySavedData.get(player.serverLevel());
        W128WebProject project = data.project(request.host()).orElse(null);

        if (project == null) {
            player.sendSystemMessage(Component.literal("Web IDE: project not found."));
            return;
        }
        if (!project.ownerUuid().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Web IDE: you do not own this project."));
            return;
        }

        long now = System.currentTimeMillis();
        String requestedPath = normalizeSelectablePath(project, request.path());

        W128WebBuildResult operation = switch (request.action()) {
            case OPEN, REFRESH -> W128WebBuildResult.ok(
                    request.action() == W128IdeAction.OPEN
                            ? "File opened."
                            : "Project refreshed."
            );
            case SAVE -> save(data, project, player, requestedPath, request.content(), now);
            case CREATE -> create(data, project, player, request.path(), now);
            case DELETE -> delete(data, project, player, requestedPath, now);
            case BUILD -> data.build(player.getUUID(), project.host(), now);
            case PUBLISH -> data.publish(
                    player.serverLevel(),
                    player.getUUID(),
                    project.host(),
                    now
            );
            case UNPUBLISH -> data.unpublish(player.getUUID(), project.host(), now);
            case IMPORT_BOOK -> importBook(data, project, player, requestedPath, now);
            case REGENERATE -> regenerate(data, project, player, requestedPath, now);
        };

        project = data.project(project.host()).orElse(project);

        String selected = switch (request.action()) {
            case CREATE -> operation.success()
                    ? normalizeSelectablePath(project, request.path())
                    : requestedPath;
            case DELETE -> operation.success()
                    ? defaultPath(project)
                    : requestedPath;
            default -> requestedPath;
        };

        if (selected.isBlank() || project.file(selected) == null) {
            selected = defaultPath(project);
        }

        send(player, project, selected, operation.message(), operation.success());
    }

    private static W128WebBuildResult save(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String path,
            String content,
            long now
    ) {
        if (path.isBlank()) {
            return W128WebBuildResult.fail("Select or create a file first.");
        }

        return data.putFile(
                player.getUUID(),
                project.host(),
                path,
                content,
                now
        );
    }

    private static W128WebBuildResult create(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String requestedPath,
            long now
    ) {
        String path;

        try {
            path = W128WebRegistrySavedData.normalizePath(requestedPath);
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(exception.getMessage());
        }

        if (project.file(path) != null) {
            return W128WebBuildResult.fail("File already exists: " + path);
        }

        return data.putFile(
                player.getUUID(),
                project.host(),
                path,
                "",
                now
        );
    }

    private static W128WebBuildResult delete(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String path,
            long now
    ) {
        if (path.isBlank()) {
            return W128WebBuildResult.fail("Select a file first.");
        }

        return data.removeFile(
                player.getUUID(),
                project.host(),
                path,
                now
        );
    }

    private static W128WebBuildResult importBook(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String path,
            long now
    ) {
        if (path.isBlank()) {
            return W128WebBuildResult.fail(
                    "Select or create a destination file first."
            );
        }

        String content = bookText(player);
        if (content == null) {
            return W128WebBuildResult.fail(
                    "Hold a Book & Quill or Written Book in either hand."
            );
        }

        return data.putFile(
                player.getUUID(),
                project.host(),
                path,
                content,
                now
        );
    }

    private static W128WebBuildResult regenerate(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String path,
            long now
    ) {
        if (project.mode() != W128WebMode.REACT) {
            return W128WebBuildResult.fail(
                    "Regenerate is only available for React build outputs."
            );
        }

        if (!managedOutput(path)) {
            return W128WebBuildResult.fail(
                    "This file is not managed by the React build."
            );
        }

        W128WebFile previous = project.file(path);

        if (previous != null) {
            project.removeFile(path, now);
        }

        W128WebBuildResult result = data.build(
                player.getUUID(),
                project.host(),
                now
        );

        if (!result.success()) {
            if (previous != null) {
                project.putFile(previous);
            }

            return W128WebBuildResult.fail(
                    "Regenerate failed: " + result.message()
            );
        }

        W128WebProject refreshed = data.project(project.host()).orElse(project);

        return W128WebBuildResult.ok(
                "Regenerated "
                        + path
                        + " | revision="
                        + refreshed.buildRevision()
                        + " | publish required"
        );
    }

    private static boolean managedOutput(String path) {
        return "/app.js".equals(path)
                || "/index.html".equals(path)
                || "/react-runtime.js".equals(path);
    }

    private static void send(
            ServerPlayer player,
            W128WebProject project,
            String selected,
            String message,
            boolean success
    ) {
        VsiaNetwork.sendToPlayer(
                player,
                W128IdeSnapshotPacket.fromProject(
                        project,
                        selected,
                        message,
                        success
                )
        );
    }

    private static String normalizeSelectablePath(
            W128WebProject project,
            String requestedPath
    ) {
        if (requestedPath == null || requestedPath.isBlank()) {
            return defaultPath(project);
        }

        try {
            return W128WebRegistrySavedData.normalizePath(requestedPath);
        } catch (IllegalArgumentException ignored) {
            return defaultPath(project);
        }
    }

    private static String defaultPath(W128WebProject project) {
        String preferred = project.mode() == W128WebMode.REACT
                ? "/src/App.jsx"
                : "/index.html";

        if (project.file(preferred) != null) {
            return preferred;
        }

        return project.files()
                .keySet()
                .stream()
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .orElse("");
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
        return stack != null
                && (stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK));
    }
}
