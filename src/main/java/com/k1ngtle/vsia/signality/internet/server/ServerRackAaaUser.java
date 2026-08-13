package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackAaaUser(String username, String password, int privilege, boolean enabled) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Username", username);
        tag.putString("Password", password);
        tag.putInt("Privilege", privilege);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public static ServerRackAaaUser load(CompoundTag tag) {
        return new ServerRackAaaUser(tag.getString("Username"), tag.getString("Password"),
                tag.getInt("Privilege"), tag.getBoolean("Enabled"));
    }
}
