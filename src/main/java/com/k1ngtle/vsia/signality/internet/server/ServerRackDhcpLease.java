package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackDhcpLease(
        String clientId,
        String address,
        String pool,
        boolean ipv6,
        long expiresAt
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putString("ClientId", clientId);
        tag.putString("Address", address);
        tag.putString("Pool", pool);
        tag.putBoolean("Ipv6", ipv6);
        tag.putLong("ExpiresAt", expiresAt);

        return tag;
    }

    public static ServerRackDhcpLease load(CompoundTag tag) {
        return new ServerRackDhcpLease(
                tag.getString("ClientId"),
                tag.getString("Address"),
                tag.getString("Pool"),
                tag.getBoolean("Ipv6"),
                tag.getLong("ExpiresAt")
        );
    }
}