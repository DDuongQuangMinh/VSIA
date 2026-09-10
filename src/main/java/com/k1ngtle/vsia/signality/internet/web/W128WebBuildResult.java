package com.k1ngtle.vsia.signality.internet.web;

public record W128WebBuildResult(
        boolean success,
        String message
) {
    public static W128WebBuildResult ok(String message) {
        return new W128WebBuildResult(true, message);
    }

    public static W128WebBuildResult fail(String message) {
        return new W128WebBuildResult(false, message);
    }
}
