package com.k1ngtle.vsia.signality.internet.web;

import net.minecraft.nbt.CompoundTag;

public record W128WebFile(
        String path,
        String content,
        String contentType,
        long modifiedAtMillis,
        boolean generated
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Path", path);
        tag.putString("Content", content);
        tag.putString("ContentType", contentType);
        tag.putLong("ModifiedAtMillis", modifiedAtMillis);
        tag.putBoolean("Generated", generated);
        return tag;
    }

    public static W128WebFile load(CompoundTag tag) {
        return new W128WebFile(
                tag.getString("Path"),
                tag.getString("Content"),
                tag.getString("ContentType"),
                tag.getLong("ModifiedAtMillis"),
                tag.getBoolean("Generated")
        );
    }
}
