package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class W131SymbolIndex {
    private static final int MAX_SYMBOLS = 4096;
    private static final int MAX_REFERENCES = 512;

    private static final Pattern PYTHON_DECL = Pattern.compile(
            "^\\s*(def|class)\\s+([A-Za-z_][A-Za-z0-9_]*)\\b"
    );

    private static final Pattern PYTHON_ASSIGN = Pattern.compile(
            "^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=(?!=)"
    );

    private static final Pattern JS_DECL = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:default\\s+)?(function|class)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );

    private static final Pattern JS_VARIABLE = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:const|let|var)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\s*="
    );

    private static final Pattern CLASS_DECL = Pattern.compile(
            "\\b(class|interface|enum|record|struct)\\s+([A-Za-z_$][A-Za-z0-9_$]*)\\b"
    );

    private static final Pattern METHOD_DECL = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|static|final|abstract|virtual|override|async|synchronized|inline|constexpr|extern|internal|sealed|partial)\\s+)*"
                    + "(?:[A-Za-z_$][A-Za-z0-9_$:<>,.?\\[\\]*&]*\\s+)+"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\([^;]*\\)\\s*(?:\\{|=>)?\\s*$"
    );

    private static final Pattern FIELD_DECL = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|static|final|const|let|var|readonly|volatile)\\s+)*"
                    + "(?:[A-Za-z_$][A-Za-z0-9_$:<>,.?\\[\\]*&]*\\s+)"
                    + "([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?:=|;)"
    );

    private static final Pattern ASM_LABEL = Pattern.compile(
            "^\\s*([A-Za-z_.$][A-Za-z0-9_.$]*):"
    );

    private static final Pattern HTML_ID = Pattern.compile(
            "\\bid\\s*=\\s*[\"']([^\"']+)[\"']"
    );

    private static final Pattern CSS_SELECTOR = Pattern.compile(
            "^\\s*([^@][^{]{0,80})\\{\\s*$"
    );

    private W131SymbolIndex() {
    }

    public static Index build(W128WebProject project) {
        List<Symbol> collected = new ArrayList<>();

        project.files()
                .values()
                .stream()
                .filter(file -> !W130Workspace.internalPath(file.path()))
                .filter(file -> !file.generated())
                .sorted(Comparator.comparing(W128WebFile::path))
                .forEach(file -> parseFile(file, collected));

        List<Symbol> symbols = collected.size() > MAX_SYMBOLS
                ? new ArrayList<>(
                collected.subList(0, MAX_SYMBOLS)
        )
                : new ArrayList<>(collected);

        symbols.sort(
                Comparator.comparing(Symbol::path)
                        .thenComparingInt(Symbol::line)
                        .thenComparing(Symbol::name)
        );

        return new Index(symbols);
    }

    public static List<Symbol> outline(
            W128WebProject project,
            String path
    ) {
        if (path == null || path.isBlank()) {
            return List.of();
        }

        return build(project).symbols()
                .stream()
                .filter(symbol -> symbol.path().equals(path))
                .toList();
    }

    public static List<Symbol> search(
            W128WebProject project,
            String query,
            int limit
    ) {
        String normalized = query == null
                ? ""
                : query.trim().toLowerCase(Locale.ROOT);

        int cap = Math.max(1, limit);

        return build(project).symbols()
                .stream()
                .filter(symbol -> normalized.isEmpty()
                        || symbol.name().toLowerCase(Locale.ROOT).contains(normalized)
                        || symbol.kind().toLowerCase(Locale.ROOT).contains(normalized)
                        || symbol.path().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(cap)
                .toList();
    }

    public static List<Symbol> definitions(
            W128WebProject project,
            String name,
            int limit
    ) {
        String target = name == null ? "" : name.trim();

        if (target.isEmpty()) {
            return List.of();
        }

        return build(project).symbols()
                .stream()
                .filter(symbol -> symbol.name().equals(target))
                .limit(Math.max(1, limit))
                .toList();
    }

    public static List<Symbol> completions(
            W128WebProject project,
            String prefix,
            int limit
    ) {
        String target = prefix == null
                ? ""
                : prefix.trim().toLowerCase(Locale.ROOT);

        LinkedHashMap<String, Symbol> unique = new LinkedHashMap<>();

        for (Symbol symbol : build(project).symbols()) {
            if (!target.isEmpty()
                    && !symbol.name().toLowerCase(Locale.ROOT).startsWith(target)) {
                continue;
            }

            unique.putIfAbsent(symbol.name(), symbol);

            if (unique.size() >= Math.max(1, limit)) {
                break;
            }
        }

        return List.copyOf(unique.values());
    }

    public static List<Symbol> references(
            W128WebProject project,
            String name,
            int limit
    ) {
        String target = name == null ? "" : name.trim();

        if (target.isEmpty()) {
            return List.of();
        }

        Pattern pattern = Pattern.compile(
                "(?<![A-Za-z0-9_$])"
                        + Pattern.quote(target)
                        + "(?![A-Za-z0-9_$])"
        );

        List<Symbol> result = new ArrayList<>();

        for (W128WebFile file : project.files().values()) {
            if (W130Workspace.internalPath(file.path())
                    || file.generated()) {
                continue;
            }

            String[] lines = normalize(file.content()).split("\\n", -1);

            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = pattern.matcher(lines[i]);

                while (matcher.find()) {
                    result.add(
                            new Symbol(
                                    "REF",
                                    target,
                                    file.path(),
                                    i + 1,
                                    matcher.start() + 1,
                                    preview(lines[i])
                            )
                    );

                    if (result.size() >= Math.min(MAX_REFERENCES, Math.max(1, limit))) {
                        return List.copyOf(result);
                    }
                }
            }
        }

        return List.copyOf(result);
    }

    public static String toWire(List<Symbol> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return "";
        }

        return symbols.stream()
                .map(W131SymbolIndex::toWire)
                .reduce(
                        (left, right) -> left + "\n" + right
                )
                .orElse("");
    }

    public static String summary(W128WebProject project) {
        Index index = build(project);
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (Symbol symbol : index.symbols()) {
            counts.merge(symbol.kind(), 1, Integer::sum);
        }

        StringBuilder out = new StringBuilder();
        out.append("Workspace symbol index\n");
        out.append("Symbols: ").append(index.symbols().size()).append('\n');
        out.append("Files: ").append(
                project.files()
                        .values()
                        .stream()
                        .filter(file -> !W130Workspace.internalPath(file.path()))
                        .filter(file -> !file.generated())
                        .count()
        );

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            out.append('\n')
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue());
        }

        return out.toString();
    }

    private static void parseFile(
            W128WebFile file,
            List<Symbol> output
    ) {
        W128IdeLanguage language = W128IdeLanguage.detect(file.path());
        String[] lines = normalize(file.content()).split("\\n", -1);

        for (int i = 0; i < lines.length && output.size() < MAX_SYMBOLS; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            switch (language) {
                case PYTHON -> parsePython(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                case JAVASCRIPT, JSX -> parseJavaScript(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                case JAVA, CSHARP, C, CPP -> parseCompiled(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                case ASSEMBLY -> parseAssembly(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                case HTML -> parseHtml(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                case CSS -> parseCss(
                        file.path(),
                        line,
                        lineNumber,
                        output
                );

                default -> {
                }
            }
        }
    }

    private static void parsePython(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher declaration = PYTHON_DECL.matcher(line);

        if (declaration.find()) {
            add(
                    output,
                    "class".equals(declaration.group(1))
                            ? "CLASS"
                            : "FUNCTION",
                    declaration.group(2),
                    path,
                    lineNumber,
                    column(line, declaration.group(2)),
                    preview(line)
            );
            return;
        }

        Matcher assignment = PYTHON_ASSIGN.matcher(line);

        if (assignment.find()) {
            add(
                    output,
                    "VARIABLE",
                    assignment.group(1),
                    path,
                    lineNumber,
                    column(line, assignment.group(1)),
                    preview(line)
            );
        }
    }

    private static void parseJavaScript(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher declaration = JS_DECL.matcher(line);

        if (declaration.find()) {
            add(
                    output,
                    "class".equals(declaration.group(1))
                            ? "CLASS"
                            : "FUNCTION",
                    declaration.group(2),
                    path,
                    lineNumber,
                    column(line, declaration.group(2)),
                    preview(line)
            );
            return;
        }

        Matcher variable = JS_VARIABLE.matcher(line);

        if (variable.find()) {
            String kind = line.contains("=>")
                    ? "FUNCTION"
                    : "VARIABLE";

            add(
                    output,
                    kind,
                    variable.group(1),
                    path,
                    lineNumber,
                    column(line, variable.group(1)),
                    preview(line)
            );
        }
    }

    private static void parseCompiled(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher classMatcher = CLASS_DECL.matcher(line);

        if (classMatcher.find()) {
            add(
                    output,
                    "CLASS",
                    classMatcher.group(2),
                    path,
                    lineNumber,
                    column(line, classMatcher.group(2)),
                    preview(line)
            );
        }

        Matcher method = METHOD_DECL.matcher(line);

        if (method.find()) {
            String name = method.group(1);

            if (!Set.of(
                    "if",
                    "for",
                    "while",
                    "switch",
                    "catch"
            ).contains(name)) {
                add(
                        output,
                        "FUNCTION",
                        name,
                        path,
                        lineNumber,
                        column(line, name),
                        preview(line)
                );
                return;
            }
        }

        Matcher field = FIELD_DECL.matcher(line);

        if (field.find()) {
            add(
                    output,
                    "VARIABLE",
                    field.group(1),
                    path,
                    lineNumber,
                    column(line, field.group(1)),
                    preview(line)
            );
        }
    }

    private static void parseAssembly(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher label = ASM_LABEL.matcher(line);

        if (label.find()) {
            add(
                    output,
                    "LABEL",
                    label.group(1),
                    path,
                    lineNumber,
                    column(line, label.group(1)),
                    preview(line)
            );
        }
    }

    private static void parseHtml(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher id = HTML_ID.matcher(line);

        while (id.find()) {
            add(
                    output,
                    "ID",
                    id.group(1),
                    path,
                    lineNumber,
                    id.start(1) + 1,
                    preview(line)
            );
        }
    }

    private static void parseCss(
            String path,
            String line,
            int lineNumber,
            List<Symbol> output
    ) {
        Matcher selector = CSS_SELECTOR.matcher(line);

        if (selector.find()) {
            String value = selector.group(1).trim();

            if (!value.isBlank()) {
                add(
                        output,
                        "SELECTOR",
                        value,
                        path,
                        lineNumber,
                        column(line, value),
                        preview(line)
                );
            }
        }
    }

    private static void add(
            List<Symbol> output,
            String kind,
            String name,
            String path,
            int line,
            int column,
            String detail
    ) {
        if (name == null || name.isBlank()) {
            return;
        }

        Symbol symbol = new Symbol(
                kind,
                name,
                path,
                line,
                column,
                detail
        );

        if (!output.contains(symbol)) {
            output.add(symbol);
        }
    }

    private static int column(String line, String name) {
        int index = line.indexOf(name);
        return index < 0 ? 1 : index + 1;
    }

    private static String normalize(String source) {
        return source == null
                ? ""
                : source.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String preview(String line) {
        String value = line == null
                ? ""
                : line.trim().replace('|', '/');

        if (value.length() > 120) {
            return value.substring(0, 117) + "...";
        }

        return value;
    }

    private static String toWire(Symbol symbol) {
        return clean(symbol.kind())
                + "|"
                + clean(symbol.name())
                + "|"
                + clean(symbol.path())
                + "|"
                + symbol.line()
                + "|"
                + symbol.column()
                + "|"
                + clean(symbol.detail());
    }

    private static String clean(String value) {
        return value == null
                ? ""
                : value.replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ');
    }

    public record Index(
            List<Symbol> symbols
    ) {
        public Index {
            symbols = symbols == null
                    ? List.of()
                    : List.copyOf(symbols);
        }
    }

    public record Symbol(
            String kind,
            String name,
            String path,
            int line,
            int column,
            String detail
    ) {
        public Symbol {
            kind = kind == null ? "SYMBOL" : kind;
            name = name == null ? "" : name;
            path = path == null ? "" : path;
            line = Math.max(1, line);
            column = Math.max(1, column);
            detail = detail == null ? "" : detail;
        }
    }
}
