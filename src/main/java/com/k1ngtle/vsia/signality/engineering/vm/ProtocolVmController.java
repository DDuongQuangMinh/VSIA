package com.k1ngtle.vsia.signality.engineering.vm;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class ProtocolVmController {

    private final ProtocolVmHost host;

    private ResourceLocation programId;
    private ProtocolVirtualMachine vm;

    private ProtocolVmRunResult lastResult =
            ProtocolVmRunResult.ok(
                    true,
                    0
            );

    public ProtocolVmController(
            ProtocolVmHost host
    ) {
        if (host == null) {
            throw new IllegalArgumentException(
                    "host"
            );
        }

        this.host =
                host;
    }

    public ResourceLocation programId() {
        return programId;
    }

    public boolean bound() {
        return programId != null;
    }

    public ProtocolVmRunResult lastResult() {
        return lastResult;
    }

    public boolean bind(
            ResourceLocation id
    ) {
        ProtocolProgram program =
                ProtocolProgramRegistry
                        .get(
                                id
                        )
                        .orElse(
                                null
                        );

        if (program == null) {
            return false;
        }

        programId =
                id;

        vm =
                new ProtocolVirtualMachine(
                        program,
                        host
                );

        return true;
    }

    public void unbind() {
        programId =
                null;

        vm =
                null;
    }

    public ProtocolVmRunResult transmitHostPayload(
            byte[] payload,
            ProtocolVmEnvironment environment
    ) {
        ProtocolVirtualMachine active =
                resolveVm();

        if (active == null) {
            return ProtocolVmRunResult.failed(
                    0,
                    "No protocol program bound"
            );
        }

        lastResult =
                active.run(
                        "on_tx",
                        payload,
                        environment
                );

        return lastResult;
    }

    public ProtocolVmRunResult receiveFrame(
            byte[] frame,
            ProtocolVmEnvironment environment
    ) {
        ProtocolVirtualMachine active =
                resolveVm();

        if (active == null) {
            return ProtocolVmRunResult.failed(
                    0,
                    "No protocol program bound"
            );
        }

        lastResult =
                active.run(
                        "on_receive",
                        frame,
                        environment
                );

        return lastResult;
    }

    public void tick() {
        ProtocolVirtualMachine active =
                resolveVm();

        if (active == null) {
            return;
        }

        List<String> entries =
                active.dueTimerEntrypoints();

        for (String entry : entries) {
            lastResult =
                    active.run(
                            entry,
                            new byte[0],
                            ProtocolVmEnvironment.empty()
                    );
        }
    }

    public CompoundTag save() {
        CompoundTag tag =
                new CompoundTag();

        if (programId != null) {
            tag.putString(
                    "Program",
                    programId.toString()
            );
        }

        return tag;
    }

    public void load(
            CompoundTag tag
    ) {
        programId =
                null;

        vm =
                null;

        if (!tag.contains(
                "Program"
        )) {
            return;
        }

        ResourceLocation parsed =
                ResourceLocation.tryParse(
                        tag.getString(
                                "Program"
                        )
                );

        if (parsed != null) {
            programId =
                    parsed;
        }
    }

    private ProtocolVirtualMachine resolveVm() {
        if (programId == null) {
            vm =
                    null;

            return null;
        }

        ProtocolProgram program =
                ProtocolProgramRegistry
                        .get(
                                programId
                        )
                        .orElse(
                                null
                        );

        if (program == null) {
            vm =
                    null;

            return null;
        }

        if (vm == null
                || vm.program()
                != program) {
            vm =
                    new ProtocolVirtualMachine(
                            program,
                            host
                    );
        }

        return vm;
    }
}
