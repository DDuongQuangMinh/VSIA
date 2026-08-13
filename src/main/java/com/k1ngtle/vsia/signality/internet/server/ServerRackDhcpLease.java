package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackDhcpLease(String clientId,String address,String pool,boolean ipv6,long expiresAt){
    public CompoundTag save(){CompoundTag t=new CompoundTag();t.putString("ClientId",clientId);t.putString("Address",address);t.putString("Pool",pool);t.putBoolean("Ipv6",ipv6);t.putLong("ExpiresAt",expiresAt);return t;}
    public static ServerRackDhcpLease load(CompoundTag t){return new ServerRackDhcpLease(t.getString("ClientId"),t.getString("Address"),t.getString("Pool"),t.getBoolean("Ipv6"),t.getLong("ExpiresAt"));}
}
