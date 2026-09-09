package com.k1ngtle.vsia.signality.internet.dns.physical;

import java.util.Locale;

public enum PhysicalDnsRole {
    NONE,
    RECURSIVE,
    ROOT,
    TLD,
    AUTHORITATIVE_PRIMARY,
    AUTHORITATIVE_SECONDARY;

    public static PhysicalDnsRole parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        String normalized = value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');

        return switch (normalized) {
            case "RECURSIVE", "RESOLVER" -> RECURSIVE;
            case "ROOT" -> ROOT;
            case "TLD" -> TLD;
            case "AUTHORITATIVE_PRIMARY", "AUTH_PRIMARY", "PRIMARY" -> AUTHORITATIVE_PRIMARY;
            case "AUTHORITATIVE_SECONDARY", "AUTH_SECONDARY", "SECONDARY" -> AUTHORITATIVE_SECONDARY;
            default -> NONE;
        };
    }
}
