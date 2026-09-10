package com.k1ngtle.vsia.signality.internet.web;

import java.util.Locale;

public enum W128WebMode {
    STATIC,
    REACT;

    public static W128WebMode parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Mode must be static or react.");
        }

        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "static", "html" -> STATIC;
            case "react", "jsx" -> REACT;
            default -> throw new IllegalArgumentException("Mode must be static or react.");
        };
    }
}
