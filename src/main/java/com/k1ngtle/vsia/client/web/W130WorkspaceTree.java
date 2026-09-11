package com.k1ngtle.vsia.client.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class W130WorkspaceTree {
    private W130WorkspaceTree() {
    }

    public static List<Row> rows(
            Collection<String> paths,
            Set<String> expandedFolders
    ) {
        Node root = new Node("/", "/", true);

        if (paths != null) {
            for (String raw : paths) {
                add(root, normalize(raw));
            }
        }

        List<Row> output = new ArrayList<>();
        append(root, expandedFolders == null ? Set.of() : expandedFolders, output, 0);
        return List.copyOf(output);
    }

    private static void add(Node root, String path) {
        if (path.isBlank() || "/".equals(path)) {
            return;
        }

        String[] parts = path.substring(1).split("/");
        Node current = root;
        StringBuilder full = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }

            full.append('/').append(part);
            boolean folder = i < parts.length - 1;
            String key = full.toString();
            current = current.children.computeIfAbsent(
                    part,
                    ignored -> new Node(key, part, folder)
            );
            if (folder) {
                current.folder = true;
            }
        }
    }

    private static void append(
            Node root,
            Set<String> expanded,
            List<Row> output,
            int depth
    ) {
        List<Node> children = root.children.values().stream()
                .sorted(Comparator.comparing((Node node) -> !node.folder)
                        .thenComparing(node -> node.name.toLowerCase()))
                .toList();

        for (Node child : children) {
            output.add(new Row(child.path, child.name, child.folder, depth));
            if (child.folder && expanded.contains(child.path)) {
                append(child, expanded, output, depth + 1);
            }
        }
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String value = path.replace('\\', '/');
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.contains("//")) {
            value = value.replace("//", "/");
        }
        return value;
    }

    public record Row(
            String path,
            String label,
            boolean folder,
            int depth
    ) {
        public Row {
            path = path == null ? "" : path;
            label = label == null ? "" : label;
            depth = Math.max(0, depth);
        }
    }

    private static final class Node {
        private final String path;
        private final String name;
        private boolean folder;
        private final Map<String, Node> children = new LinkedHashMap<>();

        private Node(String path, String name, boolean folder) {
            this.path = path;
            this.name = name;
            this.folder = folder;
        }
    }
}
