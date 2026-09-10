package com.k1ngtle.vsia.signality.internet.web;

import java.util.Locale;

public final class W128CodeFormatter {
    private W128CodeFormatter() {
    }

    public static boolean supports(String path) {
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".html")
                || lower.endsWith(".htm")
                || lower.endsWith(".xml")
                || lower.endsWith(".svg")
                || lower.endsWith(".css")
                || lower.endsWith(".js")
                || lower.endsWith(".mjs")
                || lower.endsWith(".jsx")
                || lower.endsWith(".java")
                || lower.endsWith(".c")
                || lower.endsWith(".h")
                || lower.endsWith(".cpp")
                || lower.endsWith(".cc")
                || lower.endsWith(".cxx")
                || lower.endsWith(".hpp")
                || lower.endsWith(".cs");
    }

    public static String format(String path, String source) {
        String value = normalize(source);
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".html")
                || lower.endsWith(".htm")
                || lower.endsWith(".xml")
                || lower.endsWith(".svg")) {
            return formatMarkup(value);
        }

        if (lower.endsWith(".css")) {
            return formatStructured(value, true);
        }

        if (lower.endsWith(".js")
                || lower.endsWith(".mjs")
                || lower.endsWith(".jsx")
                || lower.endsWith(".java")
                || lower.endsWith(".c")
                || lower.endsWith(".h")
                || lower.endsWith(".cpp")
                || lower.endsWith(".cc")
                || lower.endsWith(".cxx")
                || lower.endsWith(".hpp")
                || lower.endsWith(".cs")) {
            return formatStructured(value, false);
        }

        return value;
    }

    private static String formatMarkup(String source) {
        String prepared = source.replaceAll(">\\s*<", ">\n<").trim();
        if (prepared.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        int depth = 0;

        for (String raw : prepared.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }

            boolean closing = line.startsWith("</");
            boolean declaration = line.startsWith("<!") || line.startsWith("<?");
            boolean selfClosing = line.endsWith("/>")
                    || line.matches("(?i)^<(area|base|br|col|embed|hr|img|input|link|meta|param|source|track|wbr)\\b.*");

            if (closing) {
                depth = Math.max(0, depth - 1);
            }

            indent(out, depth);
            out.append(line).append('\n');

            boolean opens = !closing
                    && !declaration
                    && !selfClosing
                    && line.matches("^<[^/!][^>]*>.*")
                    && !line.matches("(?s).*?</[^>]+>.*");

            if (opens) {
                depth++;
            }
        }

        return out.toString().stripTrailing();
    }

    private static String formatStructured(String source, boolean cssMode) {
        StringBuilder out = new StringBuilder();
        StringBuilder token = new StringBuilder();

        int depth = 0;
        boolean single = false;
        boolean dbl = false;
        boolean template = false;
        boolean escape = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (lineComment) {
                token.append(c);
                if (c == '\n') {
                    flush(out, token, depth);
                    lineComment = false;
                }
                continue;
            }

            if (blockComment) {
                token.append(c);
                if (c == '*' && next == '/') {
                    token.append('/');
                    i++;
                    blockComment = false;
                }
                continue;
            }

            if (!single && !dbl && !template) {
                if (c == '/' && next == '/') {
                    token.append("//");
                    i++;
                    lineComment = true;
                    continue;
                }
                if (c == '/' && next == '*') {
                    token.append("/*");
                    i++;
                    blockComment = true;
                    continue;
                }
            }

            if (escape) {
                token.append(c);
                escape = false;
                continue;
            }

            if ((single || dbl || template) && c == '\\') {
                token.append(c);
                escape = true;
                continue;
            }

            if (!dbl && !template && c == '\'') {
                single = !single;
                token.append(c);
                continue;
            }

            if (!single && !template && c == '"') {
                dbl = !dbl;
                token.append(c);
                continue;
            }

            if (!single && !dbl && c == '`') {
                template = !template;
                token.append(c);
                continue;
            }

            if (single || dbl || template) {
                token.append(c);
                continue;
            }

            if (c == '{') {
                trim(token);
                if (!token.isEmpty()) {
                    token.append(" {");
                } else {
                    token.append('{');
                }
                flush(out, token, depth);
                depth++;
                continue;
            }

            if (c == '}') {
                flush(out, token, depth);
                depth = Math.max(0, depth - 1);
                indent(out, depth);
                out.append('}');
                if (next == ';') {
                    out.append(';');
                    i++;
                }
                out.append('\n');
                continue;
            }

            if (c == ';') {
                token.append(';');
                flush(out, token, depth);
                continue;
            }

            if (c == '\n' || Character.isWhitespace(c)) {
                if (!token.isEmpty() && !Character.isWhitespace(token.charAt(token.length() - 1))) {
                    token.append(' ');
                }
                continue;
            }

            if (cssMode && c == ':') {
                trim(token);
                token.append(": ");
                continue;
            }

            if (c == ',') {
                trim(token);
                token.append(", ");
                continue;
            }

            token.append(c);
        }

        flush(out, token, depth);
        return out.toString().stripTrailing();
    }

    private static void flush(StringBuilder out, StringBuilder token, int depth) {
        String value = token.toString().trim();
        token.setLength(0);

        if (value.isEmpty()) {
            return;
        }

        indent(out, depth);
        out.append(value).append('\n');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("    ".repeat(Math.max(0, depth)));
    }

    private static void trim(StringBuilder builder) {
        while (!builder.isEmpty() && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.setLength(builder.length() - 1);
        }
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }
}
