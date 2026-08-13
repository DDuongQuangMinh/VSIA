package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackRadiusClient(String name, String address, String sharedSecret, boolean enabled) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Address", address);
        tag.putString("SharedSecret", sharedSecret);
        tag.putBoolean("Enabled", enabled);
        return tag;
    }

    public static ServerRackRadiusClient load(CompoundTag tag) {
        return new ServerRackRadiusClient(tag.getString("Name"), tag.getString("Address"),
                tag.getString("SharedSecret"), tag.getBoolean("Enabled"));
    }
}
