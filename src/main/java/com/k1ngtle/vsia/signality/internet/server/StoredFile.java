package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public class StoredFile {
    private String name;
    private String language;
    private String content;

    public StoredFile(String name, String language, String content) {
        this.name = name;
        this.language = language;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns size in bytes based on UTF-8 string content length.
     */
    public int getSizeInBytes() {
        if (content == null) return 0;
        return content.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    public String getFormattedSize() {
        int bytes = getSizeInBytes();
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0));
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putString("Language", language);
        tag.putString("Content", content != null ? content : "");
        return tag;
    }

    public static StoredFile deserializeNBT(CompoundTag tag) {
        String name = tag.getString("Name");
        String language = tag.getString("Language");
        String content = tag.getString("Content");
        return new StoredFile(name, language, content);
    }
}