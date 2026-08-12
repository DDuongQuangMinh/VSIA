package com.k1ngtle.vsia.weapon.server.hitscan;

import java.util.Objects;
import java.util.function.Consumer;

public final class HitscanEvents {
    private static Consumer<HitscanResult> listener = result -> {};
    private HitscanEvents() {}
    public static void setListener(Consumer<HitscanResult> newListener) { listener = Objects.requireNonNull(newListener); }
    static void publish(HitscanResult result) { listener.accept(result); }
}
