package com.k1ngtle.vsia.phone.client;

public final class PhoneClockClientState {
    private static boolean running;
    private static long accumulatedMillis;
    private static long startedAtMillis;

    private PhoneClockClientState() {
    }

    public static boolean running() {
        return running;
    }

    public static void start() {
        if (running) {
            return;
        }

        running = true;
        startedAtMillis = System.currentTimeMillis();
    }

    public static void stop() {
        if (!running) {
            return;
        }

        accumulatedMillis +=
                System.currentTimeMillis()
                        - startedAtMillis;

        running = false;
    }

    public static void reset() {
        running = false;
        accumulatedMillis = 0L;
        startedAtMillis = 0L;
    }

    public static long elapsedMillis() {
        if (!running) {
            return accumulatedMillis;
        }

        return accumulatedMillis
                + System.currentTimeMillis()
                - startedAtMillis;
    }

    public static String formattedElapsed() {
        long millis = elapsedMillis();

        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        long hundredths = (millis % 1000L) / 10L;

        return String.format(
                "%02d:%02d.%02d",
                minutes,
                seconds,
                hundredths
        );
    }
}
