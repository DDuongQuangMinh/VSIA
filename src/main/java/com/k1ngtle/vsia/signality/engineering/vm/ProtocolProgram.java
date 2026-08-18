package com.k1ngtle.vsia.signality.engineering.vm;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record ProtocolProgram(
        ResourceLocation id,
        int version,
        ProtocolVmLimits limits,
        Map<String, Integer> entrypoints,
        List<ProtocolInstruction> instructions
) {
    public ProtocolProgram {
        if (id == null) {
            throw new IllegalArgumentException("id");
        }

        if (version < 1) {
            throw new IllegalArgumentException("version");
        }

        limits = limits == null
                ? ProtocolVmLimits.DEFAULT
                : limits;

        entrypoints = Map.copyOf(
                entrypoints == null
                        ? Map.of()
                        : entrypoints
        );

        instructions = List.copyOf(
                instructions == null
                        ? List.of()
                        : instructions
        );

        for (Map.Entry<String, Integer> entry
                : entrypoints.entrySet()) {
            int pc = entry.getValue();

            if (pc < 0
                    || pc >= instructions.size()) {
                throw new IllegalArgumentException(
                        "Entrypoint "
                                + entry.getKey()
                                + " points outside program"
                );
            }
        }
    }

    public int entrypoint(
            String name
    ) {
        return entrypoints.getOrDefault(
                name,
                -1
        );
    }
}
