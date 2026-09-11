package com.k1ngtle.vsia.signality.internet.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class W130SelfTest {
    private W130SelfTest() {
    }

    public static String run() {
        StringBuilder out = new StringBuilder("[W1.30 self-test]\n");
        int[] counts = new int[2];

        test(out, counts, "w130-hidden-workspace", () ->
                W130Workspace.internalPath("/.vsia/settings.json")
                        && !W130Workspace.internalPath("/index.html"));

        test(out, counts, "w130-launch-config", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    W130Workspace.LAUNCH_PATH,
                    "{\"configurations\":[{\"name\":\"Debug current\",\"program\":\"${file}\",\"stopOnEntry\":true}]}"
            ));
            W130Workspace.LaunchConfig config = W130Workspace.launchConfig(project, "/tools/test.py");
            return "Debug current".equals(config.name())
                    && "/tools/test.py".equals(config.program())
                    && config.stopOnEntry();
        });

        test(out, counts, "w130-task-graph", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    W130Workspace.TASKS_PATH,
                    "{\"tasks\":["
                            + "{\"label\":\"check\",\"command\":\"check ${file}\",\"dependsOn\":[]},"
                            + "{\"label\":\"build\",\"command\":\"build\",\"dependsOn\":[\"check\"]}]}"
            ));
            W130TaskEngine.ResolvedTask resolved = W130TaskEngine.resolve(project, "build", "/tools/test.py");
            return resolved.order().equals(List.of("check", "build"))
                    && resolved.commands().equals(List.of("check /tools/test.py", "build"));
        });

        test(out, counts, "w130-task-cycle", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    W130Workspace.TASKS_PATH,
                    "{\"tasks\":["
                            + "{\"label\":\"a\",\"command\":\"build\",\"dependsOn\":[\"b\"]},"
                            + "{\"label\":\"b\",\"command\":\"build\",\"dependsOn\":[\"a\"]}]}"
            ));
            try {
                W130TaskEngine.resolve(project, "a", "/x.py");
                return false;
            } catch (IllegalArgumentException expected) {
                return expected.getMessage().contains("cycle");
            }
        });

        test(out, counts, "w130-python-debug-line", () ->
                W130Debugger.firstExecutableLine(
                        W128IdeLanguage.PYTHON,
                        "def main():\n    count = 1\n    print(count)\n"
                ) == 2);

        test(out, counts, "w130-c-debug-line", () ->
                W130Debugger.firstExecutableLine(
                        W128IdeLanguage.C,
                        "#include <stdio.h>\nint main(void) {\nint count = 1;\nprintf(\"x\");\n}\n"
                ) == 3);

        test(out, counts, "w130-jsx-debug-line", () ->
                W130Debugger.firstExecutableLine(
                        W128IdeLanguage.JSX,
                        "function App() {\n  const [count, setCount] = React.useState(0);\n  return <main>{count}</main>;\n}\n"
                ) == 2);

        test(out, counts, "w130-python-assignment", () -> {
            Map<String, Object> variables = new LinkedHashMap<>();
            W130Debugger.applyLine(W128IdeLanguage.PYTHON, "count = 2 + 3", variables);
            return "5".equals(W129Expression.printable(variables.get("count")));
        });

        test(out, counts, "w130-c-assignment", () -> {
            Map<String, Object> variables = new LinkedHashMap<>();
            W130Debugger.applyLine(W128IdeLanguage.C, "int count = 7;", variables);
            return "7".equals(W129Expression.printable(variables.get("count")));
        });

        test(out, counts, "w130-assembly-register", () -> {
            Map<String, Object> variables = new LinkedHashMap<>();
            W130Debugger.applyLine(W128IdeLanguage.ASSEMBLY, "MOV RAX, 4", variables);
            W130Debugger.applyLine(W128IdeLanguage.ASSEMBLY, "ADD RAX, 3", variables);
            return "7".equals(W129Expression.printable(variables.get("RAX")));
        });

        test(out, counts, "w130-debug-wire", () -> {
            W130DebugSnapshot snapshot = new W130DebugSnapshot(
                    "PAUSED",
                    "/test.py",
                    3,
                    "breakpoint",
                    List.of("main"),
                    Map.of("x", "4"),
                    Map.of("x+1", "5"),
                    Map.of("/test.py", List.of(3))
            );
            W130DebugSnapshot decoded = W130DebugSnapshot.fromJson(snapshot.toJson());
            return decoded.line() == 3
                    && "4".equals(decoded.variables().get("x"));
        });

        test(out, counts, "w130-scm-wire", () -> {
            W130ScmSnapshot snapshot = new W130ScmSnapshot(
                    2,
                    List.of(new W130ScmSnapshot.Change("M", "/a.txt", true)),
                    List.of(new W130ScmSnapshot.CommitInfo(2, "test", 10L))
            );
            W130ScmSnapshot decoded = W130ScmSnapshot.fromJson(snapshot.toJson());
            return decoded.head() == 2
                    && decoded.stagedCount() == 1
                    && "test".equals(decoded.history().get(0).message());
        });

        test(out, counts, "w130-expression-watch", () -> {
            Object value = W129Expression.evaluate("a * 2 + 1", Map.of("a", 4.0D));
            return "9".equals(W129Expression.printable(value));
        });

        out.append("W1.30 self-test result: ")
                .append(counts[0])
                .append(" passed, ")
                .append(counts[1])
                .append(" failed");
        return out.toString();
    }

    private static W128WebProject project() {
        return new W128WebProject(
                "test.w130.local",
                "w130.local",
                UUID.fromString("00000000-0000-0000-0000-000000000130"),
                "W130",
                W128WebMode.STATIC,
                1L
        );
    }

    private static W128WebFile file(String path, String content) {
        return new W128WebFile(path, content, W128MimeTypes.forPath(path), 1L, false);
    }

    private static void test(
            StringBuilder out,
            int[] counts,
            String name,
            Check check
    ) {
        boolean passed;
        try {
            passed = check.run();
        } catch (Throwable throwable) {
            passed = false;
        }

        if (passed) {
            counts[0]++;
            out.append("[PASS] ").append(name).append('\n');
        } else {
            counts[1]++;
            out.append("[FAIL] ").append(name).append('\n');
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean run();
    }
}
