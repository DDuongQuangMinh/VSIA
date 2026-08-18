package com.k1ngtle.vsia.signality.engineering.vm;

import java.util.Map;

public record ProtocolVmEnvironment(
        Map<String, Double> numbers,
        Map<String, String> strings
) {
    public ProtocolVmEnvironment {
        numbers = Map.copyOf(
                numbers == null
                        ? Map.of()
                        : numbers
        );

        strings = Map.copyOf(
                strings == null
                        ? Map.of()
                        : strings
        );
    }

    public double number(
            String key
    ) {
        return numbers.getOrDefault(
                key,
                0.0
        );
    }

    public String string(
            String key
    ) {
        return strings.getOrDefault(
                key,
                ""
        );
    }

    public static ProtocolVmEnvironment empty() {
        return new ProtocolVmEnvironment(
                Map.of(),
                Map.of()
        );
    }
}
