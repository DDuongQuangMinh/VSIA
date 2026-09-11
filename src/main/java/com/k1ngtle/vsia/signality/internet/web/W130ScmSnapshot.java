package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public record W130ScmSnapshot(
        int head,
        List<Change> changes,
        List<CommitInfo> history
) {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public W130ScmSnapshot {
        head = Math.max(0, head);
        changes = changes == null ? List.of() : List.copyOf(changes);
        history = history == null ? List.of() : List.copyOf(history);
    }

    public static W130ScmSnapshot empty() {
        return new W130ScmSnapshot(0, List.of(), List.of());
    }

    public int stagedCount() {
        return (int) changes.stream().filter(Change::staged).count();
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static W130ScmSnapshot fromJson(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }

        try {
            W130ScmSnapshot value = GSON.fromJson(json, W130ScmSnapshot.class);
            return value == null ? empty() : value;
        } catch (RuntimeException ignored) {
            return empty();
        }
    }

    public record Change(
            String state,
            String path,
            boolean staged
    ) {
        public Change {
            state = state == null ? "M" : state;
            path = path == null ? "" : path;
        }
    }

    public record CommitInfo(
            int id,
            String message,
            long atMillis
    ) {
        public CommitInfo {
            id = Math.max(0, id);
            message = message == null ? "" : message;
        }
    }
}
