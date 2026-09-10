package com.k1ngtle.vsia.signality.internet.web;

import java.util.List;

public record W129RunResult(
        boolean success,
        int exitCode,
        String output,
        long cycles,
        long memoryBytes,
        List<W129Diagnostic> diagnostics
) {
    public W129RunResult {
        output = output == null ? "" : output;
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        cycles = Math.max(0L, cycles);
        memoryBytes = Math.max(0L, memoryBytes);
    }

    public static W129RunResult ok(
            String output,
            long cycles,
            long memoryBytes
    ) {
        return new W129RunResult(
                true,
                0,
                output,
                cycles,
                memoryBytes,
                List.of()
        );
    }

    public static W129RunResult fail(
            String output,
            List<W129Diagnostic> diagnostics
    ) {
        return new W129RunResult(
                false,
                1,
                output,
                0L,
                0L,
                diagnostics
        );
    }

    public String terminalText(String path, String runtimeName) {
        StringBuilder out = new StringBuilder();

        out.append("> run ")
                .append(path == null ? "" : path)
                .append('\n');

        if (!output.isBlank()) {
            out.append(output);
            if (!output.endsWith("\n")) {
                out.append('\n');
            }
        }

        out.append('\n')
                .append("Process exited with code ")
                .append(exitCode)
                .append('\n')
                .append("Runtime: ")
                .append(runtimeName == null ? "VSIA" : runtimeName)
                .append('\n')
                .append("Cycles: ")
                .append(cycles)
                .append('\n')
                .append("Memory: ")
                .append(memoryBytes)
                .append(" B");

        return out.toString();
    }
}
