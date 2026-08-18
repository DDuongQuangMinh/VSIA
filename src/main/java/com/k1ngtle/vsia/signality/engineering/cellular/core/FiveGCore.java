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
}
