package com.k1ngtle.vsia.signality.engineering.wifi.ip.router;

import java.util.List;

public record RouterEngineeringSnapshot(
        boolean enabled,
        List<String> interfaces,
        List<String> routes,
        int neighborCount,
        List<String> diagnostics
) {
    public static RouterEngineeringSnapshot empty() {
        return new RouterEngineeringSnapshot(
                false,
                List.of(),
                List.of(),
                0,
                List.of()
        );
    }
}
