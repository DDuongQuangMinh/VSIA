package com.k1ngtle.vsia.signality.internet.provider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RegisteredDomain(
        String domain,
        UUID ownerUuid,
        String ownerName,
        String registrarProviderId,
        long createdAtMillis,
        long expiresAtMillis,
        long transferLockedUntilMillis,
        String authInfo,
        List<String> nameServers,
        boolean active,
        boolean systemReserved
) {
    public RegisteredDomain {
        nameServers = nameServers == null ? List.of() : List.copyOf(nameServers);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Domain", domain);
        tag.putUUID("OwnerUuid", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putString("RegistrarProviderId", registrarProviderId);
        tag.putLong("CreatedAtMillis", createdAtMillis);
        tag.putLong("ExpiresAtMillis", expiresAtMillis);
        tag.putLong("TransferLockedUntilMillis", transferLockedUntilMillis);
        tag.putString("AuthInfo", authInfo);

        ListTag list = new ListTag();
        for (String server : nameServers) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Name", server);
            list.add(entry);
        }
        tag.put("NameServers", list);
        tag.putBoolean("Active", active);
        tag.putBoolean("SystemReserved", systemReserved);
        return tag;
    }

    public static RegisteredDomain load(CompoundTag tag) {
        List<String> nameServers = new ArrayList<>();
        if (tag.contains("NameServers", Tag.TAG_LIST)) {
            ListTag list = tag.getList("NameServers", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                nameServers.add(list.getCompound(i).getString("Name"));
            }
        }

        return new RegisteredDomain(
                tag.getString("Domain"),
                tag.hasUUID("OwnerUuid") ? tag.getUUID("OwnerUuid") : new UUID(0L, 0L),
                tag.getString("OwnerName"),
                tag.getString("RegistrarProviderId"),
                tag.getLong("CreatedAtMillis"),
                tag.getLong("ExpiresAtMillis"),
                tag.getLong("TransferLockedUntilMillis"),
                tag.getString("AuthInfo"),
                nameServers,
                !tag.contains("Active") || tag.getBoolean("Active"),
                tag.getBoolean("SystemReserved")
        );
    }

    public boolean ownedBy(UUID playerUuid) {
        return playerUuid != null && ownerUuid.equals(playerUuid);
    }

    public boolean expired(long nowMillis) {
        return !systemReserved && expiresAtMillis > 0L && nowMillis >= expiresAtMillis;
    }
}
