package com.k1ngtle.vsia.signality.internet.provider;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record OwnedInternetProvider(
        String id,
        String displayName,
        UUID ownerUuid,
        String ownerName,
        long autonomousSystemNumber,
        long createdAtMillis,
        boolean registrarEnabled,
        boolean active,
        boolean systemReserved
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("DisplayName", displayName);
        tag.putUUID("OwnerUuid", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putLong("Asn", autonomousSystemNumber);
        tag.putLong("CreatedAtMillis", createdAtMillis);
        tag.putBoolean("RegistrarEnabled", registrarEnabled);
        tag.putBoolean("Active", active);
        tag.putBoolean("SystemReserved", systemReserved);
        return tag;
    }

    public static OwnedInternetProvider load(CompoundTag tag) {
        return new OwnedInternetProvider(
                tag.getString("Id"),
                tag.getString("DisplayName"),
                tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : new UUID(0L, 0L),
                tag.getString("OwnerName"),
                tag.getLong("Asn"),
                tag.getLong("CreatedAtMillis"),
                tag.getBoolean("RegistrarEnabled"),
                !tag.contains("Active") || tag.getBoolean("Active"),
                tag.getBoolean("SystemReserved")
        );
    }

    public boolean ownedBy(UUID playerUuid) {
        return playerUuid != null && ownerUuid.equals(playerUuid);
    }
}
