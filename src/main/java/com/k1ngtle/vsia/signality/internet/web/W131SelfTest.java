package com.k1ngtle.vsia.signality.internet.web;

import java.util.List;
import java.util.UUID;

public final class W131SelfTest {
    private W131SelfTest() {
    }

    public static String run() {
        StringBuilder out = new StringBuilder("[W1.31 self-test]\n");
        int[] counts = new int[2];

        test(out, counts, "w131-w130-regression", () ->
                !W130SelfTest.run().contains("[FAIL]"));

        test(out, counts, "w131-path-relative", () ->
                "/tools/test.py".equals(
                        W131WorkspaceFs.resolvePath(
                                "/src",
                                "../tools/test.py"
                        )
                ));

        test(out, counts, "w131-path-root-clamp", () ->
                "/x.txt".equals(
                        W131WorkspaceFs.resolvePath(
                                "/",
                                "../../x.txt"
                        )
                ));

        test(out, counts, "w131-shell-tokenizer", () ->
                W131WorkspaceFs.tokenize(
                        "cp \"a b.txt\" '/dst/c d.txt'"
                ).equals(
                        List.of(
                                "cp",
                                "a b.txt",
                                "/dst/c d.txt"
                        )
                ));

        test(out, counts, "w131-derived-folder", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/src/app/main.py",
                    "print('x')"
            ));

            return W131WorkspaceFs.directoryExists(
                    project,
                    "/src/app"
            );
        });

        test(out, counts, "w131-ls", () -> {
            W128WebProject project = project();
            project.putFile(file("/src/a.py", ""));
            project.putFile(file("/src/lib/b.py", ""));

            return W131WorkspaceFs.listEntries(
                    project,
                    "/src"
            ).equals(
                    List.of("a.py", "lib/")
            );
        });

        test(out, counts, "w131-python-symbols", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/tools/a.py",
                    "class Worker:\n"
                            + "    pass\n\n"
                            + "def run():\n"
                            + "    value = 3\n"
            ));

            List<W131SymbolIndex.Symbol> symbols =
                    W131SymbolIndex.outline(
                            project,
                            "/tools/a.py"
                    );

            return symbols.stream().anyMatch(
                    symbol -> "Worker".equals(symbol.name())
                            && "CLASS".equals(symbol.kind())
            )
                    && symbols.stream().anyMatch(
                    symbol -> "run".equals(symbol.name())
                            && "FUNCTION".equals(symbol.kind())
            );
        });

        test(out, counts, "w131-jsx-symbols", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/src/App.jsx",
                    "function App() {\n"
                            + "  const count = 1;\n"
                            + "}\n"
            ));

            List<W131SymbolIndex.Symbol> symbols =
                    W131SymbolIndex.outline(
                            project,
                            "/src/App.jsx"
                    );

            return symbols.stream().anyMatch(
                    symbol -> "App".equals(symbol.name())
                            && "FUNCTION".equals(symbol.kind())
            )
                    && symbols.stream().anyMatch(
                    symbol -> "count".equals(symbol.name())
            );
        });

        test(out, counts, "w131-java-symbols", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/java/Main.java",
                    "public class Main {\n"
                            + "  public static void hello() {\n"
                            + "  }\n"
                            + "}\n"
            ));

            List<W131SymbolIndex.Symbol> symbols =
                    W131SymbolIndex.outline(
                            project,
                            "/java/Main.java"
                    );

            return symbols.stream().anyMatch(
                    symbol -> "Main".equals(symbol.name())
            )
                    && symbols.stream().anyMatch(
                    symbol -> "hello".equals(symbol.name())
            );
        });

        test(out, counts, "w131-c-symbols", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/native/main.c",
                    "int calculate(int x) {\n"
                            + "  return x + 1;\n"
                            + "}\n"
            ));

            return W131SymbolIndex.outline(
                    project,
                    "/native/main.c"
            ).stream().anyMatch(
                    symbol -> "calculate".equals(symbol.name())
            );
        });

        test(out, counts, "w131-assembly-label", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/asm/main.asm",
                    "_start:\n"
                            + "  nop\n"
            ));

            return W131SymbolIndex.outline(
                    project,
                    "/asm/main.asm"
            ).stream().anyMatch(
                    symbol -> "_start".equals(symbol.name())
            );
        });

        test(out, counts, "w131-definition", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/tools/a.py",
                    "def alpha():\n"
                            + "    pass\n"
            ));

            return W131SymbolIndex.definitions(
                    project,
                    "alpha",
                    8
            ).size() == 1;
        });

        test(out, counts, "w131-references", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/tools/a.py",
                    "def alpha():\n"
                            + "    return 1\n"
                            + "value = alpha()\n"
            ));

            return W131SymbolIndex.references(
                    project,
                    "alpha",
                    16
            ).size() == 2;
        });

        test(out, counts, "w131-completion", () -> {
            W128WebProject project = project();
            project.putFile(file(
                    "/tools/a.py",
                    "def network_scan():\n"
                            + "    pass\n"
            ));

            return W131SymbolIndex.completions(
                    project,
                    "net",
                    16
            ).stream().anyMatch(
                    symbol -> "network_scan".equals(symbol.name())
            );
        });

        test(out, counts, "w131-state-folders", () -> {
            W128WebProject project = project();
            W128WebRegistrySavedData data =
                    new W128WebRegistrySavedData();

            W131WorkspaceState.ensureDefaults(
                    data,
                    project,
                    1L
            );

            W131WorkspaceState.addFolder(
                    data,
                    project,
                    "/empty/nested",
                    2L
            );

            return W131WorkspaceState.folders(project)
                    .contains("/empty")
                    && W131WorkspaceState.folders(project)
                    .contains("/empty/nested");
        });

        test(out, counts, "w131-state-recent", () -> {
            W128WebProject project = project();
            W128WebRegistrySavedData data =
                    new W128WebRegistrySavedData();

            project.putFile(file(
                    "/tools/recent.py",
                    ""
            ));

            W131WorkspaceState.ensureDefaults(
                    data,
                    project,
                    1L
            );

            W131WorkspaceState.rememberFile(
                    data,
                    project,
                    "/tools/recent.py",
                    2L
            );

            return W131WorkspaceState.recentFiles(project)
                    .equals(
                            List.of("/tools/recent.py")
                    );
        });

        out.append("W1.31 self-test result: ")
                .append(counts[0])
                .append(" passed, ")
                .append(counts[1])
                .append(" failed");

        return out.toString();
    }

    private static W128WebProject project() {
        return new W128WebProject(
                "test.w131.local",
                "w131.local",
                UUID.fromString(
                        "00000000-0000-0000-0000-000000000131"
                ),
                "W131",
                W128WebMode.STATIC,
                1L
        );
    }

    private static W128WebFile file(
            String path,
            String content
    ) {
        return new W128WebFile(
                path,
                content,
                W128MimeTypes.forPath(path),
                1L,
                false
        );
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
            out.append("[PASS] ")
                    .append(name)
                    .append('\n');
        } else {
            counts[1]++;
            out.append("[FAIL] ")
                    .append(name)
                    .append('\n');
        }
    }

    @FunctionalInterface
    private interface Check {
        boolean run();
    }
}
