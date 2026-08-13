package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackHostedFile(
        String name,
        String content,
        boolean readable,
        boolean writable
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("Name", name);
        tag.putString("Content", content);
        tag.putBoolean("Readable", readable);
        tag.putBoolean("Writable", writable);

        return tag;
    }

    public static ServerRackHostedFile load(CompoundTag tag) {
        return new ServerRackHostedFile(
                tag.getString("Name"),
                tag.getString("Content"),
                tag.getBoolean("Readable"),
                tag.getBoolean("Writable")
        );
    }
}