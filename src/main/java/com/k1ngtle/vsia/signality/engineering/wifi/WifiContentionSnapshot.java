package com.k1ngtle.vsia.signality.engineering.wifi;

public record WifiContentionSnapshot(
        int queueDepth,
        int queueCapacity,
        int queuePeak,
        long enqueued,
        long attempts,
        long successes,
        long retries,
        long drops,
        long deferrals,
        int cwVoice,
        int cwVideo,
        int cwBestEffort,
        int cwBackground
) {
    public String compact() {
        return "W1.23 q="
                + queueDepth
                + "/"
                + queueCapacity
                + " peak="
                + queuePeak
                + " tx="
                + attempts
                + " ok="
                + successes
                + " retry="
                + retries
                + " drop="
                + drops
                + " defer="
                + deferrals
                + " cwBE="
                + cwBestEffort;
    }
}
