package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Persistent per-world sequence used to give every rack a stable automatic ID. */
public final class ServerRackIdSavedData extends SavedData {
    private static final String DATA_NAME = "vsia_server_rack_ids";
    private int nextId;

    public ServerRackIdSavedData() {}

    public static ServerRackIdSavedData load(CompoundTag tag) {
        ServerRackIdSavedData data = new ServerRackIdSavedData();
        data.nextId = Math.max(0, tag.getInt("NextId"));
        return data;
    }

    public static int allocate(ServerLevel level) {
        // Store the sequence in the Overworld so IDs remain unique across every dimension.
        ServerRackIdSavedData data = level.getServer().overworld().getDataStorage().computeIfAbsent(
                ServerRackIdSavedData::load,
                ServerRackIdSavedData::new,
                DATA_NAME
        );
        int allocated = data.nextId++;
        data.setDirty();
        return allocated;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("NextId", nextId);
        return tag;
    }
}
