package com.k1ngtle.vsia.signality.internet.radio.comsec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.security.SecureRandom;
import java.util.Arrays;

public final class RadioComsecKeyStoreSavedData extends SavedData {
    public static final int MIN_SLOT = 1;
    public static final int MAX_SLOT = 8;

    private static final String DATA_NAME =
            "vsia_radio_comsec_keys_v1";

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private final byte[][] keys =
            new byte[MAX_SLOT + 1][];

    private final int[] epochs =
            new int[MAX_SLOT + 1];

    public RadioComsecKeyStoreSavedData() {
        ensureAll();
    }

    public static RadioComsecKeyStoreSavedData get(
            ServerLevel level
    ) {
        return level.getDataStorage()
                .computeIfAbsent(
                        RadioComsecKeyStoreSavedData::load,
                        RadioComsecKeyStoreSavedData::new,
                        DATA_NAME
                );
    }

    public static RadioComsecKeyStoreSavedData load(
            CompoundTag tag
    ) {
        RadioComsecKeyStoreSavedData data =
                new RadioComsecKeyStoreSavedData();

        for (int slot = MIN_SLOT;
             slot <= MAX_SLOT;
             slot++) {
            String keyName =
                    "Key" + slot;

            String epochName =
                    "Epoch" + slot;

            if (tag.contains(keyName)) {
                byte[] stored =
                        tag.getByteArray(
                                keyName
                        );

                if (stored.length == 32) {
                    data.keys[slot] =
                            stored.clone();
                }
            }

            if (tag.contains(epochName)) {
                data.epochs[slot] =
                        Math.max(
                                1,
                                tag.getInt(
                                        epochName
                                )
                        );
            }
        }

        data.ensureAll();
        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag
    ) {
        ensureAll();

        for (int slot = MIN_SLOT;
             slot <= MAX_SLOT;
             slot++) {
            tag.putByteArray(
                    "Key" + slot,
                    keys[slot]
            );

            tag.putInt(
                    "Epoch" + slot,
                    epochs[slot]
            );
        }

        return tag;
    }

    public synchronized KeyMaterial material(
            int requestedSlot
    ) {
        int slot =
                normalizeSlot(
                        requestedSlot
                );

        ensure(
                slot
        );

        return new KeyMaterial(
                slot,
                epochs[slot],
                keys[slot].clone()
        );
    }

    public synchronized KeyMaterial rotate(
            int requestedSlot
    ) {
        int slot =
                normalizeSlot(
                        requestedSlot
                );

        byte[] key =
                new byte[32];

        RANDOM.nextBytes(
                key
        );

        keys[slot] =
                key;

        epochs[slot] =
                Math.max(
                        1,
                        epochs[slot] + 1
                );

        setDirty();

        return material(
                slot
        );
    }

    public static int normalizeSlot(
            int slot
    ) {
        return Math.max(
                MIN_SLOT,
                Math.min(
                        MAX_SLOT,
                        slot
                )
        );
    }

    private void ensureAll() {
        for (int slot = MIN_SLOT;
             slot <= MAX_SLOT;
             slot++) {
            ensure(
                    slot
            );
        }
    }

    private void ensure(
            int slot
    ) {
        boolean changed =
                false;

        if (keys[slot] == null
                || keys[slot].length != 32) {
            byte[] key =
                    new byte[32];

            RANDOM.nextBytes(
                    key
            );

            keys[slot] =
                    key;

            changed =
                    true;
        }

        if (epochs[slot] <= 0) {
            epochs[slot] =
                    1;

            changed =
                    true;
        }

        if (changed) {
            setDirty();
        }
    }

    public record KeyMaterial(
            int slot,
            int epoch,
            byte[] key
    ) {
        public KeyMaterial {
            key =
                    key == null
                            ? new byte[0]
                            : key.clone();
        }

        @Override
        public byte[] key() {
            return key.clone();
        }

        public String keyId() {
            return String.format(
                    "TEK-%02d/%d",
                    slot,
                    epoch
            );
        }
    }
}
