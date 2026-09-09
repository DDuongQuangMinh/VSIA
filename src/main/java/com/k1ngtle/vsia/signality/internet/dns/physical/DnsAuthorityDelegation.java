package com.k1ngtle.vsia.signality.internet.dns.physical;

import net.minecraft.nbt.CompoundTag;

public record DnsAuthorityDelegation(
        String zone,
        String tld,
        String primaryIp,
        String secondaryIp,
        String primaryNameServer,
        String secondaryNameServer
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Zone", zone);
        tag.putString("Tld", tld);
        tag.putString("PrimaryIp", primaryIp);
        tag.putString("SecondaryIp", secondaryIp);
        tag.putString("PrimaryNameServer", primaryNameServer);
        tag.putString("SecondaryNameServer", secondaryNameServer);
        return tag;
    }

    public static DnsAuthorityDelegation load(CompoundTag tag) {
        return new DnsAuthorityDelegation(
                tag.getString("Zone"),
                tag.getString("Tld"),
                tag.getString("PrimaryIp"),
                tag.getString("SecondaryIp"),
                tag.getString("PrimaryNameServer"),
                tag.getString("SecondaryNameServer")
        );
    }
}
