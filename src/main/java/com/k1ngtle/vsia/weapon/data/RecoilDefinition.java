package com.k1ngtle.vsia.weapon.data;

public record RecoilDefinition(float vertical, float horizontal, float recovery) {
    public RecoilDefinition {
        if (vertical < 0 || horizontal < 0 || recovery < 0) throw new IllegalArgumentException("recoil values must be non-negative");
    }
}
