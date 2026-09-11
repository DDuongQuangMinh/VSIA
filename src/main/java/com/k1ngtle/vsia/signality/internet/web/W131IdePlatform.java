package com.k1ngtle.vsia.signality.internet.web;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.network.web.W129IdeEventPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class W131IdePlatform {
    private static final Set<String> DIRECT_VERBS = Set.of(
            "index",
            "outline",
            "symbols",
            "definition",
            "references",
            "diagnostics",
            "complete",
            "recent",
            "open"
    );

    private W131IdePlatform() {
    }

    public static void initialize(
            W128WebRegistrySavedData data,
            W128WebProject project,
            long nowMillis
    ) {
        W130IdePlatform.initialize(
                data,
                project,
                nowMillis
        );

        W131WorkspaceState.ensureDefaults(
                data,
                project,
                nowMillis
        );
    }

    public static boolean handlesTerminalVerb(String verb) {
        if (verb == null || verb.isBlank()) {
            return false;
        }

        String value = verb.trim().toLowerCase(Locale.ROOT);

        return "w131".equals(value)
                || W131WorkspaceFs.handlesTerminalVerb(value)
                || DIRECT_VERBS.contains(value);
    }

    public static W128WebBuildResult handle(
            W128WebRegistrySavedData data,
            W128WebProject project,
            ServerPlayer player,
            String activePath,
            String rawCommand,
            long nowMillis
    ) {
        initialize(
                data,
                project,
                nowMillis
        );

        String command = rawCommand == null
                ? ""
                : rawCommand.trim();

        String lower = command.toLowerCase(Locale.ROOT);
        boolean explicitW131 =
                lower.equals("w131")
                        || lower.startsWith("w131 ");

        if (explicitW131) {
            command = command.length() <= 4
                    ? ""
                    : command.substring(4).trim();
            lower = command.toLowerCase(Locale.ROOT);
        }

        if (!explicitW131
                && !isDirectW131Command(command)) {
            return W130IdePlatform.handle(
                    data,
                    project,
                    player,
                    activePath,
                    rawCommand,
                    nowMillis
            );
        }

        if (command.isBlank()
                || "help".equals(lower)) {
            sendEvent(
                    player,
                    "TERMINAL",
                    true,
                    "W1.31 Help",
                    help()
            );

            return W128WebBuildResult.ok(
                    "W1.31 help printed."
            );
        }

        if ("selftest".equals(lower)) {
            String payload = W131SelfTest.run();
            boolean success = !payload.contains("[FAIL]");

            sendEvent(
                    player,
                    "TERMINAL",
                    success,
                    "W1.31 self-test",
                    payload
            );

            return success
                    ? W128WebBuildResult.ok(
                    "W1.31 self-test passed."
            )
                    : W128WebBuildResult.fail(
                    "W1.31 self-test failed."
            );
        }

        List<String> tokens =
                W131WorkspaceFs.tokenize(command);

        String verb = tokens.isEmpty()
                ? ""
                : tokens.get(0).toLowerCase(Locale.ROOT);

        if (W131WorkspaceFs.handlesTerminalVerb(verb)) {
            W131WorkspaceFs.ShellResult result =
                    W131WorkspaceFs.execute(
                            data,
                            project,
                            player.getUUID(),
                            command,
                            nowMillis
                    );

            sendEvent(
                    player,
                    "TERMINAL",
                    result.success(),
                    result.title(),
                    result.payload()
            );

            sendWorkspaceState(
                    player,
                    project
            );

            return result.success()
                    ? W128WebBuildResult.ok(
                    result.message()
            )
                    : W128WebBuildResult.fail(
                    result.message()
            );
        }

        if (lower.startsWith("workspace ")) {
            command = command.substring(
                    "workspace ".length()
            ).trim();
            lower = command.toLowerCase(Locale.ROOT);
        }

        if ("index".equals(lower)) {
            sendEvent(
                    player,
                    "OUTPUT",
                    true,
                    "W1.31 Workspace Index",
                    W131SymbolIndex.summary(project)
            );

            return W128WebBuildResult.ok(
                    "Workspace symbol index refreshed."
            );
        }

        if (lower.equals("diagnostics")
                || lower.equals("diagnose")) {
            return diagnostics(
                    project,
                    player
            );
        }

        if (lower.equals("recent")) {
            List<String> recent =
                    W131WorkspaceState.recentFiles(
                            project
                    );

            sendEvent(
                    player,
                    "OUTPUT",
                    true,
                    "Recent Files",
                    recent.isEmpty()
                            ? "No recent files."
                            : String.join("\n", recent)
            );

            return W128WebBuildResult.ok(
                    "Recent files printed."
            );
        }

        if (lower.startsWith("outline")) {
            String argument = tail(
                    command,
                    "outline"
            );

            String path = argument.isBlank()
                    ? activePath
                    : resolveProjectPath(
                    project,
                    argument
            );

            if (project.file(path) == null) {
                return W128WebBuildResult.fail(
                        "Outline file not found: " + path
                );
            }

            List<W131SymbolIndex.Symbol> symbols =
                    W131SymbolIndex.outline(
                            project,
                            path
                    );

            sendSymbols(
                    player,
                    true,
                    "Outline " + path,
                    symbols
            );

            return W128WebBuildResult.ok(
                    "Outline refreshed: "
                            + symbols.size()
                            + " symbol(s)"
            );
        }

        if (lower.startsWith("symbols")) {
            String query = tail(
                    command,
                    "symbols"
            );

            List<W131SymbolIndex.Symbol> symbols =
                    W131SymbolIndex.search(
                            project,
                            query,
                            128
                    );

            sendSymbols(
                    player,
                    true,
                    query.isBlank()
                            ? "Workspace Symbols"
                            : "Workspace Symbols: " + query,
                    symbols
            );

            return W128WebBuildResult.ok(
                    "Workspace symbol search: "
                            + symbols.size()
                            + " result(s)"
            );
        }

        if (lower.startsWith("definition")) {
            String name = tail(
                    command,
                    "definition"
            );

            if (name.isBlank()) {
                return W128WebBuildResult.fail(
                        "Usage: definition <symbol>"
                );
            }

            List<W131SymbolIndex.Symbol> definitions =
                    W131SymbolIndex.definitions(
                            project,
                            name,
                            64
                    );

            if (definitions.isEmpty()) {
                sendSymbols(
                        player,
                        false,
                        "Definition: " + name,
                        List.of()
                );

                return W128WebBuildResult.fail(
                        "Definition not found: " + name
                );
            }

            if (definitions.size() == 1) {
                sendNavigate(
                        player,
                        definitions.get(0)
                );

                return W128WebBuildResult.ok(
                        "Definition found: " + name
                );
            }

            sendSymbols(
                    player,
                    true,
                    "Definitions: " + name,
                    definitions
            );

            return W128WebBuildResult.ok(
                    definitions.size()
                            + " definitions found for "
                            + name
            );
        }

        if (lower.startsWith("references")) {
            String name = tail(
                    command,
                    "references"
            );

            if (name.isBlank()) {
                return W128WebBuildResult.fail(
                        "Usage: references <symbol>"
                );
            }

            List<W131SymbolIndex.Symbol> references =
                    W131SymbolIndex.references(
                            project,
                            name,
                            256
                    );

            sendSymbols(
                    player,
                    true,
                    "References: " + name,
                    references
            );

            return W128WebBuildResult.ok(
                    references.size()
                            + " reference(s) found for "
                            + name
            );
        }

        if (lower.startsWith("complete")) {
            String prefix = tail(
                    command,
                    "complete"
            );

            List<W131SymbolIndex.Symbol> completions =
                    W131SymbolIndex.completions(
                            project,
                            prefix,
                            64
                    );

            sendSymbols(
                    player,
                    true,
                    "Completions: " + prefix,
                    completions
            );

            return W128WebBuildResult.ok(
                    completions.size()
                            + " completion(s)"
            );
        }

        if (lower.startsWith("open")) {
            String argument = tail(
                    command,
                    "open"
            );

            if (argument.isBlank()) {
                return W128WebBuildResult.fail(
                        "Usage: open <path>[:line]"
                );
            }

            int line = 1;
            String rawPath = argument;
            int colon = argument.lastIndexOf(':');

            if (colon > 0) {
                String possibleLine =
                        argument.substring(
                                colon + 1
                        ).trim();

                try {
                    line = Integer.parseInt(
                            possibleLine
                    );
                    rawPath = argument.substring(
                            0,
                            colon
                    ).trim();
                } catch (NumberFormatException ignored) {
                }
            }

            String path = resolveProjectPath(
                    project,
                    rawPath
            );

            if (project.file(path) == null) {
                return W128WebBuildResult.fail(
                        "File not found: " + path
                );
            }

            sendNavigate(
                    player,
                    new W131SymbolIndex.Symbol(
                            "FILE",
                            W131WorkspaceFs.baseName(
                                    path
                            ),
                            path,
                            Math.max(1, line),
                            1,
                            ""
                    )
            );

            return W128WebBuildResult.ok(
                    "Open requested: "
                            + path
                            + ":"
                            + Math.max(1, line)
            );
        }

        return W128WebBuildResult.fail(
                "Unknown W1.31 command: "
                        + command
                        + " | use w131 help"
        );
    }

    public static void sendInitialState(
            ServerPlayer player,
            W128WebProject project
    ) {
        sendWorkspaceState(
                player,
                project
        );
    }

    public static void sendWorkspaceState(
            ServerPlayer player,
            W128WebProject project
    ) {
        StringBuilder payload = new StringBuilder();

        payload.append("CWD|")
                .append(
                        W131WorkspaceState.cwd(
                                project
                        )
                );

        for (String folder :
                W131WorkspaceState.folders(
                        project
                ).stream().sorted().toList()) {
            payload.append('\n')
                    .append("FOLDER|")
                    .append(folder);
        }

        for (String path :
                W131WorkspaceState.recentFiles(
                        project
                )) {
            payload.append('\n')
                    .append("RECENT|")
                    .append(path);
        }

        sendEvent(
                player,
                "W131_WORKSPACE",
                true,
                "W1.31 Workspace",
                payload.toString()
        );
    }

    public static String help() {
        return """
                W1.31 project-intelligence commands:
                  w131 selftest
                  w131 index
                  w131 diagnostics
                  w131 outline [path]
                  w131 symbols [query]
                  w131 definition <symbol>
                  w131 references <symbol>
                  w131 complete [prefix]
                  w131 recent
                  w131 open <path>[:line]

                W1.31 workspace shell:
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

                Existing W1.30 debug/task/source-control commands remain supported.
                """;
    }

    private static boolean isDirectW131Command(
            String command
    ) {
        if (command == null || command.isBlank()) {
            return false;
        }

        List<String> tokens =
                W131WorkspaceFs.tokenize(command);

        if (tokens.isEmpty()) {
            return false;
        }

        String verb = tokens.get(0)
                .toLowerCase(Locale.ROOT);

        if (W131WorkspaceFs.handlesTerminalVerb(
                verb
        )) {
            return true;
        }

        if (DIRECT_VERBS.contains(verb)) {
            return true;
        }

        String lower =
                command.toLowerCase(Locale.ROOT);

        return lower.startsWith("workspace index")
                || lower.startsWith("workspace diagnostics")
                || lower.startsWith("workspace outline")
                || lower.startsWith("workspace symbols")
                || lower.startsWith("workspace recent");
    }

    private static W128WebBuildResult diagnostics(
            W128WebProject project,
            ServerPlayer player
    ) {
        StringBuilder payload =
                new StringBuilder();

        int diagnosticCount = 0;
        int errorCount = 0;

        for (W128WebFile file :
                project.files()
                        .values()
                        .stream()
                        .filter(value ->
                                !value.generated())
                        .filter(value ->
                                !W130Workspace.internalPath(
                                        value.path()
                                ))
                        .sorted(
                                Comparator.comparing(
                                        W128WebFile::path
                                )
                        )
                        .toList()) {
            List<W129Diagnostic> diagnostics =
                    W129ComputeEngine.validate(
                            file.path(),
                            file.content()
                    );

            for (W129Diagnostic diagnostic :
                    diagnostics) {
                if (diagnosticCount >= 256) {
                    break;
                }

                if (!payload.isEmpty()) {
                    payload.append('\n');
                }

                payload.append(
                                diagnostic.severity()
                                        .name()
                        )
                        .append('|')
                        .append(
                                file.path()
                                        .replace('|', '/')
                        )
                        .append('|')
                        .append(
                                diagnostic.line()
                        )
                        .append('|')
                        .append(
                                diagnostic.column()
                        )
                        .append('|')
                        .append(
                                diagnostic.message()
                                        .replace('|', '/')
                                        .replace('\n', ' ')
                                        .replace('\r', ' ')
                        );

                diagnosticCount++;

                if (diagnostic.severity()
                        == W129Diagnostic.Severity.ERROR) {
                    errorCount++;
                }
            }

            if (diagnosticCount >= 256) {
                break;
            }
        }

        boolean success = errorCount == 0;

        sendEvent(
                player,
                "W130_PROBLEMS",
                success,
                "Workspace Diagnostics",
                payload.toString()
        );

        sendEvent(
                player,
                "OUTPUT",
                success,
                "W1.31 Diagnostics",
                diagnosticCount
                        + " diagnostic(s), "
                        + errorCount
                        + " error(s)"
        );

        return success
                ? W128WebBuildResult.ok(
                "Workspace diagnostics passed: "
                        + diagnosticCount
                        + " diagnostic(s)"
        )
                : W128WebBuildResult.fail(
                "Workspace diagnostics found "
                        + errorCount
                        + " error(s)"
        );
    }

    private static void sendSymbols(
            ServerPlayer player,
            boolean success,
            String title,
            List<W131SymbolIndex.Symbol> symbols
    ) {
        sendEvent(
                player,
                "W131_SYMBOLS",
                success,
                title,
                W131SymbolIndex.toWire(
                        symbols
                )
        );
    }

    private static void sendNavigate(
            ServerPlayer player,
            W131SymbolIndex.Symbol symbol
    ) {
        String payload =
                symbol.path()
                        .replace('|', '/')
                        + "|"
                        + symbol.line()
                        + "|"
                        + symbol.column();

        sendEvent(
                player,
                "W131_NAVIGATE",
                true,
                "Navigate",
                payload
        );
    }

    private static String resolveProjectPath(
            W128WebProject project,
            String rawPath
    ) {
        return W131WorkspaceFs.resolvePath(
                W131WorkspaceState.cwd(
                        project
                ),
                rawPath
        );
    }

    private static String tail(
            String command,
            String prefix
    ) {
        String value = command == null
                ? ""
                : command.trim();

        if (value.length() <= prefix.length()) {
            return "";
        }

        return value.substring(
                prefix.length()
        ).trim();
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
                new W129IdeEventPacket(
                        kind,
                        success,
                        title,
                        payload
                )
        );
    }
}
