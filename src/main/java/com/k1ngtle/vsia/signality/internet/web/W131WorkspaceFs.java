package com.k1ngtle.vsia.signality.internet.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class W131WorkspaceFs {
    private static final Set<String> SHELL_VERBS = Set.of(
            "pwd",
            "cd",
            "ls",
            "tree",
            "cat",
            "touch",
            "mkdir",
            "cp",
            "mv",
            "rm"
    );

    private W131WorkspaceFs() {
    }

    public static boolean handlesTerminalVerb(String verb) {
        return verb != null
                && SHELL_VERBS.contains(
                verb.trim().toLowerCase(Locale.ROOT)
        );
    }

    public static ShellResult execute(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String rawCommand,
            long nowMillis
    ) {
        List<String> tokens = tokenize(rawCommand);

        if (tokens.isEmpty()) {
            return ok("shell", "", "Shell ready.");
        }

        String verb = tokens.get(0).toLowerCase(Locale.ROOT);
        String cwd = W131WorkspaceState.cwd(project);

        return switch (verb) {
            case "pwd" -> ok(
                    "pwd",
                    cwd,
                    "Working directory: " + cwd
            );

            case "cd" -> cd(
                    data,
                    project,
                    tokens.size() > 1 ? tokens.get(1) : "/",
                    nowMillis
            );

            case "ls" -> ls(
                    project,
                    tokens.size() > 1 ? tokens.get(1) : ""
            );

            case "tree" -> tree(
                    project,
                    tokens.size() > 1 ? tokens.get(1) : ""
            );

            case "cat" -> cat(
                    project,
                    tokens.size() > 1 ? tokens.get(1) : ""
            );

            case "mkdir" -> mkdir(
                    data,
                    project,
                    tokens.size() > 1 ? tokens.get(1) : "",
                    nowMillis
            );

            case "touch" -> touch(
                    data,
                    project,
                    actor,
                    tokens.size() > 1 ? tokens.get(1) : "",
                    nowMillis
            );

            case "cp" -> {
                if (tokens.size() < 3) {
                    yield fail(
                            "cp",
                            "Usage: cp <source> <destination>"
                    );
                }

                yield copy(
                        data,
                        project,
                        actor,
                        tokens.get(1),
                        tokens.get(2),
                        nowMillis
                );
            }

            case "mv" -> {
                if (tokens.size() < 3) {
                    yield fail(
                            "mv",
                            "Usage: mv <source> <destination>"
                    );
                }

                yield move(
                        data,
                        project,
                        actor,
                        tokens.get(1),
                        tokens.get(2),
                        nowMillis
                );
            }

            case "rm" -> {
                if (tokens.size() < 2) {
                    yield fail(
                            "rm",
                            "Usage: rm <path>"
                    );
                }

                yield remove(
                        data,
                        project,
                        actor,
                        tokens.get(1),
                        nowMillis
                );
            }

            default -> fail(
                    "shell",
                    "Unknown W1.31 shell command: " + verb
            );
        };
    }

    public static String resolvePath(
            String cwd,
            String raw
    ) {
        String base = W131WorkspaceState.normalizeDirectory(cwd);
        String value = raw == null ? "" : raw.trim();

        if (value.isEmpty()) {
            return base;
        }

        value = value.replace('\\', '/');

        String combined;

        if (value.startsWith("/")) {
            combined = value;
        } else if ("/".equals(base)) {
            combined = "/" + value;
        } else {
            combined = base + "/" + value;
        }

        List<String> segments = new ArrayList<>();

        for (String segment : combined.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }

            if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
                continue;
            }

            if (segment.length() > 128) {
                throw new IllegalArgumentException(
                        "A path segment exceeds 128 characters."
                );
            }

            segments.add(segment);
        }

        String resolved = segments.isEmpty()
                ? "/"
                : "/" + String.join("/", segments);

        if (resolved.length() > 512) {
            throw new IllegalArgumentException(
                    "Path exceeds 512 characters."
            );
        }

        return resolved;
    }

    public static boolean directoryExists(
            W128WebProject project,
            String rawPath
    ) {
        String path = W131WorkspaceState.normalizeDirectory(rawPath);

        if ("/".equals(path)) {
            return true;
        }

        if (W131WorkspaceState.folders(project).contains(path)) {
            return true;
        }

        String prefix = path + "/";

        return project.files()
                .keySet()
                .stream()
                .anyMatch(candidate -> candidate.startsWith(prefix));
    }

    public static List<String> listEntries(
            W128WebProject project,
            String rawPath
    ) {
        String path = W131WorkspaceState.normalizeDirectory(rawPath);

        if (project.file(path) != null) {
            return List.of(baseName(path));
        }

        if (!directoryExists(project, path)) {
            return List.of();
        }

        LinkedHashSet<String> directories = knownDirectories(project);
        LinkedHashSet<String> output = new LinkedHashSet<>();

        String prefix = "/".equals(path)
                ? "/"
                : path + "/";

        for (String directory : directories) {
            if (directory.equals(path)
                    || !directory.startsWith(prefix)) {
                continue;
            }

            String remaining = directory.substring(prefix.length());

            if (!remaining.isBlank() && !remaining.contains("/")) {
                output.add(remaining + "/");
            }
        }

        for (String file : project.files().keySet()) {
            if (!file.startsWith(prefix)) {
                continue;
            }

            String remaining = file.substring(prefix.length());

            if (!remaining.isBlank() && !remaining.contains("/")) {
                output.add(remaining);
            }
        }

        return output.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static List<String> treeEntries(
            W128WebProject project,
            String rawPath
    ) {
        String path = W131WorkspaceState.normalizeDirectory(rawPath);

        if (!directoryExists(project, path)) {
            return List.of();
        }

        LinkedHashSet<String> rows = new LinkedHashSet<>();
        String prefix = "/".equals(path)
                ? "/"
                : path + "/";

        for (String directory : knownDirectories(project)) {
            if (directory.equals(path)
                    || !directory.startsWith(prefix)
                    || W130Workspace.internalPath(directory)) {
                continue;
            }

            String relative = directory.substring(prefix.length());

            if (!relative.isBlank()) {
                rows.add(indent(relative) + baseName(directory) + "/");
            }
        }

        for (String file : project.files().keySet()) {
            if (!file.startsWith(prefix)
                    || W130Workspace.internalPath(file)) {
                continue;
            }

            String relative = file.substring(prefix.length());

            if (!relative.isBlank()) {
                rows.add(indent(relative) + baseName(file));
            }
        }

        return rows.stream()
                .sorted(Comparator.comparing(String::stripLeading, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static ShellResult cd(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String rawPath,
            long nowMillis
    ) {
        String target;

        try {
            target = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("cd", exception.getMessage());
        }

        if (!directoryExists(project, target)) {
            return fail(
                    "cd",
                    "Directory not found: " + target
            );
        }

        W131WorkspaceState.setCwd(
                data,
                project,
                target,
                nowMillis
        );

        return ok(
                "cd",
                target,
                "Working directory: " + target
        );
    }

    private static ShellResult ls(
            W128WebProject project,
            String rawPath
    ) {
        String target;

        try {
            target = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("ls", exception.getMessage());
        }

        if (project.file(target) != null) {
            return ok(
                    "ls",
                    target,
                    "Listed " + target
            );
        }

        if (!directoryExists(project, target)) {
            return fail(
                    "ls",
                    "Directory not found: " + target
            );
        }

        List<String> entries = listEntries(project, target);

        return ok(
                "ls " + target,
                entries.isEmpty()
                        ? "(empty)"
                        : String.join("\n", entries),
                "Listed " + target
        );
    }

    private static ShellResult tree(
            W128WebProject project,
            String rawPath
    ) {
        String target;

        try {
            target = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("tree", exception.getMessage());
        }

        if (!directoryExists(project, target)) {
            return fail(
                    "tree",
                    "Directory not found: " + target
            );
        }

        List<String> entries = treeEntries(project, target);

        return ok(
                "tree " + target,
                entries.isEmpty()
                        ? target + "\n(empty)"
                        : target + "\n" + String.join("\n", entries),
                "Tree printed: " + target
        );
    }

    private static ShellResult cat(
            W128WebProject project,
            String rawPath
    ) {
        if (rawPath == null || rawPath.isBlank()) {
            return fail(
                    "cat",
                    "Usage: cat <path>"
            );
        }

        String path;

        try {
            path = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("cat", exception.getMessage());
        }

        W128WebFile file = project.file(path);

        if (file == null) {
            return fail(
                    "cat",
                    "File not found: " + path
            );
        }

        String content = file.content();

        if (content.length() > 16_000) {
            content = content.substring(0, 16_000)
                    + "\n... output truncated";
        }

        return ok(
                "cat " + path,
                content,
                "Printed " + path
        );
    }

    private static ShellResult mkdir(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String rawPath,
            long nowMillis
    ) {
        if (rawPath == null || rawPath.isBlank()) {
            return fail(
                    "mkdir",
                    "Usage: mkdir <path>"
            );
        }

        String path;

        try {
            path = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("mkdir", exception.getMessage());
        }

        if ("/".equals(path)) {
            return ok(
                    "mkdir",
                    "/ already exists.",
                    "Directory already exists: /"
            );
        }

        if (protectedPath(path)) {
            return fail(
                    "mkdir",
                    "The /.vsia workspace directory is managed internally."
            );
        }

        if (project.file(path) != null) {
            return fail(
                    "mkdir",
                    "A file already exists at " + path
            );
        }

        W131WorkspaceState.addFolder(
                data,
                project,
                path,
                nowMillis
        );

        return ok(
                "mkdir",
                path,
                "Created directory: " + path
        );
    }

    private static ShellResult touch(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String rawPath,
            long nowMillis
    ) {
        if (rawPath == null || rawPath.isBlank()) {
            return fail(
                    "touch",
                    "Usage: touch <path>"
            );
        }

        String path;

        try {
            path = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("touch", exception.getMessage());
        }

        if ("/".equals(path) || protectedPath(path)) {
            return fail(
                    "touch",
                    "That path is reserved."
            );
        }

        if (directoryExists(project, path)
                && project.file(path) == null) {
            return fail(
                    "touch",
                    "A directory already exists at " + path
            );
        }

        W128WebFile existing = project.file(path);
        W128WebBuildResult result = data.putFile(
                actor,
                project.host(),
                path,
                existing == null ? "" : existing.content(),
                nowMillis
        );

        if (!result.success()) {
            return fail(
                    "touch",
                    result.message()
            );
        }

        W131WorkspaceState.addFolders(
                data,
                project,
                parentDirectories(path),
                nowMillis
        );

        return ok(
                "touch",
                path,
                "Touched " + path
        );
    }

    private static ShellResult copy(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String rawSource,
            String rawDestination,
            long nowMillis
    ) {
        String source;
        String destination;

        try {
            String cwd = W131WorkspaceState.cwd(project);
            source = resolvePath(cwd, rawSource);
            destination = resolvePath(cwd, rawDestination);
        } catch (IllegalArgumentException exception) {
            return fail("cp", exception.getMessage());
        }

        if (protectedPath(source)
                || protectedPath(destination)
                || "/".equals(source)) {
            return fail(
                    "cp",
                    "Copying the workspace root or /.vsia is not permitted."
            );
        }

        W128WebFile sourceFile = project.file(source);

        if (sourceFile != null) {
            String target = directoryExists(project, destination)
                    ? join(destination, baseName(source))
                    : destination;

            if (project.file(target) != null
                    || (directoryExists(project, target)
                    && project.file(target) == null)) {
                return fail(
                        "cp",
                        "Destination already exists: " + target
                );
            }

            W128WebBuildResult result = data.putFile(
                    actor,
                    project.host(),
                    target,
                    sourceFile.content(),
                    nowMillis
            );

            if (!result.success()) {
                return fail(
                        "cp",
                        result.message()
                );
            }

            W131WorkspaceState.addFolders(
                    data,
                    project,
                    parentDirectories(target),
                    nowMillis
            );

            return ok(
                    "cp",
                    source + " -> " + target,
                    "Copied " + source + " -> " + target
            );
        }

        if (!directoryExists(project, source)) {
            return fail(
                    "cp",
                    "Source not found: " + source
            );
        }

        String targetRoot = directoryExists(project, destination)
                ? join(destination, baseName(source))
                : destination;

        if (targetRoot.equals(source)
                || targetRoot.startsWith(source + "/")) {
            return fail(
                    "cp",
                    "A directory cannot be copied into itself."
            );
        }

        List<W128WebFile> sourceFiles = project.files()
                .values()
                .stream()
                .filter(file -> file.path().startsWith(source + "/"))
                .sorted(Comparator.comparing(W128WebFile::path))
                .toList();

        Map<String, W128WebFile> targets = new LinkedHashMap<>();

        for (W128WebFile file : sourceFiles) {
            String suffix = file.path().substring(source.length());
            String target = targetRoot + suffix;

            if (project.file(target) != null) {
                return fail(
                        "cp",
                        "Destination already exists: " + target
                );
            }

            targets.put(target, file);
        }

        int projectedFiles = project.files().size() + targets.size();

        if (projectedFiles > W128WebRegistrySavedData.MAX_FILES_PER_PROJECT) {
            return fail(
                    "cp",
                    "Project file limit would be exceeded."
            );
        }

        int extraCharacters = targets.values()
                .stream()
                .mapToInt(file -> file.content().length())
                .sum();

        if (project.totalCharacters() + extraCharacters
                > W128WebRegistrySavedData.MAX_PROJECT_CHARACTERS) {
            return fail(
                    "cp",
                    "Project character limit would be exceeded."
            );
        }

        for (Map.Entry<String, W128WebFile> entry : targets.entrySet()) {
            W128WebBuildResult result = data.putFile(
                    actor,
                    project.host(),
                    entry.getKey(),
                    entry.getValue().content(),
                    nowMillis
            );

            if (!result.success()) {
                return fail(
                        "cp",
                        "Copy stopped at "
                                + entry.getKey()
                                + ": "
                                + result.message()
                );
            }
        }

        LinkedHashSet<String> folders = new LinkedHashSet<>();
        folders.add(targetRoot);

        for (String folder : knownDirectories(project)) {
            if (folder.equals(source)
                    || folder.startsWith(source + "/")) {
                String suffix = folder.substring(source.length());
                folders.add(targetRoot + suffix);
            }
        }

        W131WorkspaceState.addFolders(
                data,
                project,
                folders,
                nowMillis
        );

        return ok(
                "cp",
                source + " -> " + targetRoot,
                "Copied directory " + source + " -> " + targetRoot
        );
    }

    private static ShellResult move(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String rawSource,
            String rawDestination,
            long nowMillis
    ) {
        String source;

        try {
            source = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawSource
            );
        } catch (IllegalArgumentException exception) {
            return fail("mv", exception.getMessage());
        }

        ShellResult copied = copy(
                data,
                project,
                actor,
                rawSource,
                rawDestination,
                nowMillis
        );

        if (!copied.success()) {
            return copied;
        }

        ShellResult removed = removeResolved(
                data,
                project,
                actor,
                source,
                nowMillis
        );

        if (!removed.success()) {
            return fail(
                    "mv",
                    "Copy succeeded but source cleanup failed: "
                            + removed.message()
            );
        }

        return ok(
                "mv",
                copied.payload(),
                "Moved " + copied.payload()
        );
    }

    private static ShellResult remove(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String rawPath,
            long nowMillis
    ) {
        String path;

        try {
            path = resolvePath(
                    W131WorkspaceState.cwd(project),
                    rawPath
            );
        } catch (IllegalArgumentException exception) {
            return fail("rm", exception.getMessage());
        }

        return removeResolved(
                data,
                project,
                actor,
                path,
                nowMillis
        );
    }

    private static ShellResult removeResolved(
            W128WebRegistrySavedData data,
            W128WebProject project,
            UUID actor,
            String path,
            long nowMillis
    ) {
        if ("/".equals(path)
                || protectedPath(path)) {
            return fail(
                    "rm",
                    "Removing the workspace root or /.vsia is not permitted."
            );
        }

        W128WebFile file = project.file(path);

        if (file != null) {
            W128WebBuildResult result = data.removeFile(
                    actor,
                    project.host(),
                    path,
                    nowMillis
            );

            return result.success()
                    ? ok(
                    "rm",
                    path,
                    "Removed " + path
            )
                    : fail(
                    "rm",
                    result.message()
            );
        }

        if (!directoryExists(project, path)) {
            return fail(
                    "rm",
                    "Path not found: " + path
            );
        }

        String prefix = path + "/";

        List<String> files = project.files()
                .keySet()
                .stream()
                .filter(candidate -> candidate.startsWith(prefix))
                .sorted(Comparator.reverseOrder())
                .toList();

        for (String candidate : files) {
            if (protectedPath(candidate)) {
                continue;
            }

            W128WebBuildResult result = data.removeFile(
                    actor,
                    project.host(),
                    candidate,
                    nowMillis
            );

            if (!result.success()) {
                return fail(
                        "rm",
                        "Failed while removing "
                                + candidate
                                + ": "
                                + result.message()
                );
            }
        }

        W131WorkspaceState.removeFolderTree(
                data,
                project,
                path,
                nowMillis
        );

        return ok(
                "rm",
                path,
                "Removed directory " + path
        );
    }

    static List<String> tokenize(String input) {
        List<String> output = new ArrayList<>();

        if (input == null || input.isBlank()) {
            return output;
        }

        StringBuilder current = new StringBuilder();
        char quote = '\0';
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && quote != '\'') {
                escaped = true;
                continue;
            }

            if (quote != '\0') {
                if (c == quote) {
                    quote = '\0';
                } else {
                    current.append(c);
                }
                continue;
            }

            if (c == '"' || c == '\'') {
                quote = c;
                continue;
            }

            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    output.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            output.add(current.toString());
        }

        return List.copyOf(output);
    }

    static LinkedHashSet<String> knownDirectories(
            W128WebProject project
    ) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add("/");
        result.addAll(W131WorkspaceState.folders(project));

        for (String file : project.files().keySet()) {
            result.addAll(parentDirectories(file));
        }

        return result;
    }

    static List<String> parentDirectories(String path) {
        List<String> output = new ArrayList<>();
        String current = parent(path);

        while (!"/".equals(current)) {
            output.add(current);
            current = parent(current);
        }

        return List.copyOf(output);
    }

    static String parent(String path) {
        String normalized = W131WorkspaceState.normalizeDirectory(path);
        int slash = normalized.lastIndexOf('/');

        if (slash <= 0) {
            return "/";
        }

        return normalized.substring(0, slash);
    }

    static String baseName(String path) {
        String normalized = W131WorkspaceState.normalizeDirectory(path);

        if ("/".equals(normalized)) {
            return "/";
        }

        int slash = normalized.lastIndexOf('/');
        return slash >= 0
                ? normalized.substring(slash + 1)
                : normalized;
    }

    static String join(String directory, String child) {
        String base = W131WorkspaceState.normalizeDirectory(directory);

        if ("/".equals(base)) {
            return "/" + child;
        }

        return base + "/" + child;
    }

    private static boolean protectedPath(String path) {
        return W130Workspace.internalPath(path);
    }

    private static String indent(String relative) {
        int depth = 0;

        for (int i = 0; i < relative.length(); i++) {
            if (relative.charAt(i) == '/') {
                depth++;
            }
        }

        return "  ".repeat(Math.max(0, depth));
    }

    private static ShellResult ok(
            String title,
            String payload,
            String message
    ) {
        return new ShellResult(
                true,
                title,
                payload == null ? "" : payload,
                message == null ? "" : message
        );
    }

    private static ShellResult fail(
            String title,
            String message
    ) {
        return new ShellResult(
                false,
                title,
                message == null ? "" : message,
                message == null ? "" : message
        );
    }

    public record ShellResult(
            boolean success,
            String title,
            String payload,
            String message
    ) {
    }
}
