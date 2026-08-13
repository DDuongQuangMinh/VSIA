package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackMailAccount(
        String username,
        String domain,
        String password,
        int quota
) {
    public String address() {
        return username + "@" + domain;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("Username", username);
        tag.putString("Domain", domain);
        tag.putString("Password", password);
        tag.putInt("Quota", quota);

        return tag;
    }

    public static ServerRackMailAccount load(CompoundTag tag) {
        return new ServerRackMailAccount(
                tag.getString("Username"),
                tag.getString("Domain"),
                tag.getString("Password"),
                Math.max(10, tag.getInt("Quota"))
        );
    }
}