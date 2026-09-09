package com.k1ngtle.vsia.signality.internet.provider;

public record InternetRegistryResult(boolean success, String message) {
    public static InternetRegistryResult ok(String message) {
        return new InternetRegistryResult(true, message);
    }

    public static InternetRegistryResult fail(String message) {
        return new InternetRegistryResult(false, message);
    }
}
