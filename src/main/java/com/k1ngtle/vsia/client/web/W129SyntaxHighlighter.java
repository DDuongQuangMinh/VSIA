package com.k1ngtle.vsia.client.web;

import com.k1ngtle.vsia.signality.internet.web.W128IdeLanguage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class W129SyntaxHighlighter {
    private static final int NORMAL = 0xE7EDF5;
    private static final int KEYWORD = 0x7AA2F7;
    private static final int STRING = 0xE0AF68;
    private static final int NUMBER = 0x9ECE6A;
    private static final int COMMENT = 0x65788B;
    private static final int TYPE = 0x2AC3DE;
    private static final int PUNCT = 0xA9B1D6;

    private W129SyntaxHighlighter() {
    }

    public static List<Span> highlight(
            W128IdeLanguage language,
            String line
    ) {
        if (line == null || line.isEmpty()) {
            return List.of(new Span("", NORMAL));
        }

        List<Span> spans = new ArrayList<>();
        Set<String> keywords = keywords(language);
        Set<String> types = types(language);

        String commentMarker = switch (language) {
            case PYTHON -> "#";
            case ASSEMBLY -> ";";
            default -> "//";
        };

        int i = 0;

        while (i < line.length()) {
            if (!commentMarker.isEmpty()
                    && line.startsWith(commentMarker, i)) {
                spans.add(new Span(
                        line.substring(i),
                        COMMENT
                ));
                break;
            }

            char c = line.charAt(i);

            if (c == '"' || c == '\'') {
                int end = stringEnd(line, i, c);
                spans.add(new Span(
                        line.substring(i, end),
                        STRING
                ));
                i = end;
                continue;
            }

            if (Character.isDigit(c)) {
                int end = i + 1;

                while (end < line.length()) {
                    char n = line.charAt(end);

                    if (!Character.isLetterOrDigit(n)
                            && n != '.'
                            && n != '_'
                            && n != 'x'
                            && n != 'X') {
                        break;
                    }

                    end++;
                }

                spans.add(new Span(
                        line.substring(i, end),
                        NUMBER
                ));
                i = end;
                continue;
            }

            if (Character.isLetter(c)
                    || c == '_'
                    || c == '$'
                    || c == '#') {
                int end = i + 1;

                while (end < line.length()) {
                    char n = line.charAt(end);

                    if (!Character.isLetterOrDigit(n)
                            && n != '_'
                            && n != '$'
                            && n != '#'
                            && n != ':') {
                        break;
                    }

                    end++;
                }

                String token =
                        line.substring(i, end);

                int color =
                        keywords.contains(token)
                                ? KEYWORD
                                : (
                                types.contains(token)
                                        ? TYPE
                                        : NORMAL
                        );

                spans.add(
                        new Span(token, color)
                );
                i = end;
                continue;
            }

            if ("{}[]();,.<>:+-*/=%!&|".indexOf(c) >= 0) {
                spans.add(
                        new Span(
                                Character.toString(c),
                                PUNCT
                        )
                );
                i++;
                continue;
            }

            int end = i + 1;

            while (end < line.length()) {
                char n = line.charAt(end);

                if (n == '"'
                        || n == '\''
                        || Character.isDigit(n)
                        || Character.isLetter(n)
                        || n == '_'
                        || n == '$'
                        || "{}[]();,.<>:+-*/=%!&|".indexOf(n) >= 0
                        || line.startsWith(commentMarker, end)) {
                    break;
                }

                end++;
            }

            spans.add(
                    new Span(
                            line.substring(i, end),
                            NORMAL
                    )
            );
            i = end;
        }

        return List.copyOf(spans);
    }

    private static int stringEnd(
            String line,
            int start,
            char quote
    ) {
        boolean escape = false;

        for (int i = start + 1;
             i < line.length();
             i++) {
            char c = line.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == quote) {
                return i + 1;
            }
        }

        return line.length();
    }

    private static Set<String> keywords(
            W128IdeLanguage language
    ) {
        return switch (language) {
            case PYTHON -> Set.of(
                    "def", "if", "elif", "else", "for", "while",
                    "return", "import", "from", "class", "try",
                    "except", "finally", "with", "as", "True",
                    "False", "None", "and", "or", "not", "in"
            );
            case C, CPP -> Set.of(
                    "if", "else", "for", "while", "switch", "case",
                    "break", "continue", "return", "struct", "class",
                    "public", "private", "protected", "namespace",
                    "using", "const", "static", "new", "delete",
                    "#include"
            );
            case CSHARP -> Set.of(
                    "using", "namespace", "class", "public", "private",
                    "protected", "static", "void", "return", "if",
                    "else", "for", "while", "switch", "case", "new",
                    "var", "async", "await", "interface"
            );
            case JAVA -> Set.of(
                    "package", "import", "class", "interface", "public",
                    "private", "protected", "static", "final", "void",
                    "return", "if", "else", "for", "while", "switch",
                    "case", "new", "extends", "implements", "throws"
            );
            case ASSEMBLY -> Set.of(
                    "section", "global", "mov", "add", "sub", "mul",
                    "div", "mod", "inc", "dec", "cmp", "jmp", "je",
                    "jne", "jz", "jnz", "jg", "jl", "jge", "jle",
                    "push", "pop", "print", "nop", "halt", "ret"
            );
            case JAVASCRIPT, JSX -> Set.of(
                    "const", "let", "var", "function", "return", "if",
                    "else", "for", "while", "class", "new", "import",
                    "export", "from", "async", "await", "true", "false",
                    "null", "undefined"
            );
            case HTML -> Set.of(
                    "doctype", "html", "head", "body", "script",
                    "style", "link", "meta", "title", "div", "main"
            );
            default -> Set.of();
        };
    }

    private static Set<String> types(
            W128IdeLanguage language
    ) {
        return switch (language) {
            case C, CPP -> Set.of(
                    "void", "char", "short", "int", "long", "float",
                    "double", "bool", "size_t", "std::string"
            );
            case CSHARP -> Set.of(
                    "void", "byte", "short", "int", "long", "float",
                    "double", "decimal", "bool", "char", "string",
                    "object"
            );
            case JAVA -> Set.of(
                    "void", "byte", "short", "int", "long", "float",
                    "double", "boolean", "char", "String", "Object"
            );
            default -> Set.of();
        };
    }

    public record Span(
            String text,
            int color
    ) {
    }
}
