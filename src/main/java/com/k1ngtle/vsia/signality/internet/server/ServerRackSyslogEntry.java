package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackSyslogEntry(
        long timestamp,
        String source,
        String facility,
        int severity,
        String message
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Timestamp", timestamp);
        tag.putString("Source", source);
        tag.putString("Facility", facility);
        tag.putInt("Severity", severity);
        tag.putString("Message", message);
        return tag;
    }

    public static ServerRackSyslogEntry load(CompoundTag tag) {
        return new ServerRackSyslogEntry(
                tag.getLong("Timestamp"),
                tag.getString("Source"),
                tag.getString("Facility"),
                tag.getInt("Severity"),
                tag.getString("Message")
        );
    }
}
