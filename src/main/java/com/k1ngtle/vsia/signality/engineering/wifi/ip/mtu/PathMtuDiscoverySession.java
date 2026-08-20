package com.k1ngtle.vsia.signality.engineering.wifi.ip.mtu;

public final class PathMtuDiscoverySession {
    private final long sessionId;
    private final String destinationIp;
    private final int initialProbeBytes;

    private int currentProbeBytes;
    private int learnedMtu;
    private boolean running =
            true;
    private boolean complete;
    private String status =
            "PROBING";

    public PathMtuDiscoverySession(
            long sessionId,
            String destinationIp,
            int initialProbeBytes
    ) {
        if (destinationIp == null
                || destinationIp.isBlank()) {
            throw new IllegalArgumentException(
                    "destinationIp is blank"
            );
        }

        if (initialProbeBytes < 68
                || initialProbeBytes > 65535) {
            throw new IllegalArgumentException(
                    "Initial IPv4 PMTU probe must be 68..65535 bytes"
            );
        }

        this.sessionId =
                sessionId;
        this.destinationIp =
                destinationIp;
        this.initialProbeBytes =
                initialProbeBytes;
        this.currentProbeBytes =
                initialProbeBytes;
    }

    public boolean onFragmentationNeeded(
            int nextHopMtu
    ) {
        if (!running
                || nextHopMtu < 68
                || nextHopMtu >= currentProbeBytes) {
            return false;
        }

        learnedMtu =
                nextHopMtu;

        currentProbeBytes =
                nextHopMtu;

        status =
                "RETRY_AT_ADVERTISED_MTU";

        return true;
    }

    public boolean onEchoReply(
            long responseSessionId,
            int responseProbeBytes,
            String responderIp
    ) {
        if (!running
                || responseSessionId != sessionId
                || responseProbeBytes != currentProbeBytes
                || !destinationIp.equals(
                responderIp
        )) {
            return false;
        }

        running =
                false;
        complete =
                true;

        if (learnedMtu == 0) {
            learnedMtu =
                    currentProbeBytes;
        }

        status =
                "PATH_MTU_CONFIRMED";

        return true;
    }

    public long sessionId() {
        return sessionId;
    }

    public String destinationIp() {
        return destinationIp;
    }

    public int initialProbeBytes() {
        return initialProbeBytes;
    }

    public int currentProbeBytes() {
        return currentProbeBytes;
    }

    public int learnedMtu() {
        return learnedMtu;
    }

    public boolean running() {
        return running;
    }

    public boolean complete() {
        return complete;
    }

    public String status() {
        return status;
    }
}
