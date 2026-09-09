package com.k1ngtle.vsia.signality.internet.dns.physical;

import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.nbt.CompoundTag;

public record PhysicalDnsRackConfig(
        PhysicalDnsRole role,
        String zone,
        String rootHintIp,
        String masterIp,
        boolean dnssecValidation,
        String rootTrustAnchor
) {
    private static final String KEY = "VsiaPhysicalDns";

    public static PhysicalDnsRackConfig read(ServerRackBlockEntity rack) {
        CompoundTag tag = rack.getPersistentData().getCompound(KEY);

        return new PhysicalDnsRackConfig(
                PhysicalDnsRole.parse(tag.getString("Role")),
                tag.getString("Zone"),
                tag.getString("RootHintIp"),
                tag.getString("MasterIp"),
                !tag.contains("DnssecValidation") || tag.getBoolean("DnssecValidation"),
                tag.getString("RootTrustAnchor")
        );
    }

    public static void write(
            ServerRackBlockEntity rack,
            PhysicalDnsRole role,
            String zone,
            String rootHintIp,
            String masterIp,
            boolean dnssecValidation,
            String rootTrustAnchor
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Role", role.name());
        tag.putString("Zone", zone == null ? "" : zone);
        tag.putString("RootHintIp", rootHintIp == null ? "" : rootHintIp);
        tag.putString("MasterIp", masterIp == null ? "" : masterIp);
        tag.putBoolean("DnssecValidation", dnssecValidation);
        tag.putString("RootTrustAnchor", rootTrustAnchor == null ? "" : rootTrustAnchor);
        rack.getPersistentData().put(KEY, tag);
        rack.setChanged();
    }

    public boolean physicalEnabled() {
        return role != PhysicalDnsRole.NONE;
    }
}
