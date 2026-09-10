package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayList;
import java.util.List;

public final class W128JsxCompiler {
    private W128JsxCompiler() {
    }

    public static W128WebBuildResult validate(String source) {
        try {
            compile(source);
            return W128WebBuildResult.ok("JSX syntax accepted.");
        } catch (IllegalArgumentException exception) {
            return W128WebBuildResult.fail(exception.getMessage());
        }
    }

    public static String compile(String source) {
        if (source == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int index = 0;
        State state = State.CODE;

        while (index < source.length()) {
            char c = source.charAt(index);
            char n = index + 1 < source.length() ? source.charAt(index + 1) : '\0';

            if (state == State.CODE) {
                if (c == '\'' ) {
                    state = State.SINGLE;
                    out.append(c);
                    index++;
                    continue;
                }
                if (c == '"') {
                    state = State.DOUBLE;
                    out.append(c);
                    index++;
                    continue;
                }
                if (c == '`') {
                    state = State.TEMPLATE;
                    out.append(c);
                    index++;
                    continue;
                }
                if (c == '/' && n == '/') {
                    state = State.LINE_COMMENT;
                    out.append(c).append(n);
                    index += 2;
                    continue;
                }
                if (c == '/' && n == '*') {
                    state = State.BLOCK_COMMENT;
                    out.append(c).append(n);
                    index += 2;
                    continue;
                }
                if (c == '<' && looksLikeJsxStart(source, index)) {
                    Parsed parsed = parseNode(source, index);
                    out.append(parsed.expression());
                    index = parsed.end();
                    continue;
                }

                out.append(c);
                index++;
                continue;
            }

            out.append(c);
            index++;

            if (state == State.SINGLE) {
                if (c == '\\' && index < source.length()) {
                    out.append(source.charAt(index));
                    index++;
                } else if (c == '\'') {
                    state = State.CODE;
                }
            } else if (state == State.DOUBLE) {
                if (c == '\\' && index < source.length()) {
                    out.append(source.charAt(index));
                    index++;
                } else if (c == '"') {
                    state = State.CODE;
                }
            } else if (state == State.TEMPLATE) {
                if (c == '\\' && index < source.length()) {
                    out.append(source.charAt(index));
                    index++;
                } else if (c == '`') {
                    state = State.CODE;
                }
            } else if (state == State.LINE_COMMENT) {
                if (c == '\n') {
                    state = State.CODE;
                }
            } else if (state == State.BLOCK_COMMENT) {
                if (c == '*' && index < source.length() && source.charAt(index) == '/') {
                    out.append('/');
                    index++;
                    state = State.CODE;
                }
            }
        }

        return out.toString();
    }

    private static boolean looksLikeJsxStart(String source, int index) {
        if (index + 1 >= source.length()) {
            return false;
        }

        char next = source.charAt(index + 1);
        return next == '>' || Character.isLetter(next);
    }

    private static Parsed parseNode(String source, int start) {
        Cursor cursor = new Cursor(source, start);
        cursor.expect('<');

        if (cursor.peek() == '>') {
            cursor.next();
            List<String> children = parseChildren(cursor, "");
            return new Parsed(
                    "React.createElement(React.Fragment,null" + childSuffix(children) + ")",
                    cursor.index
            );
        }

        String tag = cursor.readTagName();
        if (tag.isBlank()) {
            throw cursor.error("Expected JSX tag name");
        }

        List<String> propPieces = new ArrayList<>();
        boolean selfClosing = false;

        while (!cursor.eof()) {
            cursor.skipWhitespace();

            if (cursor.startsWith("/>")) {
                cursor.index += 2;
                selfClosing = true;
                break;
            }

            if (cursor.peek() == '>') {
                cursor.next();
                break;
            }

            if (cursor.peek() == '{' && cursor.startsWith("{...")) {
                String spread = cursor.readBraceExpression();
                String expression = spread.trim();
                if (!expression.startsWith("...")) {
                    throw cursor.error("Invalid JSX spread attribute");
                }
                propPieces.add("...(" + expression.substring(3).trim() + ")");
                continue;
            }

            String name = cursor.readAttributeName();
            if (name.isBlank()) {
                throw cursor.error("Expected JSX attribute name");
            }

            cursor.skipWhitespace();
            String value = "true";

            if (cursor.peek() == '=') {
                cursor.next();
                cursor.skipWhitespace();

                char valueStart = cursor.peek();
                if (valueStart == '"' || valueStart == '\'') {
                    value = quote(cursor.readQuoted());
                } else if (valueStart == '{') {
                    String expression = cursor.readBraceExpression().trim();
                    value = expression.isBlank() ? "undefined" : "(" + expression + ")";
                } else {
                    throw cursor.error("JSX attribute value must be quoted or wrapped in braces");
                }
            }

            propPieces.add(quote(name) + ":" + value);
        }

        if (selfClosing) {
            return new Parsed(
                    "React.createElement(" + tagExpression(tag) + "," + propsExpression(propPieces) + ")",
                    cursor.index
            );
        }

        List<String> children = parseChildren(cursor, tag);
        return new Parsed(
                "React.createElement(" + tagExpression(tag) + "," + propsExpression(propPieces) + childSuffix(children) + ")",
                cursor.index
        );
    }

    private static List<String> parseChildren(Cursor cursor, String expectedTag) {
        List<String> children = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        while (!cursor.eof()) {
            if (cursor.startsWith("</")) {
                flushText(children, text);
                cursor.index += 2;

                if (expectedTag.isEmpty()) {
                    cursor.skipWhitespace();
                    cursor.expect('>');
                    return children;
                }

                String closing = cursor.readTagName();
                cursor.skipWhitespace();
                cursor.expect('>');

                if (!expectedTag.equals(closing)) {
                    throw cursor.error(
                            "Mismatched JSX closing tag: expected </" + expectedTag + "> but found </" + closing + ">"
                    );
                }
                return children;
            }

            if (cursor.peek() == '<' && looksLikeJsxStart(cursor.source, cursor.index)) {
                flushText(children, text);
                Parsed child = parseNode(cursor.source, cursor.index);
                children.add(child.expression());
                cursor.index = child.end();
                continue;
            }

            if (cursor.peek() == '{') {
                flushText(children, text);
                String expression = cursor.readBraceExpression().trim();
                if (!expression.isBlank() && !expression.startsWith("/*")) {
                    children.add("(" + expression + ")");
                }
                continue;
            }

            text.append(cursor.next());
        }

        throw cursor.error(expectedTag.isEmpty()
                ? "Unclosed JSX fragment"
                : "Unclosed JSX tag <" + expectedTag + ">"
        );
    }

    private static void flushText(List<String> children, StringBuilder text) {
        if (text.isEmpty()) {
            return;
        }

        String normalized = text.toString()
                .replaceAll("[\\t\\r\\n ]+", " ");

        if (!normalized.isBlank()) {
            children.add(quote(normalized));
        }

        text.setLength(0);
    }

    private static String propsExpression(List<String> pieces) {
        if (pieces.isEmpty()) {
            return "null";
        }

        boolean hasSpread = pieces.stream().anyMatch(piece -> piece.startsWith("...("));
        if (!hasSpread) {
            return "{" + String.join(",", pieces) + "}";
        }

        List<String> assign = new ArrayList<>();
        List<String> object = new ArrayList<>();

        for (String piece : pieces) {
            if (piece.startsWith("...(")) {
                if (!object.isEmpty()) {
                    assign.add("{" + String.join(",", object) + "}");
                    object.clear();
                }
                assign.add(piece.substring(3));
            } else {
                object.add(piece);
            }
        }

        if (!object.isEmpty()) {
            assign.add("{" + String.join(",", object) + "}");
        }

        return "Object.assign({}," + String.join(",", assign) + ")";
    }

    private static String childSuffix(List<String> children) {
        return children.isEmpty() ? "" : "," + String.join(",", children);
    }

    private static String tagExpression(String tag) {
        char first = tag.charAt(0);
        if (Character.isLowerCase(first) || tag.indexOf('-') >= 0) {
            return quote(tag);
        }
        return tag;
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private enum State {
        CODE,
        SINGLE,
        DOUBLE,
        TEMPLATE,
        LINE_COMMENT,
        BLOCK_COMMENT
    }

    private record Parsed(String expression, int end) {
    }

    private static final class Cursor {
        private final String source;
        private int index;

        private Cursor(String source, int index) {
            this.source = source;
            this.index = index;
        }

        private boolean eof() {
            return index >= source.length();
        }

        private char peek() {
            return eof() ? '\0' : source.charAt(index);
        }

        private char next() {
            return eof() ? '\0' : source.charAt(index++);
        }

        private boolean startsWith(String value) {
            return source.startsWith(value, index);
        }

        private void expect(char expected) {
            if (peek() != expected) {
                throw error("Expected '" + expected + "'");
            }
            index++;
        }

        private void skipWhitespace() {
            while (!eof() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private String readTagName() {
            int start = index;
            while (!eof()) {
                char c = peek();
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '.' || c == '-' || c == ':') {
                    index++;
                } else {
                    break;
                }
            }
            return source.substring(start, index);
        }

        private String readAttributeName() {
            int start = index;
            while (!eof()) {
                char c = peek();
                if (Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '-' || c == ':' || c == '.') {
                    index++;
                } else {
                    break;
                }
            }
            return source.substring(start, index);
        }

        private String readQuoted() {
            char quote = next();
            StringBuilder out = new StringBuilder();

            while (!eof()) {
                char c = next();
                if (c == quote) {
                    return out.toString();
                }
                if (c == '\\' && !eof()) {
                    char escaped = next();
                    switch (escaped) {
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        default -> out.append(escaped);
                    }
                } else {
                    out.append(c);
                }
            }

            throw error("Unclosed JSX quoted attribute");
        }

        private String readBraceExpression() {
            expect('{');
            int start = index;
            int depth = 1;
            State state = State.CODE;

            while (!eof()) {
                char c = next();
                char n = peek();

                if (state == State.CODE) {
                    if (c == '\'') {
                        state = State.SINGLE;
                    } else if (c == '"') {
                        state = State.DOUBLE;
                    } else if (c == '`') {
                        state = State.TEMPLATE;
                    } else if (c == '/' && n == '/') {
                        next();
                        state = State.LINE_COMMENT;
                    } else if (c == '/' && n == '*') {
                        next();
                        state = State.BLOCK_COMMENT;
                    } else if (c == '{') {
                        depth++;
                    } else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            return source.substring(start, index - 1);
                        }
                    }
                    continue;
                }

                if (state == State.SINGLE || state == State.DOUBLE || state == State.TEMPLATE) {
                    char terminal = state == State.SINGLE ? '\'' : state == State.DOUBLE ? '"' : '`';
                    if (c == '\\' && !eof()) {
                        next();
                    } else if (c == terminal) {
                        state = State.CODE;
                    }
                } else if (state == State.LINE_COMMENT) {
                    if (c == '\n') {
                        state = State.CODE;
                    }
                } else if (state == State.BLOCK_COMMENT) {
                    if (c == '*' && n == '/') {
                        next();
                        state = State.CODE;
                    }
                }
            }

            throw error("Unclosed JSX expression brace");
        }

        private IllegalArgumentException error(String message) {
            int line = 1;
            int column = 1;
            for (int i = 0; i < Math.min(index, source.length()); i++) {
                if (source.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return new IllegalArgumentException(message + " at line " + line + ", column " + column + ".");
        }
    }
}
