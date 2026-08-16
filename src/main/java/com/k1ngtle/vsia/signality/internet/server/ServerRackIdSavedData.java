package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class ServerRackIdSavedData extends SavedData {

    private static final String DATA_NAME = "vsia_network_ids";
    private int nextId = 0;

    public ServerRackIdSavedData() {
    }

    public ServerRackIdSavedData(CompoundTag tag) {
        if (tag.contains("NextId")) {
            this.nextId = tag.getInt("NextId");
        }
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", this.nextId);
        return tag;
    }

    public int allocateId() {
        int id = this.nextId++;
        this.setDirty();
        return id;
    }

    public static int allocate(ServerLevel level) {
        ServerRackIdSavedData data = level.getServer().overworld().getDataStorage()
                .computeIfAbsent(ServerRackIdSavedData::new, ServerRackIdSavedData::new, DATA_NAME);
        return data.allocateId();
    }
}