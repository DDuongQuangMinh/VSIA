package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackDnsRecord(String name, String type, String detail, int ttl) {
    public String key() { return type.toUpperCase() + "|" + name.toLowerCase(); }
    public CompoundTag save() { CompoundTag t=new CompoundTag();t.putString("Name",name);t.putString("Type",type);t.putString("Detail",detail);t.putInt("Ttl",ttl);return t; }
    public static ServerRackDnsRecord load(CompoundTag t){return new ServerRackDnsRecord(t.getString("Name"),t.getString("Type"),t.getString("Detail"),Math.max(30,t.getInt("Ttl")));}
}
