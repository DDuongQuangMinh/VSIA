package com.k1ngtle.vsia.signality.engineering.vm;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ProtocolProgramRegistry {
    private static final Map<ResourceLocation, ProtocolProgram> PROGRAMS =
            new LinkedHashMap<>();

    private ProtocolProgramRegistry() {
    }

    public static synchronized void clear() {
        PROGRAMS.clear();
    }

    public static synchronized void register(
            ProtocolProgram program
    ) {
        PROGRAMS.put(
                program.id(),
                program
        );
    }

    public static synchronized Optional<ProtocolProgram> get(
            ResourceLocation id
    ) {
        return Optional.ofNullable(
                PROGRAMS.get(id)
        );
    }

    public static synchronized Collection<ProtocolProgram> values() {
        return ListHolder.copy(
                PROGRAMS.values()
        );
    }

    private static final class ListHolder {
        private static Collection<ProtocolProgram> copy(
                Collection<ProtocolProgram> values
        ) {
            return java.util.List.copyOf(
                    values
            );
        }
    }
}
