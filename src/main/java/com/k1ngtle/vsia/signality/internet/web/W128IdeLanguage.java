package com.k1ngtle.vsia.signality.internet.web;

import java.util.Locale;

public enum W128IdeLanguage {
    HTML("HTML", true),
    CSS("CSS", true),
    JAVASCRIPT("JavaScript", true),
    JSX("React JSX", true),
    JSON("JSON", false),
    MARKDOWN("Markdown", false),
    PYTHON("Python", false),
    C("C", false),
    CSHARP("C#", false),
    CPP("C++", false),
    ASSEMBLY("Assembly", false),
    JAVA("Java", false),
    TEXT("Plain Text", false);

    private final String displayName;
    private final boolean webRuntime;

    W128IdeLanguage(String displayName, boolean webRuntime) {
        this.displayName = displayName;
        this.webRuntime = webRuntime;
    }

    public String displayName() {
        return displayName;
    }

    public boolean webRuntime() {
        return webRuntime;
    }

    public static W128IdeLanguage detect(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);

        if (value.endsWith(".html") || value.endsWith(".htm")) return HTML;
        if (value.endsWith(".css")) return CSS;
        if (value.endsWith(".jsx")) return JSX;
        if (value.endsWith(".js") || value.endsWith(".mjs")) return JAVASCRIPT;
        if (value.endsWith(".json")) return JSON;
        if (value.endsWith(".md")) return MARKDOWN;
        if (value.endsWith(".py")) return PYTHON;
        if (value.endsWith(".cs")) return CSHARP;
        if (value.endsWith(".cpp") || value.endsWith(".cc") || value.endsWith(".cxx")
                || value.endsWith(".hpp") || value.endsWith(".hh") || value.endsWith(".hxx")) return CPP;
        if (value.endsWith(".c") || value.endsWith(".h")) return C;
        if (value.endsWith(".asm") || value.endsWith(".s") || value.endsWith(".inc")) return ASSEMBLY;
        if (value.endsWith(".java")) return JAVA;

        return TEXT;
    }
}
