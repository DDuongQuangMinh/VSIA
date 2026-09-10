package com.k1ngtle.vsia.signality.internet.web;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class W128WebUnitTestSuite {
    private W128WebUnitTestSuite() {
    }

    public static List<W128WebTestResult> runAll() {
        List<W128WebTestResult> results = new ArrayList<>();

        run(results, "w128-path-root", () -> require(
                "/index.html".equals(W128WebRegistrySavedData.normalizePath("/")),
                "root did not normalize to /index.html"
        ));

        run(results, "w128-path-traversal", () -> {
            boolean rejected = false;
            try {
                W128WebRegistrySavedData.normalizePath("/../../secret.txt");
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "path traversal was not rejected");
        });

        run(results, "w128-mime", () -> {
            require(W128MimeTypes.forPath("/a.css").startsWith("text/css"), "css MIME mismatch");
            require(W128MimeTypes.forPath("/a.js").startsWith("text/javascript"), "js MIME mismatch");
            require(W128MimeTypes.forPath("/a.html").startsWith("text/html"), "html MIME mismatch");
        });

        run(results, "w128-ide-languages", () -> {
            require(
                    W128IdeLanguage.detect("/main.py") == W128IdeLanguage.PYTHON,
                    "python detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/main.c") == W128IdeLanguage.C,
                    "C detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/Program.cs") == W128IdeLanguage.CSHARP,
                    "C# detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/main.cpp") == W128IdeLanguage.CPP,
                    "C++ detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/boot.asm") == W128IdeLanguage.ASSEMBLY,
                    "assembly detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/Main.java") == W128IdeLanguage.JAVA,
                    "Java detection mismatch"
            );
            require(
                    W128IdeLanguage.detect("/src/App.jsx") == W128IdeLanguage.JSX,
                    "JSX detection mismatch"
            );
        });

        run(results, "w128-source-mime", () -> {
            require(
                    W128MimeTypes.forPath("/main.py").startsWith("text/x-python"),
                    "python MIME mismatch"
            );
            require(
                    W128MimeTypes.forPath("/main.c").startsWith("text/x-c"),
                    "C MIME mismatch"
            );
            require(
                    W128MimeTypes.forPath("/Program.cs").startsWith("text/x-csharp"),
                    "C# MIME mismatch"
            );
            require(
                    W128MimeTypes.forPath("/main.cpp").startsWith("text/x-c++"),
                    "C++ MIME mismatch"
            );
            require(
                    W128MimeTypes.forPath("/boot.asm").startsWith("text/x-asm"),
                    "assembly MIME mismatch"
            );
            require(
                    W128MimeTypes.forPath("/Main.java").startsWith("text/x-java-source"),
                    "Java MIME mismatch"
            );
        });

        run(results, "w128-jsx-basic", () -> {
            String output = W128JsxCompiler.compile(
                    "function App(){return <main><h1>Hello</h1><p>World</p></main>;}"
            );
            require(output.contains("React.createElement(\"main\""), "main element missing");
            require(output.contains("React.createElement(\"h1\""), "h1 element missing");
        });

        run(results, "w128-jsx-expression", () -> {
            String output = W128JsxCompiler.compile(
                    "function App(){const n=7;return <div data-n={n}>Count: {n}</div>;}"
            );
            require(output.contains("\"data-n\":(n)"), "expression attribute missing");
            require(output.contains("(n)"), "expression child missing");
        });

        run(results, "w128-jsx-event", () -> {
            String output = W128JsxCompiler.compile(
                    "function App(){return <button onClick={() => ping()}>Go</button>;}"
            );
            require(output.contains("\"onClick\":(() => ping())"), "event expression missing");
        });

        run(results, "w128-jsx-fragment", () -> {
            String output = W128JsxCompiler.compile(
                    "function App(){return <><span>A</span><span>B</span></>;}"
            );
            require(output.contains("React.Fragment"), "fragment missing");
        });

        run(results, "w128-jsx-mismatch", () -> {
            boolean rejected = false;
            try {
                W128JsxCompiler.compile("function App(){return <div></span>;}");
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            require(rejected, "mismatched tag was not rejected");
        });

        run(results, "w128-react-runtime", () -> {
            String runtime = W128ReactRuntime.source();
            require(runtime.contains("useState"), "useState missing");
            require(runtime.contains("useEffect"), "useEffect missing");
            require(runtime.contains("createRoot"), "createRoot missing");
        });

        run(results, "w128-http-wire", () -> {
            W128HttpResponse response = new W128HttpResponse(
                    200,
                    "OK",
                    "text/html; charset=utf-8",
                    "<h1>ok</h1>",
                    Map.of("ETag", "\"abc\"")
            );
            String wire = new String(W128WebHostService.responseWire(response), StandardCharsets.UTF_8);
            require(wire.startsWith("HTTP/1.1 200 OK\r\n"), "HTTP status line mismatch");
            require(wire.contains("Content-Length: 11\r\n"), "HTTP Content-Length mismatch");
            require(wire.endsWith("<h1>ok</h1>"), "HTTP body mismatch");
        });

        return List.copyOf(results);
    }

    private static void run(
            List<W128WebTestResult> results,
            String id,
            Runnable body
    ) {
        try {
            body.run();
            results.add(new W128WebTestResult(id, true, "PASS"));
        } catch (Throwable throwable) {
            results.add(new W128WebTestResult(
                    id,
                    false,
                    throwable.getClass().getSimpleName() + ": " + String.valueOf(throwable.getMessage())
            ));
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
