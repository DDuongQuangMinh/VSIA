package com.k1ngtle.vsia.signality.internet.web;

import java.util.List;

public final class W129ComputeEngine {
    private W129ComputeEngine() {
    }

    public static W129RunResult run(
            String path,
            String source
    ) {
        W128IdeLanguage language =
                W128IdeLanguage.detect(path);

        if (language == W128IdeLanguage.ASSEMBLY) {
            return W129AssemblyVm.run(source);
        }

        if (
                language == W128IdeLanguage.PYTHON
                        || language == W128IdeLanguage.C
                        || language == W128IdeLanguage.CPP
                        || language == W128IdeLanguage.CSHARP
                        || language == W128IdeLanguage.JAVA
        ) {
            return W129ConsoleSubsetRuntime.run(
                    language,
                    source
            );
        }

        return W129RunResult.fail(
                "This file is not executable by the W1.29 compute runtime.",
                List.of(
                        new W129Diagnostic(
                                W129Diagnostic.Severity.INFO,
                                1,
                                1,
                                "HTML/CSS/JavaScript/JSX use the W1.28 web runtime. Use Build + Publish for website execution."
                        )
                )
        );
    }

    public static List<W129Diagnostic> validate(
            String path,
            String source
    ) {
        return W129Diagnostics.validate(
                W128IdeLanguage.detect(path),
                source
        );
    }

    public static String runtimeName(
            String path
    ) {
        return switch (
                W128IdeLanguage.detect(path)
        ) {
            case ASSEMBLY ->
                    "VSIA Virtual CPU";
            case PYTHON ->
                    "VSIA Python subset";
            case C ->
                    "VSIA C console subset";
            case CPP ->
                    "VSIA C++ console subset";
            case CSHARP ->
                    "VSIA C# console subset";
            case JAVA ->
                    "VSIA Java console subset";
            default ->
                    "W1.28 Web Runtime";
        };
    }

    public static boolean executable(
            String path
    ) {
        return switch (
                W128IdeLanguage.detect(path)
        ) {
            case ASSEMBLY,
                 PYTHON,
                 C,
                 CPP,
                 CSHARP,
                 JAVA -> true;
            default -> false;
        };
    }
}
