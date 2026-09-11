package com.k1ngtle.vsia.signality.engineering.cellular.core;

import com.k1ngtle.vsia.signality.engineering.cellular.nas.FiveGAkaEngine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class FiveGCore {
    private final Map<String, byte[]> subscriberKeys =
            new HashMap<>();

    private final Map<UUID, String> registeredUes =
            new HashMap<>();

    private final Map<UUID, PduSession> sessions =
            new HashMap<>();

    private final AtomicInteger nextSessionId =
            new AtomicInteger(1);

    private int nextHost = 2;

    public void provisionSubscriber(
            String supi,
            byte[] subscriberKey
    ) {
        subscriberKeys.put(
                supi,
                subscriberKey.clone()
        );
    }

    public boolean hasSubscriber(String supi) {
        return subscriberKeys.containsKey(supi);
    }

    public byte[] subscriberKey(String supi) {
        byte[] key = subscriberKeys.get(supi);
        return key == null ? null : key.clone();
    }

    public boolean authenticate(
            String supi,
            byte[] challenge,
            byte[] response
    ) {
        byte[] key = subscriberKeys.get(supi);

        if (key == null) {
            return false;
        }

        byte[] expected =
                FiveGAkaEngine.calculateResponse(
                        key,
                        supi,
                        challenge
                );

        return Arrays.equals(
                expected,
                response
        );
    }

    public void register(
            UUID ueId,
            String supi
    ) {
        registeredUes.put(
                ueId,
                supi
        );
    }

    public boolean isRegistered(UUID ueId) {
        return registeredUes.containsKey(ueId);
    }

    public PduSession establishSession(
            UUID ueId,
            String dnn,
            int fiveQi
    ) {
        if (!isRegistered(ueId)) {
            return null;
        }

        String ipAddress =
                "10.0.0."
                        + Math.max(
                        2,
                        Math.min(
                                254,
                                nextHost++
                        )
                );

        PduSession session =
                new PduSession(
                        nextSessionId.getAndIncrement(),
                        ueId,
                        dnn,
                        ipAddress,
                        Math.max(1, fiveQi),
                        true
                );

        sessions.put(
                ueId,
                session
        );

        return session;
    }

    public PduSession session(UUID ueId) {
        return sessions.get(ueId);
    }

    public net.minecraft.nbt.CompoundTag save() {
        net.minecraft.nbt.CompoundTag tag =
                new net.minecraft.nbt.CompoundTag();

        tag.putInt(
                "NextSessionId",
                nextSessionId.get()
        );

        tag.putInt(
                "NextHost",
                nextHost
        );

        net.minecraft.nbt.ListTag subscribers =
                new net.minecraft.nbt.ListTag();

        subscriberKeys.entrySet()
                .stream()
                .sorted(
                        java.util.Map.Entry.comparingByKey()
                )
                .forEach(entry -> {
                    net.minecraft.nbt.CompoundTag row =
                            new net.minecraft.nbt.CompoundTag();

                    row.putString(
                            "Supi",
                            entry.getKey()
                    );

                    row.putByteArray(
                            "Key",
                            entry.getValue()
                    );

                    subscribers.add(
                            row
                    );
                });

        tag.put(
                "Subscribers",
                subscribers
        );

        net.minecraft.nbt.ListTag registrations =
                new net.minecraft.nbt.ListTag();

        registeredUes.forEach(
                (ueId, supi) -> {
                    net.minecraft.nbt.CompoundTag row =
                            new net.minecraft.nbt.CompoundTag();

                    row.putUUID(
                            "UeId",
                            ueId
                    );

                    row.putString(
                            "Supi",
                            supi
                    );

                    registrations.add(
                            row
                    );
                }
        );

        tag.put(
                "Registrations",
                registrations
        );

        net.minecraft.nbt.ListTag savedSessions =
                new net.minecraft.nbt.ListTag();

        sessions.values()
                .stream()
                .filter(
                        PduSession::active
                )
                .forEach(session -> {
                    net.minecraft.nbt.CompoundTag row =
                            new net.minecraft.nbt.CompoundTag();

                    row.putInt(
                            "SessionId",
                            session.sessionId()
                    );

                    row.putUUID(
                            "UeId",
                            session.ueId()
                    );

                    row.putString(
                            "Dnn",
                            session.dnn()
                    );

                    row.putString(
                            "IpAddress",
                            session.ipAddress()
                    );

                    row.putInt(
                            "FiveQi",
                            session.fiveQi()
                    );

                    savedSessions.add(
                            row
                    );
                });

        tag.put(
                "Sessions",
                savedSessions
        );

        return tag;
    }

    public void load(
            net.minecraft.nbt.CompoundTag tag
    ) {
        subscriberKeys.clear();
        registeredUes.clear();
        sessions.clear();

        if (tag == null) {
            nextSessionId.set(
                    1
            );

            nextHost =
                    2;

            return;
        }

        nextSessionId.set(
                Math.max(
                        1,
                        tag.getInt(
                                "NextSessionId"
                        )
                )
        );

        nextHost =
                Math.max(
                        2,
                        Math.min(
                                254,
                                tag.getInt(
                                        "NextHost"
                                )
                        )
                );

        if (tag.contains(
                "Subscribers",
                net.minecraft.nbt.Tag.TAG_LIST
        )) {
            net.minecraft.nbt.ListTag subscribers =
                    tag.getList(
                            "Subscribers",
                            net.minecraft.nbt.Tag.TAG_COMPOUND
                    );

            for (int i = 0;
                 i < subscribers.size();
                 i++) {
                net.minecraft.nbt.CompoundTag row =
                        subscribers.getCompound(
                                i
                        );

                String supi =
                        row.getString(
                                "Supi"
                        );

                byte[] key =
                        row.getByteArray(
                                "Key"
                        );

                if (!supi.isBlank()
                        && key.length >= 16) {
                    subscriberKeys.put(
                            supi,
                            key
                    );
                }
            }
        }

        if (tag.contains(
                "Registrations",
                net.minecraft.nbt.Tag.TAG_LIST
        )) {
            net.minecraft.nbt.ListTag registrations =
                    tag.getList(
                            "Registrations",
                            net.minecraft.nbt.Tag.TAG_COMPOUND
                    );

            for (int i = 0;
                 i < registrations.size();
                 i++) {
                net.minecraft.nbt.CompoundTag row =
                        registrations.getCompound(
                                i
                        );

                if (!row.hasUUID(
                        "UeId"
                )) {
                    continue;
                }

                java.util.UUID ueId =
                        row.getUUID(
                                "UeId"
                        );

                String supi =
                        row.getString(
                                "Supi"
                        );

                if (subscriberKeys.containsKey(
                        supi
                )) {
                    registeredUes.put(
                            ueId,
                            supi
                    );
                }
            }
        }

        int highestSessionId =
                0;

        if (tag.contains(
                "Sessions",
                net.minecraft.nbt.Tag.TAG_LIST
        )) {
            net.minecraft.nbt.ListTag savedSessions =
                    tag.getList(
                            "Sessions",
                            net.minecraft.nbt.Tag.TAG_COMPOUND
                    );

            for (int i = 0;
                 i < savedSessions.size();
                 i++) {
                net.minecraft.nbt.CompoundTag row =
                        savedSessions.getCompound(
                                i
                        );

                if (!row.hasUUID(
                        "UeId"
                )) {
                    continue;
                }

                java.util.UUID ueId =
                        row.getUUID(
                                "UeId"
                        );

                if (!registeredUes.containsKey(
                        ueId
                )) {
                    continue;
                }

                int sessionId =
                        Math.max(
                                1,
                                row.getInt(
                                        "SessionId"
                                )
                        );

                highestSessionId =
                        Math.max(
                                highestSessionId,
                                sessionId
                        );

                sessions.put(
                        ueId,
                        new PduSession(
                                sessionId,
                                ueId,
                                row.getString(
                                        "Dnn"
                                ),
                                row.getString(
                                        "IpAddress"
                                ),
                                Math.max(
                                        1,
                                        row.getInt(
                                                "FiveQi"
                                        )
                                ),
                                true
                        )
                );
            }
        }

        nextSessionId.set(
                Math.max(
                        nextSessionId.get(),
                        highestSessionId + 1
                )
        );
    }

}
