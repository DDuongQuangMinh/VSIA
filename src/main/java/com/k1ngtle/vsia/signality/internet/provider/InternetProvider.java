package com.k1ngtle.vsia.signality.internet.provider;

public record InternetProvider(
        String id,
        String displayName,
        String autonomousSystem,
        String dnsSuffix
) {
    public InternetProvider {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName");
        autonomousSystem = autonomousSystem == null ? "" : autonomousSystem;
        dnsSuffix = dnsSuffix == null ? "" : dnsSuffix.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
