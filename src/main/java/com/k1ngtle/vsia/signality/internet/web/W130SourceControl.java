package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class W130SourceControl {
    private static final int MAX_HISTORY = 20;
    private static final int MAX_DIFF_LINES = 400;
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private W130SourceControl() {
    }

    public static void ensureRepository(
            W128WebRegistrySavedData data,
            W128WebProject project,
            long nowMillis
    ) {
        if (project.file(W130Workspace.SOURCE_CONTROL_PATH) != null) {
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("head", 0);
        root.add("baseline", baselineObject(project));
        root.add("staged", new JsonArray());
        root.add("history", new JsonArray());
        persist(data, project, root, nowMillis);
    }

    public static W130ScmSnapshot snapshot(W128WebProject project) {
        State state = state(project);
        Map<String, String> current = currentFiles(project);
        Set<String> all = new LinkedHashSet<>();
        all.addAll(state.baseline().keySet());
        all.addAll(current.keySet());

        List<W130ScmSnapshot.Change> changes = new ArrayList<>();
        for (String path : all.stream().sorted().toList()) {
            boolean inBase = state.baseline().containsKey(path);
            boolean inCurrent = current.containsKey(path);
            String kind;

            if (!inBase && inCurrent) {
                kind = "A";
            } else if (inBase && !inCurrent) {
                kind = "D";
            } else if (!state.baseline().get(path).equals(current.get(path))) {
                kind = "M";
            } else {
                continue;
            }

            changes.add(new W130ScmSnapshot.Change(
                    kind,
                    path,
                    state.staged().contains(path)
            ));
        }

        return new W130ScmSnapshot(
                state.head(),
                List.copyOf(changes),
                state.history()
        );
    }

    public static W128WebBuildResult stage(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String path,
            long nowMillis
    ) {
        State state = state(project);
        W130ScmSnapshot snapshot = snapshot(project);
        Set<String> changed = new LinkedHashSet<>();
        snapshot.changes().forEach(change -> changed.add(change.path()));

        Set<String> staged = new LinkedHashSet<>(state.staged());
        if ("*".equals(path)) {
            staged.addAll(changed);
        } else if (changed.contains(path)) {
            staged.add(path);
        } else {
            return W128WebBuildResult.fail("No source-control change for " + path);
        }

        writeState(data, project, state.withStaged(staged), nowMillis);
        return W128WebBuildResult.ok("Staged " + ("*".equals(path) ? changed.size() + " change(s)" : path));
    }

    public static W128WebBuildResult unstage(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String path,
            long nowMillis
    ) {
        State state = state(project);
        Set<String> staged = new LinkedHashSet<>(state.staged());

        if ("*".equals(path)) {
            staged.clear();
        } else {
            staged.remove(path);
        }

        writeState(data, project, state.withStaged(staged), nowMillis);
        return W128WebBuildResult.ok("Unstaged " + ("*".equals(path) ? "all changes" : path));
    }

    public static W128WebBuildResult commit(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String rawMessage,
            long nowMillis
    ) {
        State state = state(project);
        if (state.staged().isEmpty()) {
            return W128WebBuildResult.fail("Nothing is staged for commit.");
        }

        Map<String, String> baseline = new LinkedHashMap<>(state.baseline());
        Map<String, String> current = currentFiles(project);

        for (String path : state.staged()) {
            if (current.containsKey(path)) {
                baseline.put(path, current.get(path));
            } else {
                baseline.remove(path);
            }
        }

        int nextHead = state.head() + 1;
        String message = rawMessage == null ? "" : rawMessage.trim();
        if (message.isEmpty()) {
            message = "Commit " + nextHead;
        }
        if (message.length() > 120) {
            message = message.substring(0, 120);
        }

        List<W130ScmSnapshot.CommitInfo> history = new ArrayList<>(state.history());
        history.add(0, new W130ScmSnapshot.CommitInfo(nextHead, message, nowMillis));
        if (history.size() > MAX_HISTORY) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        }

        State next = new State(
                nextHead,
                baseline,
                Set.of(),
                List.copyOf(history)
        );
        writeState(data, project, next, nowMillis);
        return W128WebBuildResult.ok(
                "Committed " + state.staged().size() + " change(s) as #" + nextHead + ": " + message
        );
    }

    public static W128WebBuildResult revert(
            W128WebRegistrySavedData data,
            W128WebProject project,
            String requestedPath,
            long nowMillis
    ) {
        State state = state(project);
        W130ScmSnapshot snapshot = snapshot(project);
        Set<String> targets = new LinkedHashSet<>();

        if ("*".equals(requestedPath)) {
            snapshot.changes().forEach(change -> targets.add(change.path()));
        } else {
            targets.add(requestedPath);
        }

        int reverted = 0;
        for (String path : targets) {
            if (path == null || path.isBlank() || W130Workspace.internalPath(path)) {
                continue;
            }

            if (state.baseline().containsKey(path)) {
                project.putFile(new W128WebFile(
                        path,
                        state.baseline().get(path),
                        W128MimeTypes.forPath(path),
                        nowMillis,
                        false
                ));
                reverted++;
            } else if (project.file(path) != null) {
                project.removeFile(path, nowMillis);
                reverted++;
            }
        }

        Set<String> staged = new LinkedHashSet<>(state.staged());
        staged.removeAll(targets);
        writeState(data, project, state.withStaged(staged), nowMillis);

        if (reverted > 0) {
            project.setPublished(false, nowMillis);
            data.setDirty();
        }

        return W128WebBuildResult.ok("Reverted " + reverted + " file(s) to HEAD.");
    }

    public static String diff(W128WebProject project, String path) {
        State state = state(project);
        String before = state.baseline().get(path);
        W128WebFile file = project.file(path);
        String after = file == null ? null : file.content();

        if (before == null && after == null) {
            return "No tracked content for " + path;
        }

        String[] left = lines(before == null ? "" : before);
        String[] right = lines(after == null ? "" : after);

        StringBuilder out = new StringBuilder();
        out.append("--- HEAD ").append(path).append('\n');
        out.append("+++ WORKTREE ").append(path).append('\n');

        int max = Math.min(Math.max(left.length, right.length), MAX_DIFF_LINES);
        for (int i = 0; i < max; i++) {
            String a = i < left.length ? left[i] : null;
            String b = i < right.length ? right[i] : null;

            if (a != null && b != null && a.equals(b)) {
                out.append("  ").append(a).append('\n');
            } else {
                if (a != null) {
                    out.append("- ").append(a).append('\n');
                }
                if (b != null) {
                    out.append("+ ").append(b).append('\n');
                }
            }
        }

        if (Math.max(left.length, right.length) > MAX_DIFF_LINES) {
            out.append("... diff truncated after ").append(MAX_DIFF_LINES).append(" lines\n");
        }

        return out.toString();
    }

    private static State state(W128WebProject project) {
        W128WebFile file = project.file(W130Workspace.SOURCE_CONTROL_PATH);
        if (file == null) {
            return new State(0, currentFiles(project), Set.of(), List.of());
        }

        JsonObject root = W130Workspace.parseObject(file.content());
        int head = integer(root, "head", 0);
        Map<String, String> baseline = new LinkedHashMap<>();
        Set<String> staged = new LinkedHashSet<>();
        List<W130ScmSnapshot.CommitInfo> history = new ArrayList<>();

        if (root.has("baseline") && root.get("baseline").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("baseline").entrySet()) {
                try {
                    baseline.put(entry.getKey(), entry.getValue().getAsString());
                } catch (RuntimeException ignored) {
                }
            }
        }

        if (root.has("staged") && root.get("staged").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("staged")) {
                try {
                    String value = element.getAsString();
                    if (!value.isBlank()) {
                        staged.add(value);
                    }
                } catch (RuntimeException ignored) {
                }
            }
        }

        if (root.has("history") && root.get("history").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("history")) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                history.add(new W130ScmSnapshot.CommitInfo(
                        integer(object, "id", 0),
                        string(object, "message", ""),
                        longValue(object, "atMillis", 0L)
                ));
            }
        }

        return new State(head, baseline, staged, List.copyOf(history));
    }

    private static void writeState(
            W128WebRegistrySavedData data,
            W128WebProject project,
            State state,
            long nowMillis
    ) {
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.addProperty("head", state.head());

        JsonObject baseline = new JsonObject();
        state.baseline().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> baseline.addProperty(entry.getKey(), entry.getValue()));
        root.add("baseline", baseline);

        JsonArray staged = new JsonArray();
        state.staged().stream().sorted().forEach(staged::add);
        root.add("staged", staged);

        JsonArray history = new JsonArray();
        for (W130ScmSnapshot.CommitInfo commit : state.history()) {
            JsonObject object = new JsonObject();
            object.addProperty("id", commit.id());
            object.addProperty("message", commit.message());
            object.addProperty("atMillis", commit.atMillis());
            history.add(object);
        }
        root.add("history", history);

        persist(data, project, root, nowMillis);
    }

    private static void persist(
            W128WebRegistrySavedData data,
            W128WebProject project,
            JsonObject root,
            long nowMillis
    ) {
        W130Workspace.writeInternal(
                data,
                project,
                W130Workspace.SOURCE_CONTROL_PATH,
                GSON.toJson(root),
                nowMillis
        );
    }

    private static JsonObject baselineObject(W128WebProject project) {
        JsonObject object = new JsonObject();
        currentFiles(project).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> object.addProperty(entry.getKey(), entry.getValue()));
        return object;
    }

    private static Map<String, String> currentFiles(W128WebProject project) {
        Map<String, String> result = new LinkedHashMap<>();
        project.files().values().stream()
                .filter(file -> !W130Workspace.internalPath(file.path()))
                .filter(file -> !file.generated())
                .sorted(Comparator.comparing(W128WebFile::path))
                .forEach(file -> result.put(file.path(), file.content()));
        return result;
    }

    private static String[] lines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private static int integer(JsonObject object, String name, int fallback) {
        try {
            return object.has(name) ? object.get(name).getAsInt() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long longValue(JsonObject object, String name, long fallback) {
        try {
            return object.has(name) ? object.get(name).getAsLong() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static String string(JsonObject object, String name, String fallback) {
        try {
            return object.has(name) ? object.get(name).getAsString() : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private record State(
            int head,
            Map<String, String> baseline,
            Set<String> staged,
            List<W130ScmSnapshot.CommitInfo> history
    ) {
        private State {
            head = Math.max(0, head);
            baseline = baseline == null ? Map.of() : Map.copyOf(baseline);
            staged = staged == null ? Set.of() : Set.copyOf(staged);
            history = history == null ? List.of() : List.copyOf(history);
        }

        private State withStaged(Set<String> value) {
            return new State(head, baseline, value, history);
        }
    }
}
