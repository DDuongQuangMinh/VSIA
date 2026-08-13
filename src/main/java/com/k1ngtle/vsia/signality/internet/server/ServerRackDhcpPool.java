package com.k1ngtle.vsia.signality.internet.server;

import net.minecraft.nbt.CompoundTag;

public record ServerRackDhcpPool(String name, boolean ipv6, String start, String end,
                                 String prefixOrMask, String gateway, String dns, int leaseSeconds, String exclusions) {
    public CompoundTag save(){CompoundTag t=new CompoundTag();t.putString("Name",name);t.putBoolean("Ipv6",ipv6);t.putString("Start",start);t.putString("End",end);t.putString("PrefixOrMask",prefixOrMask);t.putString("Gateway",gateway);t.putString("Dns",dns);t.putInt("LeaseSeconds",leaseSeconds);t.putString("Exclusions",exclusions);return t;}
    public static ServerRackDhcpPool load(CompoundTag t){return new ServerRackDhcpPool(t.getString("Name"),t.getBoolean("Ipv6"),t.getString("Start"),t.getString("End"),t.getString("PrefixOrMask"),t.getString("Gateway"),t.getString("Dns"),Math.max(60,t.getInt("LeaseSeconds")),t.getString("Exclusions"));}
}
