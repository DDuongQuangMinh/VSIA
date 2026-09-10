package com.k1ngtle.vsia.signality.internet.web;

import java.util.Locale;

public final class W128MimeTypes {
    private W128MimeTypes() {
    }

    public static String forPath(String path) {
        String value = path == null ? "" : path.toLowerCase(Locale.ROOT);

        if (value.endsWith(".html") || value.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        }
        if (value.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (value.endsWith(".js") || value.endsWith(".mjs") || value.endsWith(".jsx")) {
            return "text/javascript; charset=utf-8";
        }
        if (value.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }
        if (value.endsWith(".txt") || value.endsWith(".md")) {
            return "text/plain; charset=utf-8";
        }
        if (value.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (value.endsWith(".xml")) {
            return "application/xml; charset=utf-8";
        }
        if (value.endsWith(".py")) {
            return "text/x-python; charset=utf-8";
        }
        if (value.endsWith(".c") || value.endsWith(".h")) {
            return "text/x-c; charset=utf-8";
        }
        if (value.endsWith(".cs")) {
            return "text/x-csharp; charset=utf-8";
        }
        if (value.endsWith(".cpp")
                || value.endsWith(".cc")
                || value.endsWith(".cxx")
                || value.endsWith(".hpp")
                || value.endsWith(".hh")
                || value.endsWith(".hxx")) {
            return "text/x-c++; charset=utf-8";
        }
        if (value.endsWith(".asm")
                || value.endsWith(".s")
                || value.endsWith(".inc")) {
            return "text/x-asm; charset=utf-8";
        }
        if (value.endsWith(".java")) {
            return "text/x-java-source; charset=utf-8";
        }

        return "application/octet-stream";
    }
}
