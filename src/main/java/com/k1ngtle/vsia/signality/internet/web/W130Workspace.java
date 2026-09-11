package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class W130Workspace {
    public static final String INTERNAL_ROOT = "/.vsia/";
    public static final String SETTINGS_PATH = INTERNAL_ROOT + "settings.json";
    public static final String LAUNCH_PATH = INTERNAL_ROOT + "launch.json";
    public static final String TASKS_PATH = INTERNAL_ROOT + "tasks.json";
    public static final String STATE_PATH = INTERNAL_ROOT + "workspace.json";
    public static final String SOURCE_CONTROL_PATH = INTERNAL_ROOT + "source-control.json";

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private W130Workspace() {
    }

    public static boolean internalPath(String path) {
        return path != null
                && (path.equals("/.vsia") || path.startsWith(INTERNAL_ROOT));
    }

    public static void ensureDefaults(
            W128WebRegistrySavedData data,
            W128WebProject project,
            long nowMillis
    ) {
        boolean changed = false;

        changed |= putIfMissing(
                project,
                SETTINGS_PATH,
                defaultSettings(),
                nowMillis
        );
        changed |= putIfMissing(
                project,
                LAUNCH_PATH,
                defaultLaunch(),
                nowMillis
        );
        changed |= putIfMissing(
                project,
                TASKS_PATH,
                defaultTasks(),
                nowMillis
        );
        changed |= putIfMissing(
                project,
                STATE_PATH,
                defaultState(),
                nowMillis
        );

        if (changed) {
            data.setDirty();
        }
    }

    public static Map<String, List<Integer>> breakpoints(
            W128WebProject project
    ) {
        JsonObject root = stateRoot(project);
        JsonObject object = object(root, "breakpoints");
        Map<String, List<Integer>> result = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (!entry.getValue().isJsonArray()) {
                continue;
            }

            Set<Integer> unique = new LinkedHashSet<>();
            for (JsonElement line : entry.getValue().getAsJsonArray()) {
                try {
                    int value = line.getAsInt();
                    if (value > 0) {
                        unique.add(value);
                    }
                } catch (RuntimeException ignored) {
                }
            }

            if (!unique.isEmpty()) {
                result.put(entry.getKey(), List.copyOf(unique));
            }
        }

        return result;
    }

    public static List<String> watches(W128WebProject project) {
        JsonObject root = stateRoot(project);
        JsonArray array = array(root, "watches");
        List<String> result = new ArrayList<>();

        for (JsonElement element : array) {
            try {
                String value = element.getAsString().trim();
                if (!value.isEmpty() && !result.contains(value)) {
                    result.add(value);
                }
            } catch (RuntimeException ignored) {
            }
        }

        return List.copyOf(result);
    }

    public static boolean toggleBreakpoint(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String path,
            int oneBasedLine,
            long nowMillis
    ) {
        if (path == null || path.isBlank() || oneBasedLine <= 0) {
            return false;
        }

        JsonObject root = stateRoot(project);
        JsonObject breakpoints = object(root, "breakpoints");
        JsonArray old = breakpoints.has(path) && breakpoints.get(path).isJsonArray()
                ? breakpoints.getAsJsonArray(path)
                : new JsonArray();

        Set<Integer> lines = new LinkedHashSet<>();
        for (JsonElement element : old) {
            try {
                int value = element.getAsInt();
                if (value > 0) {
                    lines.add(value);
                }
            } catch (RuntimeException ignored) {
            }
        }

        boolean added;
        if (lines.contains(oneBasedLine)) {
            lines.remove(oneBasedLine);
            added = false;
        } else {
            lines.add(oneBasedLine);
            added = true;
        }

        JsonArray replacement = new JsonArray();
        lines.stream().sorted().forEach(replacement::add);

        if (replacement.size() == 0) {
            breakpoints.remove(path);
        } else {
            breakpoints.add(path, replacement);
        }

        saveState(data, project, root, nowMillis);
        return added;
    }

    public static void addWatch(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String expression,
            long nowMillis
    ) {
        String value = expression == null ? "" : expression.trim();
        if (value.isEmpty()) {
            return;
        }

        JsonObject root = stateRoot(project);
        List<String> values = new ArrayList<>();
        for (JsonElement element : array(root, "watches")) {
            try {
                String current = element.getAsString().trim();
                if (!current.isEmpty() && !values.contains(current)) {
                    values.add(current);
                }
            } catch (RuntimeException ignored) {
            }
        }

        if (!values.contains(value)) {
            values.add(value);
        }

        JsonArray output = new JsonArray();
        values.stream().limit(32).forEach(output::add);
        root.add("watches", output);
        saveState(data, project, root, nowMillis);
    }

    public static void removeWatch(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String expression,
            long nowMillis
    ) {
        String value = expression == null ? "" : expression.trim();
        JsonObject root = stateRoot(project);
        JsonArray output = new JsonArray();

        for (JsonElement element : array(root, "watches")) {
            try {
                String current = element.getAsString().trim();
                if (!current.isEmpty() && !current.equals(value)) {
                    output.add(current);
                }
            } catch (RuntimeException ignored) {
            }
        }

        root.add("watches", output);
        saveState(data, project, root, nowMillis);
    }

    public static LaunchConfig launchConfig(
            W128WebProject project,
            String activePath
    ) {
        W128WebFile file = project.file(LAUNCH_PATH);
        if (file == null) {
            return LaunchConfig.defaultFor(activePath);
        }

        try {
            JsonObject root = JsonParser.parseString(file.content()).getAsJsonObject();
            JsonArray configs = array(root, "configurations");
            if (configs.size() == 0) {
                return LaunchConfig.defaultFor(activePath);
            }

            JsonObject config = configs.get(0).getAsJsonObject();
            String name = string(config, "name", "Run current file");
            String program = expand(
                    string(config, "program", "${file}"),
                    activePath,
                    project.host()
            );
            boolean stopOnEntry = bool(config, "stopOnEntry", true);
            String cwd = expand(
                    string(config, "cwd", "${workspaceFolder}"),
                    activePath,
                    project.host()
            );
            List<String> args = new ArrayList<>();
            Map<String, String> environment = new LinkedHashMap<>();

            if (config.has("args") && config.get("args").isJsonArray()) {
                for (JsonElement element : config.getAsJsonArray("args")) {
                    args.add(expand(element.getAsString(), activePath, project.host()));
                }
            }

            if (config.has("env") && config.get("env").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : config.getAsJsonObject("env").entrySet()) {
                    try {
                        environment.put(
                                entry.getKey(),
                                expand(entry.getValue().getAsString(), activePath, project.host())
                        );
                    } catch (RuntimeException ignored) {
                    }
                }
            }

            return new LaunchConfig(
                    name,
                    program,
                    List.copyOf(args),
                    cwd,
                    Map.copyOf(environment),
                    stopOnEntry
            );
        } catch (RuntimeException ignored) {
            return LaunchConfig.defaultFor(activePath);
        }
    }

    public static String expand(String raw, String activePath, String host) {
        String value = raw == null ? "" : raw;
        return value
                .replace("${file}", activePath == null ? "" : activePath)
                .replace("${host}", host == null ? "" : host)
                .replace("${workspaceFolder}", "/");
    }

    static JsonObject parseObject(String raw) {
        try {
            JsonElement element = JsonParser.parseString(raw == null ? "{}" : raw);
            return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    static void writeInternal(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String path,
            String content,
            long nowMillis
    ) {
        project.putFile(new W128WebFile(
                path,
                content == null ? "" : content,
                W128MimeTypes.forPath(path),
                nowMillis,
                false
        ));
        data.setDirty();
    }

    private static JsonObject stateRoot(W128WebProject project) {
        W128WebFile file = project.file(STATE_PATH);
        JsonObject root = file == null
                ? new JsonObject()
                : parseObject(file.content());

        if (!root.has("version")) {
            root.addProperty("version", 1);
        }
        if (!root.has("breakpoints") || !root.get("breakpoints").isJsonObject()) {
            root.add("breakpoints", new JsonObject());
        }
        if (!root.has("watches") || !root.get("watches").isJsonArray()) {
            root.add("watches", new JsonArray());
        }

        return root;
    }

    private static void saveState(
            W128WebRegistrySavedData data,
            W128WebProject project,
            JsonObject root,
            long nowMillis
    ) {
        writeInternal(data, project, STATE_PATH, GSON.toJson(root), nowMillis);
    }

    private static boolean putIfMissing(
            W128WebProject project,
            String path,
            String content,
            long nowMillis
    ) {
        if (project.file(path) != null) {
            return false;
        }

        project.putFile(new W128WebFile(
                path,
                content,
                W128MimeTypes.forPath(path),
                nowMillis,
                false
        ));
        return true;
    }

    private static JsonObject object(JsonObject root, String name) {
        if (root.has(name) && root.get(name).isJsonObject()) {
            return root.getAsJsonObject(name);
        }
        JsonObject value = new JsonObject();
        root.add(name, value);
        return value;
    }

    private static JsonArray array(JsonObject root, String name) {
        if (root.has(name) && root.get(name).isJsonArray()) {
            return root.getAsJsonArray(name);
        }
        JsonArray value = new JsonArray();
        root.add(name, value);
        return value;
    }

    private static String string(JsonObject object, String name, String fallback) {
        try {
            return object.has(name) ? object.get(name).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject object, String name, boolean fallback) {
        try {
            return object.has(name) ? object.get(name).getAsBoolean() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String defaultSettings() {
        return """
                {
                  "version": 1,
                  "editor.formatOnSave": false,
                  "editor.wordWrap": false,
                  "editor.tabSize": 4,
                  "files.autoSave": false,
                  "debug.console": true,
                  "sourceControl.enabled": true
                }
                """;
    }

    private static String defaultLaunch() {
        return """
                {
                  "version": 1,
                  "configurations": [
                    {
                      "name": "Run current file",
                      "program": "${file}",
                      "args": [],
                      "cwd": "${workspaceFolder}",
                      "env": {},
                      "stopOnEntry": true
                    }
                  ]
                }
                """;
    }

    private static String defaultTasks() {
        return """
                {
                  "version": 1,
                  "tasks": [
                    {
                      "label": "check",
                      "command": "check ${file}",
                      "dependsOn": []
                    },
                    {
                      "label": "build",
                      "command": "build",
                      "dependsOn": ["check"]
                    },
                    {
                      "label": "selftest",
                      "command": "w130 selftest",
                      "dependsOn": []
                    }
                  ]
                }
                """;
    }

    private static String defaultState() {
        return """
                {
                  "version": 1,
                  "breakpoints": {},
                  "watches": []
                }
                """;
    }

    public record LaunchConfig(
            String name,
            String program,
            List<String> args,
            String cwd,
            Map<String, String> environment,
            boolean stopOnEntry
    ) {
        public LaunchConfig {
            name = name == null ? "Run current file" : name;
            program = program == null ? "" : program;
            args = args == null ? List.of() : List.copyOf(args);
            cwd = cwd == null || cwd.isBlank() ? "/" : cwd;
            environment = environment == null ? Map.of() : Map.copyOf(environment);
        }

        public static LaunchConfig defaultFor(String path) {
            return new LaunchConfig(
                    "Run current file",
                    path == null ? "" : path,
                    List.of(),
                    "/",
                    Map.of(),
                    true
            );
        }
    }
}
