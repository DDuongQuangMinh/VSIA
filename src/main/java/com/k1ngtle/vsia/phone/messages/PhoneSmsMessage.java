package com.k1ngtle.vsia.phone.messages;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public record PhoneSmsMessage(
        UUID id,
        String from,
        String to,
        String body,
        long timestampMillis,
        String state
) {
    public PhoneSmsMessage {
        id = id == null ? UUID.randomUUID() : id;
        from = safe(from);
        to = safe(to);
        body = safe(body);
        state = safe(state);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("From", from);
        tag.putString("To", to);
        tag.putString("Body", body);
        tag.putLong("Timestamp", timestampMillis);
        tag.putString("State", state);
        return tag;
    }

    public static PhoneSmsMessage load(CompoundTag tag) {
        return new PhoneSmsMessage(
                tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID(),
                tag.getString("From"),
                tag.getString("To"),
                tag.getString("Body"),
                tag.getLong("Timestamp"),
                tag.getString("State")
        );
    }

    public PhoneSmsMessage withState(String newState) {
        return new PhoneSmsMessage(
                id,
                from,
                to,
                body,
                timestampMillis,
                newState
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
