package com.k1ngtle.vsia.signality.internet.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record W130DebugSnapshot(
        String state,
        String path,
        int line,
        String reason,
        List<String> callStack,
        Map<String, String> variables,
        Map<String, String> watches,
        Map<String, List<Integer>> breakpoints
) {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public W130DebugSnapshot {
        state = state == null ? "IDLE" : state;
        path = path == null ? "" : path;
        line = Math.max(0, line);
        reason = reason == null ? "" : reason;
        callStack = callStack == null ? List.of() : List.copyOf(callStack);
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        watches = watches == null ? Map.of() : Map.copyOf(watches);

        if (breakpoints == null || breakpoints.isEmpty()) {
            breakpoints = Map.of();
        } else {
            Map<String, List<Integer>> copy = new LinkedHashMap<>();
            breakpoints.forEach((key, value) -> copy.put(
                    key == null ? "" : key,
                    value == null ? List.of() : List.copyOf(value)
            ));
            breakpoints = Map.copyOf(copy);
        }
    }

    public static W130DebugSnapshot idle() {
        return new W130DebugSnapshot(
                "IDLE",
                "",
                0,
                "",
                List.of(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public boolean active() {
        return !"IDLE".equalsIgnoreCase(state)
                && !"TERMINATED".equalsIgnoreCase(state);
    }

    public boolean paused() {
        return "PAUSED".equalsIgnoreCase(state);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static W130DebugSnapshot fromJson(String json) {
        if (json == null || json.isBlank()) {
            return idle();
        }

        try {
            W130DebugSnapshot snapshot = GSON.fromJson(json, W130DebugSnapshot.class);
            return snapshot == null ? idle() : snapshot;
        } catch (RuntimeException ignored) {
            return idle();
        }
    }
}
