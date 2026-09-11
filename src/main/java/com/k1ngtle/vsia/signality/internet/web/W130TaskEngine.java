package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class W130TaskEngine {
    private static final int MAX_TASKS = 64;
    private static final int MAX_DEPENDENCY_DEPTH = 16;

    private W130TaskEngine() {
    }

    public static List<TaskDefinition> tasks(W128WebProject project) {
        W128WebFile file = project.file(W130Workspace.TASKS_PATH);
        if (file == null) {
            return List.of();
        }

        JsonObject root = W130Workspace.parseObject(file.content());
        if (!root.has("tasks") || !root.get("tasks").isJsonArray()) {
            return List.of();
        }

        List<TaskDefinition> output = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("tasks")) {
            if (output.size() >= MAX_TASKS || !element.isJsonObject()) {
                break;
            }

            JsonObject object = element.getAsJsonObject();
            String label = string(object, "label", "").trim();
            String command = string(object, "command", "").trim();
            if (label.isEmpty() || command.isEmpty()) {
                continue;
            }

            List<String> dependsOn = new ArrayList<>();
            if (object.has("dependsOn") && object.get("dependsOn").isJsonArray()) {
                JsonArray array = object.getAsJsonArray("dependsOn");
                for (JsonElement dependency : array) {
                    try {
                        String value = dependency.getAsString().trim();
                        if (!value.isEmpty() && !dependsOn.contains(value)) {
                            dependsOn.add(value);
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
            }

            output.add(new TaskDefinition(label, command, List.copyOf(dependsOn)));
        }

        return List.copyOf(output);
    }

    public static ResolvedTask resolve(
            W128WebProject project,
            String requestedLabel,
            String activePath
    ) {
        String label = requestedLabel == null ? "" : requestedLabel.trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("Task label is empty.");
        }

        Map<String, TaskDefinition> index = new LinkedHashMap<>();
        for (TaskDefinition task : tasks(project)) {
            index.put(task.label(), task);
        }

        if (!index.containsKey(label)) {
            throw new IllegalArgumentException("Task not found: " + label);
        }

        List<String> commands = new ArrayList<>();
        List<String> order = new ArrayList<>();
        resolveRecursive(
                label,
                project,
                activePath,
                index,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                commands,
                order,
                0
        );

        return new ResolvedTask(label, List.copyOf(order), List.copyOf(commands));
    }

    private static void resolveRecursive(
            String label,
            W128WebProject project,
            String activePath,
            Map<String, TaskDefinition> index,
            Set<String> visiting,
            Set<String> completed,
            List<String> commands,
            List<String> order,
            int depth
    ) {
        if (completed.contains(label)) {
            return;
        }
        if (depth > MAX_DEPENDENCY_DEPTH) {
            throw new IllegalArgumentException("Task dependency depth exceeded at " + label);
        }
        if (!visiting.add(label)) {
            throw new IllegalArgumentException("Task dependency cycle detected at " + label);
        }

        TaskDefinition task = index.get(label);
        if (task == null) {
            throw new IllegalArgumentException("Task dependency not found: " + label);
        }

        for (String dependency : task.dependsOn()) {
            resolveRecursive(
                    dependency,
                    project,
                    activePath,
                    index,
                    visiting,
                    completed,
                    commands,
                    order,
                    depth + 1
            );
        }

        visiting.remove(label);
        completed.add(label);
        order.add(label);
        commands.add(W130Workspace.expand(task.command(), activePath, project.host()));
    }

    private static String string(JsonObject object, String name, String fallback) {
        try {
            return object.has(name) ? object.get(name).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public record TaskDefinition(
            String label,
            String command,
            List<String> dependsOn
    ) {
        public TaskDefinition {
            label = label == null ? "" : label;
            command = command == null ? "" : command;
            dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
        }
    }

    public record ResolvedTask(
            String label,
            List<String> order,
            List<String> commands
    ) {
        public ResolvedTask {
            label = label == null ? "" : label;
            order = order == null ? List.of() : List.copyOf(order);
            commands = commands == null ? List.of() : List.copyOf(commands);
        }
    }
}
