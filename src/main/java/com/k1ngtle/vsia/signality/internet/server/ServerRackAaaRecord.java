package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackAaaRecord(long timestamp, String username, String service,
                                  String action, boolean success, String source) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Timestamp", timestamp);
        tag.putString("Username", username);
        tag.putString("Service", service);
        tag.putString("Action", action);
        tag.putBoolean("Success", success);
        tag.putString("Source", source);
        return tag;
    }

    public static ServerRackAaaRecord load(CompoundTag tag) {
        return new ServerRackAaaRecord(tag.getLong("Timestamp"), tag.getString("Username"),
                tag.getString("Service"), tag.getString("Action"),
                tag.getBoolean("Success"), tag.getString("Source"));
    }
}
