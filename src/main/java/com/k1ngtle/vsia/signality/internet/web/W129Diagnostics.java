package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayList;
import java.util.List;

public final class W129Diagnostics {
    private W129Diagnostics() {
    }

    public static List<W129Diagnostic> validate(
            W128IdeLanguage language,
            String source
    ) {
        if (language == W128IdeLanguage.ASSEMBLY) {
            return W129AssemblyVm.validate(source);
        }

        List<W129Diagnostic> diagnostics =
                new ArrayList<>();

        String value =
                source == null
                        ? ""
                        : source
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');

        balance(
                value,
                '(',
                ')',
                "parenthesis",
                diagnostics
        );

        balance(
                value,
                '[',
                ']',
                "bracket",
                diagnostics
        );

        if (language != W128IdeLanguage.PYTHON) {
            balance(
                    value,
                    '{',
                    '}',
                    "brace",
                    diagnostics
            );
        }

        String[] lines =
                value.split("\n", -1);

        for (int i = 0;
             i < lines.length;
             i++) {
            String line = lines[i];

            if (line.length() > 240) {
                diagnostics.add(
                        new W129Diagnostic(
                                W129Diagnostic.Severity.INFO,
                                i + 1,
                                241,
                                "Long line (>240 characters)."
                        )
                );
            }

            if (line.contains("\t")) {
                diagnostics.add(
                        new W129Diagnostic(
                                W129Diagnostic.Severity.INFO,
                                i + 1,
                                1,
                                "Tab character detected; VSIA editor uses four-space indentation."
                        )
                );
            }
        }

        if (language == W128IdeLanguage.PYTHON) {
            validatePython(
                    lines,
                    diagnostics
            );
        }

        return List.copyOf(diagnostics);
    }

    private static void validatePython(
            String[] lines,
            List<W129Diagnostic> diagnostics
    ) {
        for (int i = 0;
             i < lines.length;
             i++) {
            String trimmed =
                    lines[i].trim();

            if (
                    (
                            trimmed.startsWith("def ")
                                    || trimmed.startsWith("if ")
                                    || trimmed.startsWith("elif ")
                                    || trimmed.startsWith("else")
                                    || trimmed.startsWith("for ")
                                    || trimmed.startsWith("while ")
                    )
                            && !trimmed.endsWith(":")
                            && !trimmed.isEmpty()
            ) {
                diagnostics.add(
                        new W129Diagnostic(
                                W129Diagnostic.Severity.WARNING,
                                i + 1,
                                Math.max(
                                        1,
                                        lines[i].length()
                                ),
                                "Python block header normally ends with ':'."
                        )
                );
            }
        }
    }

    private static void balance(
            String source,
            char open,
            char close,
            String name,
            List<W129Diagnostic> diagnostics
    ) {
        int depth = 0;
        boolean single = false;
        boolean dbl = false;
        boolean escape = false;
        int line = 1;
        int column = 0;

        for (int i = 0;
             i < source.length();
             i++) {
            char c = source.charAt(i);

            if (c == '\n') {
                line++;
                column = 0;
                continue;
            }

            column++;

            if (escape) {
                escape = false;
                continue;
            }

            if ((single || dbl)
                    && c == '\\') {
                escape = true;
                continue;
            }

            if (c == '\''
                    && !dbl) {
                single = !single;
                continue;
            }

            if (c == '"'
                    && !single) {
                dbl = !dbl;
                continue;
            }

            if (single || dbl) {
                continue;
            }

            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;

                if (depth < 0) {
                    diagnostics.add(
                            new W129Diagnostic(
                                    W129Diagnostic.Severity.ERROR,
                                    line,
                                    column,
                                    "Unexpected closing "
                                            + name
                                            + "."
                            )
                    );
                    return;
                }
            }
        }

        if (depth > 0) {
            diagnostics.add(
                    new W129Diagnostic(
                            W129Diagnostic.Severity.ERROR,
                            line,
                            Math.max(1, column),
                            "Unclosed "
                                    + name
                                    + "."
                    )
            );
        }
    }
}
