package com.k1ngtle.vsia.signality.internet.web;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class W128WebProject {
    private final String host;
    private final String rootDomain;
    private final UUID ownerUuid;
    private String ownerName;
    private W128WebMode mode;
    private final long createdAtMillis;
    private long modifiedAtMillis;
    private String boundServerIp;
    private boolean published;
    private long buildRevision;
    private final Map<String, W128WebFile> files = new LinkedHashMap<>();

    public W128WebProject(
            String host,
            String rootDomain,
            UUID ownerUuid,
            String ownerName,
            W128WebMode mode,
            long createdAtMillis
    ) {
        this.host = host;
        this.rootDomain = rootDomain;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName == null ? "" : ownerName;
        this.mode = mode == null ? W128WebMode.STATIC : mode;
        this.createdAtMillis = createdAtMillis;
        this.modifiedAtMillis = createdAtMillis;
        this.boundServerIp = "";
    }

    public String host() {
        return host;
    }

    public String rootDomain() {
        return rootDomain;
    }

    public UUID ownerUuid() {
        return ownerUuid;
    }

    public String ownerName() {
        return ownerName;
    }

    public W128WebMode mode() {
        return mode;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    public long modifiedAtMillis() {
        return modifiedAtMillis;
    }

    public String boundServerIp() {
        return boundServerIp;
    }

    public boolean published() {
        return published;
    }

    public long buildRevision() {
        return buildRevision;
    }

    public Map<String, W128WebFile> files() {
        return Map.copyOf(files);
    }

    public W128WebFile file(String path) {
        return files.get(path);
    }

    public void putFile(W128WebFile file) {
        files.put(file.path(), file);
        modifiedAtMillis = Math.max(modifiedAtMillis, file.modifiedAtMillis());
    }

    public boolean removeFile(String path, long nowMillis) {
        boolean changed = files.remove(path) != null;
        if (changed) {
            modifiedAtMillis = nowMillis;
        }
        return changed;
    }

    public void setBoundServerIp(String value, long nowMillis) {
        boundServerIp = value == null ? "" : value.trim();
        modifiedAtMillis = nowMillis;
    }

    public void setPublished(boolean value, long nowMillis) {
        published = value;
        modifiedAtMillis = nowMillis;
    }

    public void markBuilt(long nowMillis) {
        buildRevision++;
        modifiedAtMillis = nowMillis;
    }

    public int totalCharacters() {
        return files.values()
                .stream()
                .mapToInt(file -> file.content().length())
                .sum();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Host", host);
        tag.putString("RootDomain", rootDomain);
        tag.putUUID("OwnerUuid", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putString("Mode", mode.name());
        tag.putLong("CreatedAtMillis", createdAtMillis);
        tag.putLong("ModifiedAtMillis", modifiedAtMillis);
        tag.putString("BoundServerIp", boundServerIp);
        tag.putBoolean("Published", published);
        tag.putLong("BuildRevision", buildRevision);

        ListTag list = new ListTag();
        files.values().forEach(file -> list.add(file.save()));
        tag.put("Files", list);
        return tag;
    }

    public static W128WebProject load(CompoundTag tag) {
        UUID owner = tag.hasUUID("OwnerUuid")
                ? tag.getUUID("OwnerUuid")
                : new UUID(0L, 0L);

        W128WebMode mode;
        try {
            mode = W128WebMode.valueOf(tag.getString("Mode"));
        } catch (Exception ignored) {
            mode = W128WebMode.STATIC;
        }

        W128WebProject project = new W128WebProject(
                tag.getString("Host"),
                tag.getString("RootDomain"),
                owner,
                tag.getString("OwnerName"),
                mode,
                tag.getLong("CreatedAtMillis")
        );

        project.modifiedAtMillis = tag.getLong("ModifiedAtMillis");
        project.boundServerIp = tag.getString("BoundServerIp");
        project.published = tag.getBoolean("Published");
        project.buildRevision = tag.getLong("BuildRevision");

        if (tag.contains("Files", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Files", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                W128WebFile file = W128WebFile.load(list.getCompound(i));
                project.files.put(file.path(), file);
            }
        }

        return project;
    }
}
