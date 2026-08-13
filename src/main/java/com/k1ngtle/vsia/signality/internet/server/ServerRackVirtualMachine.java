package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackVirtualMachine(String name, String operatingSystem, int cpuCores,
                                       int memoryMb, int storageGb, String state,
                                       long lastStateChange, String console) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("OperatingSystem", operatingSystem);
        tag.putInt("CpuCores", cpuCores);
        tag.putInt("MemoryMb", memoryMb);
        tag.putInt("StorageGb", storageGb);
        tag.putString("State", state);
        tag.putLong("LastStateChange", lastStateChange);
        tag.putString("Console", console);
        return tag;
    }

    public static ServerRackVirtualMachine load(CompoundTag tag) {
        return new ServerRackVirtualMachine(tag.getString("Name"), tag.getString("OperatingSystem"),
                tag.getInt("CpuCores"), tag.getInt("MemoryMb"), tag.getInt("StorageGb"),
                tag.getString("State"), tag.getLong("LastStateChange"), tag.getString("Console"));
    }
}
