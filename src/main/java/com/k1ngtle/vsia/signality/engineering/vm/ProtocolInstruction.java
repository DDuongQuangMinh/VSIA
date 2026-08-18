package com.k1ngtle.vsia.signality.engineering.vm;

public record ProtocolInstruction(
        ProtocolOpcode opcode,
        int a,
        int b,
        int c,
        double value,
        String text,
        int target
) {
    public ProtocolInstruction {
        if (opcode == null) {
            throw new IllegalArgumentException("opcode");
        }

        text = text == null ? "" : text;
    }
}
