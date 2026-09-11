package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class W131WorkspaceState {
    public static final int VERSION = 2;

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private W131WorkspaceState() {
    }

    public static void ensureDefaults(
            W128WebRegistrySavedData data,
            W128WebProject project,
            long nowMillis
    ) {
        W128WebFile file = project.file(W130Workspace.STATE_PATH);
        String before = file == null ? "" : file.content();
        JsonObject root = root(project);
        String after = GSON.toJson(root);

        if (file == null || !after.equals(before)) {
            W130Workspace.writeInternal(
                    data,
                    project,
                    W130Workspace.STATE_PATH,
                    after,
                    nowMillis
            );
        }
    }

    public static String cwd(W128WebProject project) {
        JsonObject root = root(project);
        try {
            return normalizeDirectory(root.get("cwd").getAsString());
        } catch (RuntimeException ignored) {
            return "/";
        }
    }

    public static void setCwd(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String cwd,
            long nowMillis
    ) {
        JsonObject root = root(project);
        root.addProperty("cwd", normalizeDirectory(cwd));
        save(data, project, root, nowMillis);
    }

    public static Set<String> folders(W128WebProject project) {
        JsonObject root = root(project);
        LinkedHashSet<String> result = new LinkedHashSet<>();

        for (JsonElement element : root.getAsJsonArray("folders")) {
            try {
                String value = normalizeDirectory(element.getAsString());
                if (!"/".equals(value) && !W130Workspace.internalPath(value)) {
                    result.add(value);
                }
            } catch (RuntimeException ignored) {
            }
        }

        return Set.copyOf(result);
    }

    public static void addFolder(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String folder,
            long nowMillis
    ) {
        addFolders(
                data,
                project,
                List.of(folder),
                nowMillis
        );
    }

    public static void addFolders(
            W128WebRegistrySavedData data,
            W128WebProject project,
            Collection<String> folders,
            long nowMillis
    ) {
        JsonObject root = root(project);
        LinkedHashSet<String> values = new LinkedHashSet<>(folders(project));

        if (folders != null) {
            for (String folder : folders) {
                String normalized = normalizeDirectory(folder);
                if ("/".equals(normalized) || W130Workspace.internalPath(normalized)) {
                    continue;
                }

                addParents(values, normalized);
                values.add(normalized);
            }
        }

        JsonArray array = new JsonArray();
        values.stream().sorted().forEach(array::add);
        root.add("folders", array);
        save(data, project, root, nowMillis);
    }

    public static void removeFolderTree(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String folder,
            long nowMillis
    ) {
        String normalized = normalizeDirectory(folder);
        String prefix = normalized.endsWith("/")
                ? normalized
                : normalized + "/";

        JsonObject root = root(project);
        JsonArray array = new JsonArray();

        for (String existing : folders(project)) {
            if (!existing.equals(normalized)
                    && !existing.startsWith(prefix)) {
                array.add(existing);
            }
        }

        root.add("folders", array);

        String cwd = cwd(project);
        if (cwd.equals(normalized) || cwd.startsWith(prefix)) {
            root.addProperty("cwd", "/");
        }

        save(data, project, root, nowMillis);
    }

    public static List<String> recentFiles(W128WebProject project) {
        JsonObject root = root(project);
        List<String> result = new ArrayList<>();

        for (JsonElement element : root.getAsJsonArray("recentFiles")) {
            try {
                String value = element.getAsString().trim();
                if (!value.isEmpty()
                        && !W130Workspace.internalPath(value)
                        && project.file(value) != null
                        && !result.contains(value)) {
                    result.add(value);
                }
            } catch (RuntimeException ignored) {
            }
        }

        return List.copyOf(result);
    }

    public static void rememberFile(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String path,
            long nowMillis
    ) {
        if (path == null
                || path.isBlank()
                || W130Workspace.internalPath(path)
                || project.file(path) == null) {
            return;
        }

        JsonObject root = root(project);
        List<String> values = new ArrayList<>(recentFiles(project));
        values.remove(path);
        values.add(0, path);

        JsonArray array = new JsonArray();
        values.stream().limit(12).forEach(array::add);
        root.add("recentFiles", array);
        save(data, project, root, nowMillis);
    }

    static JsonObject root(W128WebProject project) {
        W128WebFile file = project.file(W130Workspace.STATE_PATH);
        JsonObject root = file == null
                ? new JsonObject()
                : W130Workspace.parseObject(file.content());

        root.addProperty("version", VERSION);

        if (!root.has("breakpoints") || !root.get("breakpoints").isJsonObject()) {
            root.add("breakpoints", new JsonObject());
        }

        if (!root.has("watches") || !root.get("watches").isJsonArray()) {
            root.add("watches", new JsonArray());
        }

        if (!root.has("folders") || !root.get("folders").isJsonArray()) {
            root.add("folders", new JsonArray());
        }

        if (!root.has("cwd") || !root.get("cwd").isJsonPrimitive()) {
            root.addProperty("cwd", "/");
        }

        if (!root.has("recentFiles") || !root.get("recentFiles").isJsonArray()) {
            root.add("recentFiles", new JsonArray());
        }

        return root;
    }

    static String normalizeDirectory(String raw) {
        String value = raw == null ? "/" : raw.trim().replace('\\', '/');

        if (value.isBlank()) {
            return "/";
        }

        if (!value.startsWith("/")) {
            value = "/" + value;
        }

        List<String> segments = new ArrayList<>();

        for (String segment : value.split("/")) {
            if (segment.isBlank() || ".".equals(segment)) {
                continue;
            }

            if ("..".equals(segment)) {
                if (!segments.isEmpty()) {
                    segments.remove(segments.size() - 1);
                }
                continue;
            }

            segments.add(segment);
        }

        return segments.isEmpty()
                ? "/"
                : "/" + String.join("/", segments);
    }

    private static void addParents(Set<String> folders, String path) {
        String current = parent(path);

        while (!"/".equals(current)) {
            folders.add(current);
            current = parent(current);
        }
    }

    private static String parent(String path) {
        String normalized = normalizeDirectory(path);
        int slash = normalized.lastIndexOf('/');

        if (slash <= 0) {
            return "/";
        }

        return normalized.substring(0, slash);
    }

    private static void save(
            W128WebRegistrySavedData data,
            W128WebProject project,
            JsonObject root,
            long nowMillis
    ) {
        W130Workspace.writeInternal(
                data,
                project,
                W130Workspace.STATE_PATH,
                GSON.toJson(root),
                nowMillis
        );
    }
}
