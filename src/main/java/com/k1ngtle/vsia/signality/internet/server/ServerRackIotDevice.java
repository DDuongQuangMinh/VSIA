package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackIotDevice(String id, String name, String type, boolean online,
                                  String state, String telemetry, long lastSeen) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Name", name);
        tag.putString("Type", type);
        tag.putBoolean("Online", online);
        tag.putString("State", state);
        tag.putString("Telemetry", telemetry);
        tag.putLong("LastSeen", lastSeen);
        return tag;
    }

    public static ServerRackIotDevice load(CompoundTag tag) {
        return new ServerRackIotDevice(tag.getString("Id"), tag.getString("Name"),
                tag.getString("Type"), tag.getBoolean("Online"), tag.getString("State"),
                tag.getString("Telemetry"), tag.getLong("LastSeen"));
    }
}
