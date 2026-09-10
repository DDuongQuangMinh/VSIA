package com.k1ngtle.vsia.signality.internet.web;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class W129ConsoleSubsetRuntime {
    private static final Pattern DECLARATION =
            Pattern.compile(
                    "^(?:(?:const|let|var|int|long|float|double|String|string|bool|boolean)\\s+)?([A-Za-z_$][A-Za-z0-9_$]*)\\s*=\\s*(.+?);?$"
            );

    private W129ConsoleSubsetRuntime() {
    }

    public static W129RunResult run(
            W128IdeLanguage language,
            String source
    ) {
        List<W129Diagnostic> diagnostics =
                W129Diagnostics.validate(
                        language,
                        source
                );

        boolean hasError =
                diagnostics.stream()
                        .anyMatch(
                                diagnostic ->
                                        diagnostic.severity()
                                                == W129Diagnostic.Severity.ERROR
                        );

        if (hasError) {
            return W129RunResult.fail(
                    "Validation failed.",
                    diagnostics
            );
        }

        String normalized =
                source == null
                        ? ""
                        : source
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');

        String[] lines =
                normalized.split("\n", -1);

        Map<String, Object> variables =
                new HashMap<>();

        StringBuilder output =
                new StringBuilder();

        long cycles = 0L;

        for (int i = 0;
             i < lines.length;
             i++) {
            String line =
                    stripLineComment(
                            language,
                            lines[i]
                    ).trim();

            if (line.isEmpty()) {
                continue;
            }

            cycles++;

            try {
                if (language == W128IdeLanguage.PYTHON
                        && line.startsWith("print(")
                        && line.endsWith(")")) {
                    append(
                            output,
                            W129Expression.printable(
                                    W129Expression.evaluate(
                                            insideCall(line),
                                            variables
                                    )
                            )
                    );
                    continue;
                }

                if (language == W128IdeLanguage.C
                        && line.startsWith("printf(")
                        && line.endsWith(");")) {
                    append(
                            output,
                            evaluatePrintf(
                                    insideCall(
                                            line.substring(
                                                    0,
                                                    line.length() - 1
                                            )
                                    ),
                                    variables
                            )
                    );
                    continue;
                }

                if (language == W128IdeLanguage.CPP
                        && line.contains("std::cout")) {
                    append(
                            output,
                            evaluateCout(
                                    line,
                                    variables
                            )
                    );
                    continue;
                }

                if (language == W128IdeLanguage.CSHARP
                        && line.contains("Console.WriteLine(")) {
                    append(
                            output,
                            W129Expression.printable(
                                    W129Expression.evaluate(
                                            callArgument(
                                                    line,
                                                    "Console.WriteLine("
                                            ),
                                            variables
                                    )
                            )
                    );
                    continue;
                }

                if (language == W128IdeLanguage.JAVA
                        && line.contains("System.out.println(")) {
                    append(
                            output,
                            W129Expression.printable(
                                    W129Expression.evaluate(
                                            callArgument(
                                                    line,
                                                    "System.out.println("
                                            ),
                                            variables
                                    )
                            )
                    );
                    continue;
                }

                String assignment =
                        stripTrailingSemicolon(line);

                Matcher matcher =
                        DECLARATION.matcher(
                                assignment
                        );

                if (matcher.matches()
                        && !assignment.contains("==")) {
                    String name =
                            matcher.group(1);
                    String expression =
                            matcher.group(2);

                    variables.put(
                            name,
                            W129Expression.evaluate(
                                    expression,
                                    variables
                            )
                    );
                    continue;
                }

                if (line.startsWith("return ")) {
                    continue;
                }

                if (isStructural(language, line)) {
                    continue;
                }

                if (containsUnsupportedControlFlow(line)) {
                    diagnostics =
                            appendDiagnostic(
                                    diagnostics,
                                    new W129Diagnostic(
                                            W129Diagnostic.Severity.WARNING,
                                            i + 1,
                                            1,
                                            "Control flow is accepted by the editor but not executed by the W1.29 console subset runtime."
                                    )
                            );
                }
            } catch (IllegalArgumentException exception) {
                diagnostics =
                        appendDiagnostic(
                                diagnostics,
                                new W129Diagnostic(
                                        W129Diagnostic.Severity.ERROR,
                                        i + 1,
                                        1,
                                        exception.getMessage()
                                )
                        );

                return new W129RunResult(
                        false,
                        1,
                        output.toString().stripTrailing(),
                        cycles,
                        variables.size() * 16L,
                        diagnostics
                );
            }
        }

        return new W129RunResult(
                true,
                0,
                output.toString().stripTrailing(),
                cycles,
                variables.size() * 16L,
                diagnostics
        );
    }

    private static String evaluatePrintf(
            String args,
            Map<String, Object> variables
    ) {
        List<String> parts =
                splitCommaArguments(args);

        if (parts.isEmpty()) {
            return "";
        }

        Object first =
                W129Expression.evaluate(
                        parts.get(0),
                        variables
                );

        String format =
                W129Expression.printable(first);

        if (parts.size() == 1) {
            return format
                    .replace("\\n", "\n");
        }

        String result = format;

        for (int i = 1;
             i < parts.size();
             i++) {
            String replacement =
                    W129Expression.printable(
                            W129Expression.evaluate(
                                    parts.get(i),
                                    variables
                            )
                    );

            result =
                    result.replaceFirst(
                            "%[diufsg]",
                            Matcher.quoteReplacement(
                                    replacement
                            )
                    );
        }

        return result
                .replace("\\n", "\n")
                .stripTrailing();
    }

    private static String evaluateCout(
            String line,
            Map<String, Object> variables
    ) {
        String value =
                stripTrailingSemicolon(line);

        int index =
                value.indexOf("std::cout");

        value =
                value.substring(
                        index
                                + "std::cout".length()
                );

        String[] parts =
                value.split("<<");

        StringBuilder out =
                new StringBuilder();

        for (String raw : parts) {
            String part =
                    raw.trim();

            if (part.isEmpty()) {
                continue;
            }

            if ("std::endl".equals(part)
                    || "endl".equals(part)) {
                continue;
            }

            out.append(
                    W129Expression.printable(
                            W129Expression.evaluate(
                                    part,
                                    variables
                            )
                    )
            );
        }

        return out.toString();
    }

    private static String insideCall(
            String line
    ) {
        int open =
                line.indexOf('(');

        int close =
                line.lastIndexOf(')');

        if (open < 0
                || close <= open) {
            return "";
        }

        return line.substring(
                open + 1,
                close
        );
    }

    private static String callArgument(
            String line,
            String marker
    ) {
        int start =
                line.indexOf(marker);

        if (start < 0) {
            return "";
        }

        start += marker.length();

        int end =
                line.lastIndexOf(')');

        if (end < start) {
            return "";
        }

        return line.substring(
                start,
                end
        );
    }

    private static List<String> splitCommaArguments(
            String value
    ) {
        java.util.ArrayList<String> parts =
                new java.util.ArrayList<>();

        StringBuilder current =
                new StringBuilder();

        boolean single = false;
        boolean dbl = false;
        int depth = 0;

        for (int i = 0;
             i < value.length();
             i++) {
            char c = value.charAt(i);

            if (c == '\''
                    && !dbl) {
                single = !single;
                current.append(c);
                continue;
            }

            if (c == '"'
                    && !single) {
                dbl = !dbl;
                current.append(c);
                continue;
            }

            if (!single
                    && !dbl) {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (c == ','
                        && depth == 0) {
                    parts.add(
                            current.toString().trim()
                    );
                    current.setLength(0);
                    continue;
                }
            }

            current.append(c);
        }

        if (!current.toString().trim().isEmpty()) {
            parts.add(
                    current.toString().trim()
            );
        }

        return List.copyOf(parts);
    }

    private static boolean isStructural(
            W128IdeLanguage language,
            String line
    ) {
        String lower =
                line.toLowerCase(Locale.ROOT);

        if ("{".equals(line)
                || "}".equals(line)
                || "};".equals(line)
                || line.endsWith("{")
                || line.startsWith("#include")
                || line.startsWith("using ")
                || line.startsWith("package ")
                || line.startsWith("import ")
                || line.startsWith("public class ")
                || line.startsWith("class ")
                || line.startsWith("public static ")
                || line.startsWith("static ")
                || line.startsWith("int main")
                || line.startsWith("void main")
                || line.startsWith("def ")
                || line.startsWith("if __name__")) {
            return true;
        }

        return language == W128IdeLanguage.PYTHON
                && (
                lower.equals("main()")
                        || lower.equals("pass")
        );
    }

    private static boolean containsUnsupportedControlFlow(
            String line
    ) {
        String lower =
                line.toLowerCase(Locale.ROOT);

        return lower.startsWith("for ")
                || lower.startsWith("for(")
                || lower.startsWith("while ")
                || lower.startsWith("while(")
                || lower.startsWith("switch ")
                || lower.startsWith("switch(");
    }

    private static String stripTrailingSemicolon(
            String value
    ) {
        String trimmed =
                value.trim();

        return trimmed.endsWith(";")
                ? trimmed.substring(
                0,
                trimmed.length() - 1
        ).trim()
                : trimmed;
    }

    private static String stripLineComment(
            W128IdeLanguage language,
            String value
    ) {
        String marker =
                language == W128IdeLanguage.PYTHON
                        ? "#"
                        : "//";

        boolean single = false;
        boolean dbl = false;

        for (int i = 0;
             i <= value.length()
                     - marker.length();
             i++) {
            char c =
                    value.charAt(i);

            if (c == '\''
                    && !dbl) {
                single = !single;
            } else if (c == '"'
                    && !single) {
                dbl = !dbl;
            }

            if (!single
                    && !dbl
                    && value.startsWith(
                    marker,
                    i
            )) {
                return value.substring(
                        0,
                        i
                );
            }
        }

        return value;
    }

    private static void append(
            StringBuilder output,
            String value
    ) {
        if (!output.isEmpty()
                && !output.toString().endsWith("\n")) {
            output.append('\n');
        }

        output.append(
                value == null ? "" : value
        );
    }

    private static List<W129Diagnostic> appendDiagnostic(
            List<W129Diagnostic> existing,
            W129Diagnostic diagnostic
    ) {
        java.util.ArrayList<W129Diagnostic> copy =
                new java.util.ArrayList<>(
                        existing
                );

        copy.add(diagnostic);
        return List.copyOf(copy);
    }
}
