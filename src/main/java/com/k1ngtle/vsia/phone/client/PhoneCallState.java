package com.k1ngtle.vsia.phone.client;

import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;

public final class PhoneCallState {
    private static String number = "";
    private static long startedAt;
    private static boolean active;

    private PhoneCallState() {
    }

    public static synchronized boolean active() {
        return active;
    }

    public static synchronized String number() {
        return number;
    }

    public static synchronized void start(String targetNumber) {
        if (targetNumber == null
                || targetNumber.isBlank()) {
            return;
        }

        number = targetNumber.trim();
        startedAt = System.currentTimeMillis();
        active = true;
    }

    public static synchronized void end() {
        active = false;
        number = "";
        startedAt = 0L;
    }

    public static synchronized long elapsedSeconds() {
        if (!active) {
            return 0L;
        }

        return Math.max(
                0L,
                (System.currentTimeMillis() - startedAt) / 1000L
        );
    }

    public static synchronized String status() {
        if (!active) {
            return "Ended";
        }

        if (!PhoneSubscriberClientState.get()
                .hasActiveSubscription()) {
            return "No Service";
        }

        long seconds = elapsedSeconds();

        if (seconds < 2L) {
            return "Calling...";
        }

        return "Connected";
    }
}
