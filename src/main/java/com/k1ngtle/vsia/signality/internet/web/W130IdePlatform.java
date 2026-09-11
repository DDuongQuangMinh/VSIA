package com.k1ngtle.vsia.signality.internet.web;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W129IdeEventPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class W130IdePlatform {
    private W130IdePlatform() {
    }

    public static void initialize(
            W128WebRegistrySavedData data,
            W128WebProject project,
            long nowMillis
    ) {
        W130Workspace.ensureDefaults(data, project, nowMillis);
        W130SourceControl.ensureRepository(data, project, nowMillis);
    }

    public static W128WebBuildResult handle(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String rawCommand,
            long nowMillis
    ) {
        initialize(data, project, nowMillis);

        String command = rawCommand == null ? "" : rawCommand.trim();
        if (command.isEmpty()) {
            sendWorkspace(player, project);
            return W128WebBuildResult.ok("W1.30 workspace ready.");
        }

        String lower = command.toLowerCase(Locale.ROOT);

        if (lower.equals("help") || lower.equals("w130 help")) {
            sendEvent(player, "TERMINAL", true, "W1.30 Help", help());
            return W128WebBuildResult.ok("W1.30 help printed.");
        }

        if (lower.equals("selftest") || lower.equals("w130 selftest")) {
            String payload = W130SelfTest.run();
            boolean success = !payload.contains("[FAIL]");
            sendEvent(player, "TERMINAL", success, "W1.30 self-test", payload);
            return success
                    ? W128WebBuildResult.ok("W1.30 self-test passed.")
                    : W128WebBuildResult.fail("W1.30 self-test failed.");
        }

        if (lower.startsWith("debug")) {
            return debug(data, project, player, activePath, command, nowMillis);
        }

        if (lower.startsWith("break")) {
            return breakpoint(data, project, player, activePath, command, nowMillis);
        }

        if (lower.startsWith("watch")) {
            return watch(data, project, player, activePath, command, nowMillis);
        }

        if (lower.startsWith("task")) {
            return task(data, project, player, activePath, command, nowMillis);
        }

        if (lower.startsWith("scm") || lower.startsWith("source control")) {
            return sourceControl(data, project, player, activePath, command, nowMillis);
        }

        if (lower.startsWith("workspace")) {
            sendWorkspace(player, project);
            return W128WebBuildResult.ok("Workspace status printed.");
        }

        return W128WebBuildResult.fail(
                "Unknown W1.30 command: " + command
                        + " | use w130 help"
        );
    }

    public static String help() {
        return """
                W1.30 development-platform commands:
                  w130 selftest
                  workspace status
                  debug start
                  debug continue
                  debug pause
                  debug next
                  debug step
                  debug out
                  debug restart
                  debug stop
                  break <line>
                  break <path>:<line>
                  watch add <expression>
                  watch remove <expression>
                  task list
                  task run <label>
                  scm status
                  scm stage [path|*]
                  scm unstage [path|*]
                  scm commit <message>
                  scm diff [path]
                  scm log
                  scm revert [path|*]
                """;
    }

    private static W128WebBuildResult debug(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String tail = tail(command, "debug").toLowerCase(Locale.ROOT);
        W130Debugger.DebugResult result;

        switch (tail) {
            case "", "start" -> result = W130Debugger.start(player.getUUID(), project, activePath);
            case "continue", "cont", "c" -> result = W130Debugger.continueRun(player.getUUID(), project);
            case "pause" -> result = W130Debugger.pause(player.getUUID(), project);
            case "next", "over", "step over" -> result = W130Debugger.stepOver(player.getUUID(), project);
            case "step", "into", "step into" -> result = W130Debugger.stepInto(player.getUUID(), project);
            case "out", "step out" -> result = W130Debugger.stepOut(player.getUUID(), project);
            case "restart" -> result = W130Debugger.restart(player.getUUID(), project);
            case "stop" -> result = W130Debugger.stop(player.getUUID(), project);
            case "status" -> {
                sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Debug status");
                return W128WebBuildResult.ok("Debug status refreshed.");
            }
            default -> {
                return W128WebBuildResult.fail("Unknown debug command: " + tail);
            }
        }

        sendDebug(player, result.snapshot(), result.success(), result.message());

        if (result.terminated()
                && "completed".equalsIgnoreCase(result.snapshot().reason())
                && !result.snapshot().path().isBlank()) {
            W128WebFile file = project.file(result.snapshot().path());
            if (file != null && W129ComputeEngine.executable(file.path())) {
                W129RunResult run = W129ComputeEngine.run(file.path(), file.content());
                sendEvent(
                        player,
                        "TERMINAL",
                        run.success(),
                        "Debug program output " + file.path(),
                        run.terminalText(file.path(), W129ComputeEngine.runtimeName(file.path()))
                );
                sendProblems(player, run.diagnostics(), file.path());
            } else if (file != null
                    && (W128IdeLanguage.detect(file.path()) == W128IdeLanguage.JAVASCRIPT
                    || W128IdeLanguage.detect(file.path()) == W128IdeLanguage.JSX)) {
                sendEvent(
                        player,
                        "OUTPUT",
                        true,
                        "Web Debug " + file.path(),
                        "Source-level JavaScript/JSX debug walk completed. "
                                + "Website execution remains on the W1.28 web runtime; use Build + Publish to run the site."
                );
            }
        }

        return result.success()
                ? W128WebBuildResult.ok(result.message())
                : W128WebBuildResult.fail(result.message());
    }

    private static W128WebBuildResult breakpoint(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String argument = tail(command, "break");
        if (argument.isBlank()) {
            sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Breakpoints");
            return W128WebBuildResult.ok("Breakpoint list refreshed.");
        }

        String path = activePath;
        String rawLine = argument;
        int colon = argument.lastIndexOf(':');
        if (colon > 0) {
            path = argument.substring(0, colon).trim();
            rawLine = argument.substring(colon + 1).trim();
        }

        int line;
        try {
            line = Integer.parseInt(rawLine);
        } catch (NumberFormatException exception) {
            return W128WebBuildResult.fail("Invalid breakpoint line: " + rawLine);
        }

        if (project.file(path) == null) {
            return W128WebBuildResult.fail("Breakpoint file not found: " + path);
        }

        boolean added = W130Workspace.toggleBreakpoint(data, project, path, line, nowMillis);
        W130Debugger.refreshWorkspaceState(player.getUUID(), project);
        sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Breakpoint updated");
        return W128WebBuildResult.ok(
                (added ? "Added" : "Removed") + " breakpoint " + path + ":" + line
        );
    }

    private static W128WebBuildResult watch(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String argument = tail(command, "watch");
        String lower = argument.toLowerCase(Locale.ROOT);

        if (lower.startsWith("add ")) {
            String expression = argument.substring(4).trim();
            W130Workspace.addWatch(data, project, expression, nowMillis);
            W130Debugger.refreshWorkspaceState(player.getUUID(), project);
            sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Watch added");
            return W128WebBuildResult.ok("Watch added: " + expression);
        }

        if (lower.startsWith("remove ")) {
            String expression = argument.substring(7).trim();
            W130Workspace.removeWatch(data, project, expression, nowMillis);
            W130Debugger.refreshWorkspaceState(player.getUUID(), project);
            sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Watch removed");
            return W128WebBuildResult.ok("Watch removed: " + expression);
        }

        sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Watches");
        return W128WebBuildResult.ok("Watch list refreshed.");
    }

    private static W128WebBuildResult task(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String argument = tail(command, "task");
        if (argument.isBlank() || "list".equalsIgnoreCase(argument)) {
            String payload = W130TaskEngine.tasks(project).stream()
                    .map(task -> task.label() + " -> " + task.command())
                    .collect(Collectors.joining("\n"));
            sendEvent(player, "OUTPUT", true, "Tasks", payload.isBlank() ? "No tasks configured." : payload);
            return W128WebBuildResult.ok("Task list printed.");
        }

        String label = argument.toLowerCase(Locale.ROOT).startsWith("run ")
                ? argument.substring(4).trim()
                : argument.trim();

        W130TaskEngine.ResolvedTask resolved;
        try {
            resolved = W130TaskEngine.resolve(project, label, activePath);
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(exception.getMessage());
        }

        sendEvent(
                player,
                "TERMINAL",
                true,
                "Task " + resolved.label(),
                "Dependency order: " + String.join(" -> ", resolved.order())
        );

        for (String taskCommand : resolved.commands()) {
            W128WebBuildResult result = executeTaskCommand(
                    data,
                    project,
                    player,
                    activePath,
                    taskCommand,
                    nowMillis
            );
            if (!result.success()) {
                return W128WebBuildResult.fail(
                        "Task " + label + " failed: " + result.message()
                );
            }
            project = data.project(project.host()).orElse(project);
        }

        return W128WebBuildResult.ok("Task complete: " + label);
    }

    private static W128WebBuildResult executeTaskCommand(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String trimmed = command.trim();
        String[] parts = trimmed.split("\\s+", 2);
        String verb = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1].trim() : "";

        switch (verb) {
            case "check", "validate" -> {
                String path = argument.isBlank() ? activePath : argument;
                W128WebFile file = project.file(path);
                if (file == null) {
                    return W128WebBuildResult.fail("Task file not found: " + path);
                }
                List<W129Diagnostic> diagnostics = W129ComputeEngine.validate(path, file.content());
                sendProblems(player, diagnostics, path);
                boolean success = diagnostics.stream().noneMatch(
                        diagnostic -> diagnostic.severity() == W129Diagnostic.Severity.ERROR
                );
                sendEvent(
                        player,
                        "OUTPUT",
                        success,
                        "Task check " + path,
                        success ? "Validation passed." : "Validation failed."
                );
                return success
                        ? W128WebBuildResult.ok("Validation passed: " + path)
                        : W128WebBuildResult.fail("Validation failed: " + path);
            }
            case "run" -> {
                String path = argument.isBlank() ? activePath : argument;
                W128WebFile file = project.file(path);
                if (file == null) {
                    return W128WebBuildResult.fail("Task file not found: " + path);
                }
                W129RunResult run = W129ComputeEngine.run(path, file.content());
                sendEvent(
                        player,
                        "TERMINAL",
                        run.success(),
                        "Task run " + path,
                        run.terminalText(path, W129ComputeEngine.runtimeName(path))
                );
                sendProblems(player, run.diagnostics(), path);
                return run.success()
                        ? W128WebBuildResult.ok("Run passed: " + path)
                        : W128WebBuildResult.fail("Run failed: " + path);
            }
            case "build" -> {
                W128WebBuildResult result = data.build(player.getUUID(), project.host(), nowMillis);
                sendEvent(player, "OUTPUT", result.success(), "Task build", result.message());
                return result;
            }
            case "publish" -> {
                W128WebBuildResult result = data.publish(
                        player.serverLevel(),
                        player.getUUID(),
                        project.host(),
                        nowMillis
                );
                sendEvent(player, "OUTPUT", result.success(), "Task publish", result.message());
                return result;
            }
            case "w130" -> {
                return handle(data, project, player, activePath, argument, nowMillis);
            }
            default -> {
                return W128WebBuildResult.fail(
                        "Task command is not permitted by the VSIA sandbox: " + verb
                );
            }
        }
    }

    private static W128WebBuildResult sourceControl(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String command,
            long nowMillis
    ) {
        String argument = command.toLowerCase(Locale.ROOT).startsWith("source control")
                ? tail(command, "source control")
                : tail(command, "scm");
        String[] parts = argument.split("\\s+", 2);
        String verb = parts.length == 0 || parts[0].isBlank() ? "status" : parts[0].toLowerCase(Locale.ROOT);
        String value = parts.length > 1 ? parts[1].trim() : "";
        W128WebBuildResult result;

        switch (verb) {
            case "status" -> result = W128WebBuildResult.ok("Source-control status refreshed.");
            case "stage" -> result = W130SourceControl.stage(
                    data,
                    project,
                    value.isBlank() ? activePath : value,
                    nowMillis
            );
            case "unstage" -> result = W130SourceControl.unstage(
                    data,
                    project,
                    value.isBlank() ? activePath : value,
                    nowMillis
            );
            case "commit" -> result = W130SourceControl.commit(data, project, value, nowMillis);
            case "revert" -> result = W130SourceControl.revert(
                    data,
                    project,
                    value.isBlank() ? activePath : value,
                    nowMillis
            );
            case "diff" -> {
                String path = value.isBlank() ? activePath : value;
                sendEvent(
                        player,
                        "OUTPUT",
                        true,
                        "Diff " + path,
                        W130SourceControl.diff(project, path)
                );
                result = W128WebBuildResult.ok("Diff printed: " + path);
            }
            case "log", "history" -> {
                W130ScmSnapshot snapshot = W130SourceControl.snapshot(project);
                String payload = snapshot.history().isEmpty()
                        ? "No commits yet."
                        : snapshot.history().stream()
                        .map(commit -> "#" + commit.id() + " | " + commit.message() + " | " + commit.atMillis())
                        .collect(Collectors.joining("\n"));
                sendEvent(player, "OUTPUT", true, "Source Control History", payload);
                result = W128WebBuildResult.ok("Source-control history printed.");
            }
            default -> result = W128WebBuildResult.fail("Unknown source-control command: " + verb);
        }

        project = data.project(project.host()).orElse(project);
        sendScm(player, project, result.success(), result.message());
        return result;
    }

    private static void sendWorkspace(ServerPlayer player, W128WebProject project) {
        W130Workspace.LaunchConfig launch = W130Workspace.launchConfig(project, "");
        String payload =
                "Workspace: " + project.host()
                        + "\nSettings: " + W130Workspace.SETTINGS_PATH
                        + "\nLaunch: " + W130Workspace.LAUNCH_PATH
                        + "\nTasks: " + W130Workspace.TASKS_PATH
                        + "\nState: " + W130Workspace.STATE_PATH
                        + "\nLaunch configuration: " + launch.name()
                        + "\nConfigured tasks: " + W130TaskEngine.tasks(project).size();
        sendEvent(player, "OUTPUT", true, "W1.30 Workspace", payload);
    }

    public static void sendInitialState(ServerPlayer player, W128WebProject project) {
        sendDebug(player, W130Debugger.snapshot(player.getUUID(), project), true, "Debugger ready");
        sendScm(player, project, true, "Source control ready");
    }

    private static void sendDebug(
            ServerPlayer player,
            W130DebugSnapshot snapshot,
            boolean success,
            String title
    ) {
        sendEvent(player, "W130_DEBUG", success, title, snapshot.toJson());
    }

    private static void sendScm(
            ServerPlayer player,
            W128WebProject project,
            boolean success,
            String title
    ) {
        sendEvent(player, "W130_SCM", success, title, W130SourceControl.snapshot(project).toJson());
    }

    private static void sendProblems(
            ServerPlayer player,
            List<W129Diagnostic> diagnostics,
            String path
    ) {
        String payload = diagnostics.stream()
                .map(diagnostic -> diagnostic.severity().name()
                        + "|" + path
                        + "|" + diagnostic.line()
                        + "|" + diagnostic.column()
                        + "|" + diagnostic.message().replace("|", "/"))
                .collect(Collectors.joining("\n"));

        sendEvent(
                player,
                "W130_PROBLEMS",
                diagnostics.stream().noneMatch(
                        diagnostic -> diagnostic.severity() == W129Diagnostic.Severity.ERROR
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
                new W129IdeEventPacket(kind, success, title, payload)
        );
    }

    private static String tail(String command, String prefix) {
        String value = command == null ? "" : command.trim();
        if (value.length() <= prefix.length()) {
            return "";
        }
        return value.substring(prefix.length()).trim();
    }
}
