package com.k1ngtle.vsia.signality.internet.provider;

import net.minecraft.nbt.CompoundTag;

public record InternetDnsRecord(
        String zone,
        String name,
        String type,
        String value,
        int ttlSeconds,
        int priority,
        int weight,
        int port
) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Zone", zone);
        tag.putString("Name", name);
        tag.putString("Type", type);
        tag.putString("Value", value);
        tag.putInt("Ttl", ttlSeconds);
        tag.putInt("Priority", priority);
        tag.putInt("Weight", weight);
        tag.putInt("Port", port);
        return tag;
    }

    public static InternetDnsRecord load(CompoundTag tag) {
        return new InternetDnsRecord(
                tag.getString("Zone"),
                tag.getString("Name"),
                tag.getString("Type"),
                tag.getString("Value"),
                Math.max(30, tag.getInt("Ttl")),
                tag.getInt("Priority"),
                tag.getInt("Weight"),
                tag.getInt("Port")
        );
    }

    public String key() {
        return type + "|" + name + "|" + priority + "|" + weight + "|" + port + "|" + value;
    }

    public String answerValue() {
        return switch (type) {
            case "MX" -> priority + " " + value;
            case "SRV" -> priority + " " + weight + " " + port + " " + value;
            default -> value;
        };
    }
}
