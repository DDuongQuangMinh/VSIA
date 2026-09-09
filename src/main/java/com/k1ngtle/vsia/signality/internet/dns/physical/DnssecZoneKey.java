package com.k1ngtle.vsia.signality.internet.dns.physical;

import net.minecraft.nbt.CompoundTag;

public record DnssecZoneKey(
        String zone,
        int flags,
        int protocol,
        int algorithm,
        int keyTag,
        String publicKeyX509Base64,
        String privateKeyPkcs8Base64,
        String dnskeyPublicBase64,
        long createdAtMillis
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Zone", zone);
        tag.putInt("Flags", flags);
        tag.putInt("Protocol", protocol);
        tag.putInt("Algorithm", algorithm);
        tag.putInt("KeyTag", keyTag);
        tag.putString("PublicX509", publicKeyX509Base64);
        tag.putString("PrivatePkcs8", privateKeyPkcs8Base64);
        tag.putString("DnskeyPublic", dnskeyPublicBase64);
        tag.putLong("CreatedAtMillis", createdAtMillis);
        return tag;
    }

    public static DnssecZoneKey load(CompoundTag tag) {
        return new DnssecZoneKey(
                tag.getString("Zone"),
                tag.getInt("Flags"),
                tag.getInt("Protocol"),
                tag.getInt("Algorithm"),
                tag.getInt("KeyTag"),
                tag.getString("PublicX509"),
                tag.getString("PrivatePkcs8"),
                tag.getString("DnskeyPublic"),
                tag.getLong("CreatedAtMillis")
        );
    }

    public String presentation() {
        return flags + " " + protocol + " " + algorithm + " " + dnskeyPublicBase64;
    }
}
