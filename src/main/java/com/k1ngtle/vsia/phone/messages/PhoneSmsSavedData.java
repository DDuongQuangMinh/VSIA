package com.k1ngtle.vsia.phone.messages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class PhoneSmsSavedData extends SavedData {
    public static final String DATA_NAME = "vsia_phone_sms";
    private static final int MAX_MESSAGES = 4096;

    private final List<PhoneSmsMessage> messages = new ArrayList<>();

    public PhoneSmsSavedData() {
    }

    public PhoneSmsSavedData(CompoundTag tag) {
        ListTag list = tag.getList("Messages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            messages.add(PhoneSmsMessage.load(list.getCompound(i)));
        }
    }

    public static PhoneSmsSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PhoneSmsSavedData::new,
                        PhoneSmsSavedData::new,
                        DATA_NAME
                );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (PhoneSmsMessage message : messages) {
            list.add(message.save());
        }
        tag.put("Messages", list);
        return tag;
    }

    public synchronized PhoneSmsMessage add(
            String from,
            String to,
            String body,
            String state
    ) {
        PhoneSmsMessage message = new PhoneSmsMessage(
                UUID.randomUUID(),
                from,
                to,
                body,
                System.currentTimeMillis(),
                state
        );
        messages.add(message);
        trim();
        setDirty();
        return message;
    }

    public synchronized List<PhoneSmsMessage> forNumber(String number) {
        String wanted = normalize(number);
        return messages.stream()
                .filter(message -> normalize(message.from()).equals(wanted)
                        || normalize(message.to()).equals(wanted))
                .sorted(Comparator.comparingLong(PhoneSmsMessage::timestampMillis).reversed())
                .limit(100)
                .toList();
    }

    public synchronized void markDeliveredTo(String number) {
        String wanted = normalize(number);
        boolean changed = false;
        for (int i = 0; i < messages.size(); i++) {
            PhoneSmsMessage message = messages.get(i);
            if ("STORED".equalsIgnoreCase(message.state())
                    && normalize(message.to()).equals(wanted)) {
                messages.set(i, message.withState("DELIVERED"));
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private void trim() {
        while (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
    }

    private static String normalize(String number) {
        return number == null ? "" : number.replace(" ", "");
    }
}
