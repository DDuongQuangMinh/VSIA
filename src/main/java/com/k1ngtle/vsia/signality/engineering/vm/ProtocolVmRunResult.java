package com.k1ngtle.vsia.signality.engineering.vm;

public record ProtocolVmRunResult(
        boolean success,
        boolean halted,
        int instructionsExecuted,
        String error
) {
    public static ProtocolVmRunResult ok(
            boolean halted,
            int instructionsExecuted
    ) {
        return new ProtocolVmRunResult(
                true,
                halted,
                instructionsExecuted,
                ""
        );
    }

    public static ProtocolVmRunResult failed(
            int instructionsExecuted,
            String error
    ) {
        return new ProtocolVmRunResult(
                false,
                false,
                instructionsExecuted,
                error == null ? "" : error
        );
    }
}
