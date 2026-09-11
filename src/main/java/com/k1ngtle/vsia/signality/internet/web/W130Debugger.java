package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class W130Debugger {
    private static final int MAX_STEPS = 10_000;
    private static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>();

    private W130Debugger() {
    }

    public static DebugResult start(
            UUID playerUuid,
            W128WebProject project,
            String activePath
    ) {
        W130Workspace.LaunchConfig config = W130Workspace.launchConfig(project, activePath);
        String path = config.program();
        W128WebFile file = project.file(path);

        if (file == null) {
            return fail("Debug program not found: " + path, project);
        }
        if (!debuggable(path)) {
            return fail(
                    "W1.30 debugger supports JavaScript/JSX and W1.29 executable languages: " + path,
                    project
            );
        }

        Session session = new Session(
                playerUuid,
                project.host(),
                path,
                file.content(),
                W128IdeLanguage.detect(path),
                W130Workspace.breakpoints(project),
                W130Workspace.watches(project)
        );
        seedLaunchVariables(session, config);

        if (session.executableLines.isEmpty()) {
            return fail("No executable statements were found in " + path, project);
        }

        SESSIONS.put(key(playerUuid, project.host()), session);
        session.state = "PAUSED";
        session.reason = config.stopOnEntry() ? "entry" : "start";

        if (!config.stopOnEntry()) {
            continueInternal(session);
        }

        return ok("Debug session started: " + path, session, project);
    }

    public static DebugResult continueRun(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session session = session(playerUuid, project.host());
        if (session == null) {
            return fail("No active debug session.", project);
        }
        if (session.terminated()) {
            return fail("Debug session is terminated. Restart it first.", project);
        }

        continueInternal(session);
        return ok(
                session.terminated() ? "Program terminated." : "Paused at breakpoint.",
                session,
                project
        );
    }

    public static DebugResult pause(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session session = session(playerUuid, project.host());
        if (session == null || session.terminated()) {
            return fail("No running debug session.", project);
        }

        session.state = "PAUSED";
        session.reason = "pause";
        return ok("Debug session paused.", session, project);
    }

    public static DebugResult stepOver(
            UUID playerUuid,
            W128WebProject project
    ) {
        return step(playerUuid, project, "step over");
    }

    public static DebugResult stepInto(
            UUID playerUuid,
            W128WebProject project
    ) {
        return step(playerUuid, project, "step into");
    }

    public static DebugResult stepOut(
            UUID playerUuid,
            W128WebProject project
    ) {
        return step(playerUuid, project, "step out");
    }

    public static DebugResult restart(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session old = session(playerUuid, project.host());
        String path = old == null ? "" : old.path;
        if (path.isBlank()) {
            return fail("No debug session to restart.", project);
        }

        W128WebFile file = project.file(path);
        if (file == null) {
            return fail("Debug program no longer exists: " + path, project);
        }

        Session replacement = new Session(
                playerUuid,
                project.host(),
                path,
                file.content(),
                W128IdeLanguage.detect(path),
                W130Workspace.breakpoints(project),
                W130Workspace.watches(project)
        );
        replacement.state = "PAUSED";
        replacement.reason = "restart";
        SESSIONS.put(key(playerUuid, project.host()), replacement);
        return ok("Debug session restarted.", replacement, project);
    }

    public static DebugResult stop(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session session = SESSIONS.remove(key(playerUuid, project.host()));
        if (session == null) {
            return new DebugResult(
                    true,
                    "No debug session was active.",
                    idleSnapshot(project),
                    false
            );
        }

        session.state = "TERMINATED";
        session.reason = "stopped";
        return new DebugResult(
                true,
                "Debug session stopped.",
                idleSnapshot(project),
                true
        );
    }

    public static W130DebugSnapshot snapshot(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session session = session(playerUuid, project.host());
        return session == null
                ? idleSnapshot(project)
                : snapshot(session, project);
    }

    public static boolean hasSession(UUID playerUuid, String host) {
        Session session = session(playerUuid, host);
        return session != null && !session.terminated();
    }

    public static boolean debuggable(String path) {
        W128IdeLanguage language = W128IdeLanguage.detect(path);
        return W129ComputeEngine.executable(path)
                || language == W128IdeLanguage.JAVASCRIPT
                || language == W128IdeLanguage.JSX;
    }

    public static void refreshWorkspaceState(
            UUID playerUuid,
            W128WebProject project
    ) {
        Session session = session(playerUuid, project.host());
        if (session == null) {
            return;
        }
        session.breakpoints = W130Workspace.breakpoints(project);
        session.watches = W130Workspace.watches(project);
    }

    private static DebugResult step(
            UUID playerUuid,
            W128WebProject project,
            String reason
    ) {
        Session session = session(playerUuid, project.host());
        if (session == null || session.terminated()) {
            return fail("No active debug session.", project);
        }

        executeCurrent(session);
        session.pointer++;

        if (session.pointer >= session.executableLines.size()) {
            session.state = "TERMINATED";
            session.reason = "completed";
            return ok("Program terminated.", session, project);
        }

        session.state = "PAUSED";
        session.reason = reason;
        return ok("Paused after " + reason + ".", session, project);
    }

    private static void continueInternal(Session session) {
        session.state = "RUNNING";
        session.reason = "continue";
        int steps = 0;

        if (session.pointer < session.executableLines.size()) {
            executeCurrent(session);
            session.pointer++;
        }

        while (session.pointer < session.executableLines.size() && steps++ < MAX_STEPS) {
            int line = session.currentLine();
            if (session.breakpoints.getOrDefault(session.path, List.of()).contains(line)) {
                session.state = "PAUSED";
                session.reason = "breakpoint";
                return;
            }

            executeCurrent(session);
            session.pointer++;
        }

        if (steps >= MAX_STEPS) {
            session.state = "PAUSED";
            session.reason = "step limit";
            return;
        }

        session.state = "TERMINATED";
        session.reason = "completed";
    }

    private static void executeCurrent(Session session) {
        if (session.pointer < 0 || session.pointer >= session.executableLines.size()) {
            return;
        }

        int lineNumber = session.executableLines.get(session.pointer);
        String raw = session.lines.get(lineNumber - 1);
        applyLine(session.language, raw, session.variables);
    }

    private static void seedLaunchVariables(
            Session session,
            W130Workspace.LaunchConfig config
    ) {
        session.variables.put("argc", (double) config.args().size());
        session.variables.put("cwd", config.cwd());

        for (int i = 0; i < config.args().size(); i++) {
            session.variables.put("arg" + i, config.args().get(i));
        }

        config.environment().forEach((name, value) -> {
            if (name != null && name.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
                session.variables.put(name, value);
            }
        });
    }

    static void applyLine(
            W128IdeLanguage language,
            String rawLine,
            Map<String, Object> variables
    ) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isEmpty()) {
            return;
        }

        if (language == W128IdeLanguage.ASSEMBLY) {
            applyAssembly(line, variables);
            return;
        }

        String normalized = line;
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }

        if (language == W128IdeLanguage.JAVASCRIPT || language == W128IdeLanguage.JSX) {
            java.util.regex.Matcher state = java.util.regex.Pattern.compile(
                    "(?:const|let|var)\\s*\\[\\s*([A-Za-z_$][A-Za-z0-9_$]*)[^]]*]\\s*=\\s*React\\.useState\\((.*)\\)"
            ).matcher(normalized);
            if (state.matches()) {
                try {
                    variables.put(state.group(1), W129Expression.evaluate(state.group(2), variables));
                } catch (RuntimeException ignored) {
                }
                return;
            }
        }

        int equals = assignmentIndex(normalized);
        if (equals < 1) {
            return;
        }

        String left = normalized.substring(0, equals).trim();
        String expression = normalized.substring(equals + 1).trim();
        if (expression.isEmpty()) {
            return;
        }

        String[] tokens = left.split("\\s+");
        String name = tokens[tokens.length - 1]
                .replace("*", "")
                .replace("&", "")
                .trim();

        if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            return;
        }

        try {
            variables.put(name, W129Expression.evaluate(expression, variables));
        } catch (RuntimeException ignored) {
        }
    }

    static int firstExecutableLine(W128IdeLanguage language, String source) {
        List<Integer> values = executableLines(language, source);
        return values.isEmpty() ? 0 : values.get(0);
    }

    static List<Integer> executableLines(W128IdeLanguage language, String source) {
        String normalized = source == null ? "" : source.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < rawLines.length; i++) {
            String line = rawLines[i].trim();
            if (line.isEmpty() || line.equals("{") || line.equals("}")) {
                continue;
            }
            if (line.startsWith("//") || line.startsWith("# ") || line.startsWith("/*") || line.startsWith("*")) {
                continue;
            }
            if (language == W128IdeLanguage.PYTHON
                    && (line.startsWith("def ") || line.startsWith("class ") || line.startsWith("import ") || line.startsWith("from "))) {
                continue;
            }
            if (language == W128IdeLanguage.ASSEMBLY
                    && (line.endsWith(":")
                    || line.toLowerCase(Locale.ROOT).startsWith("section ")
                    || line.toLowerCase(Locale.ROOT).startsWith("global "))) {
                continue;
            }
            if ((language == W128IdeLanguage.JAVASCRIPT || language == W128IdeLanguage.JSX)
                    && (line.startsWith("import ")
                    || line.startsWith("export ")
                    || line.startsWith("function ")
                    || line.startsWith("class ")
                    || line.equals("</>")
                    || line.equals("(")
                    || line.equals(")"))) {
                continue;
            }
            if ((language == W128IdeLanguage.C
                    || language == W128IdeLanguage.CPP
                    || language == W128IdeLanguage.CSHARP
                    || language == W128IdeLanguage.JAVA)
                    && (line.startsWith("#include")
                    || line.startsWith("using ")
                    || line.startsWith("import ")
                    || line.startsWith("package ")
                    || line.startsWith("public class ")
                    || line.startsWith("class ")
                    || line.matches(".*\\)\\s*\\{?"))) {
                continue;
            }
            result.add(i + 1);
        }

        return List.copyOf(result);
    }

    private static int assignmentIndex(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '=') {
                continue;
            }
            char before = i > 0 ? line.charAt(i - 1) : '\0';
            char after = i + 1 < line.length() ? line.charAt(i + 1) : '\0';
            if (before != '=' && before != '!' && before != '<' && before != '>' && after != '=') {
                return i;
            }
        }
        return -1;
    }

    private static void applyAssembly(String line, Map<String, Object> variables) {
        String clean = line.split(";", 2)[0].trim();
        if (clean.isEmpty()) {
            return;
        }

        String[] parts = clean.split("\\s+", 2);
        String opcode = parts[0].toUpperCase(Locale.ROOT);
        String arguments = parts.length > 1 ? parts[1].trim() : "";
        String[] operands = arguments.split(",", 2);

        try {
            switch (opcode) {
                case "MOV" -> {
                    if (operands.length == 2) {
                        variables.put(
                                operands[0].trim().toUpperCase(Locale.ROOT),
                                W129Expression.evaluate(operands[1].trim(), variables)
                        );
                    }
                }
                case "ADD", "SUB" -> {
                    if (operands.length == 2) {
                        String register = operands[0].trim().toUpperCase(Locale.ROOT);
                        Object left = variables.getOrDefault(register, 0.0D);
                        Object right = W129Expression.evaluate(operands[1].trim(), variables);
                        double a = left instanceof Number n ? n.doubleValue() : 0.0D;
                        double b = right instanceof Number n ? n.doubleValue() : 0.0D;
                        variables.put(register, "ADD".equals(opcode) ? a + b : a - b);
                    }
                }
                case "INC", "DEC" -> {
                    String register = arguments.toUpperCase(Locale.ROOT);
                    Object old = variables.getOrDefault(register, 0.0D);
                    double value = old instanceof Number n ? n.doubleValue() : 0.0D;
                    variables.put(register, "INC".equals(opcode) ? value + 1.0D : value - 1.0D);
                }
                default -> {
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static W130DebugSnapshot snapshot(Session session, W128WebProject project) {
        Map<String, String> variables = new LinkedHashMap<>();
        session.variables.forEach((name, value) -> variables.put(name, W129Expression.printable(value)));

        Map<String, String> watches = new LinkedHashMap<>();
        for (String expression : W130Workspace.watches(project)) {
            try {
                watches.put(
                        expression,
                        W129Expression.printable(W129Expression.evaluate(expression, session.variables))
                );
            } catch (RuntimeException exception) {
                watches.put(expression, "<" + exception.getMessage() + ">");
            }
        }

        int line = session.terminated() ? 0 : session.currentLine();
        List<String> stack = line > 0
                ? List.of("main (" + session.path + ":" + line + ")")
                : List.of();

        return new W130DebugSnapshot(
                session.state,
                session.path,
                line,
                session.reason,
                stack,
                variables,
                watches,
                W130Workspace.breakpoints(project)
        );
    }

    private static W130DebugSnapshot idleSnapshot(W128WebProject project) {
        return new W130DebugSnapshot(
                "IDLE",
                "",
                0,
                "",
                List.of(),
                Map.of(),
                Map.of(),
                W130Workspace.breakpoints(project)
        );
    }

    private static DebugResult ok(String message, Session session, W128WebProject project) {
        return new DebugResult(true, message, snapshot(session, project), session.terminated());
    }

    private static DebugResult fail(String message, W128WebProject project) {
        return new DebugResult(false, message, idleSnapshot(project), false);
    }

    private static Session session(UUID playerUuid, String host) {
        return SESSIONS.get(key(playerUuid, host));
    }

    private static String key(UUID playerUuid, String host) {
        return playerUuid + "|" + (host == null ? "" : host.toLowerCase(Locale.ROOT));
    }

    public record DebugResult(
            boolean success,
            String message,
            W130DebugSnapshot snapshot,
            boolean terminated
    ) {
    }

    private static final class Session {
        private final UUID playerUuid;
        private final String host;
        private final String path;
        private final W128IdeLanguage language;
        private final List<String> lines;
        private final List<Integer> executableLines;
        private Map<String, List<Integer>> breakpoints;
        private List<String> watches;
        private final Map<String, Object> variables = new LinkedHashMap<>();

        private int pointer;
        private String state = "PAUSED";
        private String reason = "entry";

        private Session(
                UUID playerUuid,
                String host,
                String path,
                String source,
                W128IdeLanguage language,
                Map<String, List<Integer>> breakpoints,
                List<String> watches
        ) {
            this.playerUuid = playerUuid;
            this.host = host;
            this.path = path;
            this.language = language;
            this.lines = List.of((source == null ? "" : source)
                    .replace("\r\n", "\n")
                    .replace('\r', '\n')
                    .split("\n", -1));
            this.executableLines = executableLines(language, source);
            this.breakpoints = breakpoints == null ? Map.of() : breakpoints;
            this.watches = watches == null ? List.of() : List.copyOf(watches);
        }

        private int currentLine() {
            if (pointer < 0 || pointer >= executableLines.size()) {
                return 0;
            }
            return executableLines.get(pointer);
        }

        private boolean terminated() {
            return "TERMINATED".equals(state);
        }
    }
}
