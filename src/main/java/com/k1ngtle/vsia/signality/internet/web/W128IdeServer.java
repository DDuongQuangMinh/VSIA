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

        long now = System.currentTimeMillis();
        W131IdePlatform.initialize(data, project, now);
        project = data.project(project.host()).orElse(project);

        String selected = defaultPath(project);
        W131WorkspaceState.rememberFile(
                data,
                project,
                selected,
                now
        );
        send(player, project, selected, "VS:IA W1.31 Project Intelligence ready.", true);
        W130IdePlatform.sendInitialState(player, project);
        W131IdePlatform.sendInitialState(player, project);

        return W128WebBuildResult.ok("Opened VS:IA W1.31 IDE for " + project.host());
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
            case RENAME -> rename(
                    data,
                    project,
                    player,
                    requestedPath,
                    request.content(),
                    now
            );
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
            case SEARCH -> search(project, player, request.content());
            case RUN -> run(project, player, requestedPath);
            case VALIDATE -> validate(project, player, requestedPath);
            case TERMINAL -> terminal(
                    data,
                    project,
                    player,
                    requestedPath,
                    request.content(),
                    now
            );
            case W130 -> W131IdePlatform.handle(
                    data,
                    project,
                    player,
                    requestedPath,
                    request.content(),
                    now
            );
        };

        project = data.project(project.host()).orElse(project);

        String selected = switch (request.action()) {
            case CREATE -> operation.success()
                    ? normalizeSelectablePath(project, request.path())
                    : requestedPath;
            case DELETE -> operation.success()
                    ? defaultPath(project)
                    : requestedPath;
            case RENAME -> operation.success()
                    ? normalizeSelectablePath(project, request.content())
                    : requestedPath;
            default -> requestedPath;
        };

        if (selected.isBlank() || project.file(selected) == null) {
            selected = defaultPath(project);
        }

        if (!selected.isBlank()
                && project.file(selected) != null) {
            W131WorkspaceState.rememberFile(
                    data,
                    project,
                    selected,
                    now
            );
        }

        send(player, project, selected, operation.message(), operation.success());
        W131IdePlatform.sendWorkspaceState(
                player,
                project
        );
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

        if (W130Workspace.internalPath(path)) {
            W130Workspace.writeInternal(data, project, path, content, now);
            return W128WebBuildResult.ok(
                    "Saved workspace file " + path + " | website publication unchanged"
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

    private static W128WebBuildResult rename(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String oldPath,
            String requestedNewPath,
            long now
    ) {
        if (oldPath == null || oldPath.isBlank()) {
            return W128WebBuildResult.fail("Select a file first.");
        }

        String newPath;

        try {
            newPath = W128WebRegistrySavedData.normalizePath(
                    requestedNewPath
            );
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(
                    exception.getMessage()
            );
        }

        if (oldPath.equals(newPath)) {
            return W128WebBuildResult.fail(
                    "New path is the same as the current path."
            );
        }

        W128WebFile source = project.file(oldPath);

        if (source == null) {
            return W128WebBuildResult.fail(
                    "File not found: " + oldPath
            );
        }

        if (project.file(newPath) != null) {
            return W128WebBuildResult.fail(
                    "Destination already exists: " + newPath
            );
        }

        W128WebBuildResult saveResult = data.putFile(
                player.getUUID(),
                project.host(),
                newPath,
                source.content(),
                now
        );

        if (!saveResult.success()) {
            return saveResult;
        }

        W128WebBuildResult removeResult = data.removeFile(
                player.getUUID(),
                project.host(),
                oldPath,
                now
        );

        if (!removeResult.success()) {
            return W128WebBuildResult.fail(
                    "Rename wrote the destination but could not remove the old path: "
                            + removeResult.message()
            );
        }

        return W128WebBuildResult.ok(
                "Renamed " + oldPath + " -> " + newPath
                        + " | publish required"
        );
    }

    private static W128WebBuildResult search(
            W128WebProject project,
            ServerPlayer player,
            String rawQuery
    ) {
        String query = rawQuery == null
                ? ""
                : rawQuery.trim();

        if (query.isEmpty()) {
            sendEvent(
                    player,
                    "SEARCH",
                    false,
                    "Search",
                    "Enter a search query."
            );
            return W128WebBuildResult.fail(
                    "Search query is empty."
            );
        }

        String lowerQuery = query.toLowerCase();
        StringBuilder payload = new StringBuilder();
        int matches = 0;

        for (W128WebFile file : project.files().values()) {
            String[] lines = file.content()
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (!line.toLowerCase().contains(lowerQuery)) {
                    continue;
                }

                if (matches >= 100) {
                    payload.append("\n... search capped at 100 matches");
                    break;
                }

                if (!payload.isEmpty()) {
                    payload.append('\n');
                }

                String preview = line.trim();
                if (preview.length() > 120) {
                    preview = preview.substring(0, 117) + "...";
                }

                payload.append(file.path())
                        .append('|')
                        .append(i + 1)
                        .append('|')
                        .append(preview.replace("|", "/"));

                matches++;
            }

            if (matches >= 100) {
                break;
            }
        }

        sendEvent(
                player,
                "SEARCH",
                true,
                "Search: " + query,
                payload.toString()
        );

        return W128WebBuildResult.ok(
                "Search complete: " + matches + " match(es)"
        );
    }

    private static W128WebBuildResult run(
            W128WebProject project,
            ServerPlayer player,
            String path
    ) {
        W128WebFile file = project.file(path);

        if (file == null) {
            return W128WebBuildResult.fail(
                    "File not found: " + path
            );
        }

        W129RunResult result = W129ComputeEngine.run(
                path,
                file.content()
        );

        sendEvent(
                player,
                "TERMINAL",
                result.success(),
                "Run " + path,
                result.terminalText(
                        path,
                        W129ComputeEngine.runtimeName(path)
                )
        );

        sendProblems(
                player,
                result.diagnostics()
        );

        return result.success()
                ? W128WebBuildResult.ok(
                "Run complete: " + path
        )
                : W128WebBuildResult.fail(
                "Run failed: " + path
        );
    }

    private static W128WebBuildResult validate(
            W128WebProject project,
            ServerPlayer player,
            String path
    ) {
        W128WebFile file = project.file(path);

        if (file == null) {
            return W128WebBuildResult.fail(
                    "File not found: " + path
            );
        }

        java.util.List<W129Diagnostic> diagnostics =
                W129ComputeEngine.validate(
                        path,
                        file.content()
                );

        sendProblems(
                player,
                diagnostics
        );

        long errors = diagnostics.stream()
                .filter(
                        diagnostic ->
                                diagnostic.severity()
                                        == W129Diagnostic.Severity.ERROR
                )
                .count();

        long warnings = diagnostics.stream()
                .filter(
                        diagnostic ->
                                diagnostic.severity()
                                        == W129Diagnostic.Severity.WARNING
                )
                .count();

        String summary =
                errors
                        + " error(s), "
                        + warnings
                        + " warning(s), "
                        + diagnostics.size()
                        + " diagnostic(s)";

        sendEvent(
                player,
                "OUTPUT",
                errors == 0L,
                "Validation " + path,
                summary
        );

        return errors == 0L
                ? W128WebBuildResult.ok(
                "Validation passed: " + summary
        )
                : W128WebBuildResult.fail(
                "Validation failed: " + summary
        );
    }

    private static W128WebBuildResult terminal(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String rawCommand,
            long now
    ) {
        String command = rawCommand == null
                ? ""
                : rawCommand.trim();

        if (command.isEmpty()) {
            return W128WebBuildResult.ok(
                    "Terminal ready."
            );
        }

        String[] parts = command.split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String argument = parts.length > 1
                ? parts[1].trim()
                : "";

        if (W131IdePlatform.handlesTerminalVerb(verb)) {
            return W131IdePlatform.handle(
                    data,
                    project,
                    player,
                    activePath,
                    command,
                    now
            );
        }

        switch (verb) {
            case "clear" -> {
                sendEvent(
                        player,
                        "TERMINAL_CLEAR",
                        true,
                        "Terminal",
                        ""
                );
                return W128WebBuildResult.ok(
                        "Terminal cleared."
                );
            }

            case "help" -> {
                sendEvent(
                        player,
                        "TERMINAL",
                        true,
                        "Terminal Help",
                        """
                        W1.31 terminal commands:
                          help
                          clear
                          pwd
                          cd <path>
                          ls [path]
                          tree [path]
                          cat <path>
                          mkdir <path>
                          touch <path>
                          cp <source> <destination>
                          mv <source> <destination>
                          rm <path>
                          run [path]
                          check [path]
                          build
                          publish
                          unpublish
                          status
                          selftest
                          w130 help
                          w130 selftest
                          w131 help
                          w131 selftest
                          w131 index
                          w131 diagnostics
                          w131 outline [path]
                          w131 symbols [query]
                          w131 definition <symbol>
                          w131 references <symbol>
                          w131 complete [prefix]
                          w131 recent
                          debug start|continue|pause|next|step|out|restart|stop
                          break <line>
                          watch add|remove <expression>
                          task list|run <label>
                          scm status|stage|unstage|commit|diff|log|revert
                        """
                );
                return W128WebBuildResult.ok(
                        "Terminal help."
                );
            }

            case "pwd" -> {
                sendEvent(
                        player,
                        "TERMINAL",
                        true,
                        "pwd",
                        "/" + project.host()
                );
                return W128WebBuildResult.ok("pwd");
            }

            case "ls" -> {
                String listing = project.files()
                        .keySet()
                        .stream()
                        .sorted()
                        .collect(
                                java.util.stream.Collectors.joining("\n")
                        );

                sendEvent(
                        player,
                        "TERMINAL",
                        true,
                        "ls",
                        listing
                );

                return W128WebBuildResult.ok("ls");
            }

            case "cat" -> {
                String path = argument.isBlank()
                        ? activePath
                        : normalizeSelectablePath(
                                project,
                                argument
                        );

                W128WebFile file = project.file(path);

                if (file == null) {
                    sendEvent(
                            player,
                            "TERMINAL",
                            false,
                            "cat",
                            "File not found: " + path
                    );
                    return W128WebBuildResult.fail(
                            "File not found: " + path
                    );
                }

                String content = file.content();

                if (content.length() > 16_000) {
                    content = content.substring(0, 16_000)
                            + "\n... output truncated";
                }

                sendEvent(
                        player,
                        "TERMINAL",
                        true,
                        "cat " + path,
                        content
                );

                return W128WebBuildResult.ok(
                        "cat " + path
                );
            }

            case "run" -> {
                String path = argument.isBlank()
                        ? activePath
                        : W131WorkspaceFs.resolvePath(
                                W131WorkspaceState.cwd(project),
                                argument
                        );

                return run(
                        project,
                        player,
                        path
                );
            }

            case "check", "validate" -> {
                String path = argument.isBlank()
                        ? activePath
                        : W131WorkspaceFs.resolvePath(
                                W131WorkspaceState.cwd(project),
                                argument
                        );

                return validate(
                        project,
                        player,
                        path
                );
            }

            case "build" -> {
                W128WebBuildResult result = data.build(
                        player.getUUID(),
                        project.host(),
                        now
                );

                sendEvent(
                        player,
                        "OUTPUT",
                        result.success(),
                        "Build",
                        result.message()
                );

                return result;
            }

            case "publish" -> {
                W128WebBuildResult result = data.publish(
                        player.serverLevel(),
                        player.getUUID(),
                        project.host(),
                        now
                );

                sendEvent(
                        player,
                        "OUTPUT",
                        result.success(),
                        "Publish",
                        result.message()
                );

                return result;
            }

            case "unpublish" -> {
                W128WebBuildResult result = data.unpublish(
                        player.getUUID(),
                        project.host(),
                        now
                );

                sendEvent(
                        player,
                        "OUTPUT",
                        result.success(),
                        "Unpublish",
                        result.message()
                );

                return result;
            }

            case "status" -> {
                String payload =
                        "Host: "
                                + project.host()
                                + "\nMode: "
                                + project.mode()
                                + "\nServer: "
                                + (
                                project.boundServerIp().isBlank()
                                        ? "UNBOUND"
                                        : project.boundServerIp()
                        )
                                + "\nPublished: "
                                + project.published()
                                + "\nRevision: "
                                + project.buildRevision()
                                + "\nFiles: "
                                + project.files().size()
                                + "\nCharacters: "
                                + project.totalCharacters();

                sendEvent(
                        player,
                        "TERMINAL",
                        true,
                        "status",
                        payload
                );

                return W128WebBuildResult.ok(
                        "Status printed."
                );
            }

            case "selftest" -> {
                String payload =
                        W129SelfTest.run();

                boolean success =
                        !payload.contains("[FAIL]");

                sendEvent(
                        player,
                        "TERMINAL",
                        success,
                        "W1.29 self-test",
                        payload
                );

                return success
                        ? W128WebBuildResult.ok(
                        "W1.29 self-test passed."
                )
                        : W128WebBuildResult.fail(
                        "W1.29 self-test failed."
                );
            }

            case "w130", "w131", "debug", "break", "watch", "task", "scm", "workspace" -> {
                String forwarded = "w130".equals(verb)
                        ? argument
                        : command;
                return W131IdePlatform.handle(
                        data,
                        project,
                        player,
                        activePath,
                        forwarded,
                        now
                );
            }

            default -> {
                sendEvent(
                        player,
                        "TERMINAL",
                        false,
                        "Terminal",
                        "Unknown command: "
                                + verb
                                + "\nType 'help' for commands."
                );

                return W128WebBuildResult.fail(
                        "Unknown terminal command: "
                                + verb
                );
            }
        }
    }

    private static void sendProblems(
            ServerPlayer player,
            java.util.List<W129Diagnostic> diagnostics
    ) {
        String payload = diagnostics.stream()
                .map(W129Diagnostic::toWire)
                .collect(
                        java.util.stream.Collectors.joining("\n")
                );

        sendEvent(
                player,
                "PROBLEMS",
                diagnostics.stream()
                        .noneMatch(
                                diagnostic ->
                                        diagnostic.severity()
                                                == W129Diagnostic.Severity.ERROR
                        ),
                "Problems",
                payload
        );
    }

    private static void sendEvent(
            ServerPlayer player,
            String kind,
            boolean success,
            String title,
            String payload
    ) {
        VsiaNetwork.sendToPlayer(
                player,
                new com.k1ngtle.vsia.network.web.W129IdeEventPacket(
                        kind,
                        success,
                        title,
                        payload
                )
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
                .filter(path -> !W130Workspace.internalPath(path))
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
