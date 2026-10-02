package com.k1ngtle.vsia.cockpit.client;

import java.util.Objects;

/** Owns one GUI scissor entry; replacing it must never pop a caller's entry. */
public final class DisplayGuiClipScope implements AutoCloseable {
    public interface Backend {
        void push(int left, int top, int right, int bottom);
        void pop();
    }

    private final Backend backend;
    private boolean active, closed;

    public DisplayGuiClipScope(Backend backend) {
        this.backend = Objects.requireNonNull(backend);
    }

    public void replace(int left, int top, int right, int bottom) {
        if (closed) throw new IllegalStateException("Display clip scope is closed");
        if (active) {
            backend.pop();
            active = false;
        }
        backend.push(left, top, right, bottom);
        active = true;
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        if (active) {
            active = false;
            backend.pop();
        }
    }
}
