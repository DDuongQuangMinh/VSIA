package com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute;

import java.util.ArrayList;
import java.util.List;

public final class WifiTracerouteSession {
    public static final int MAX_ATTEMPTS_PER_HOP =
            3;

    private final long traceId;
    private final String destinationIp;
    private final int maxHops;
    private final List<WifiTracerouteHop> hops =
            new ArrayList<>();

    private int currentTtl =
            1;

    private int currentAttempt =
            0;

    private long sentMicros =
            -1L;

    private boolean waiting;
    private boolean running =
            true;

    private boolean destinationReached;

    private String finalStatus =
            "RUNNING";

    public WifiTracerouteSession(
            long traceId,
            String destinationIp,
            int maxHops
    ) {
        if (destinationIp == null
                || destinationIp.isBlank()) {
            throw new IllegalArgumentException(
                    "destinationIp is blank"
            );
        }

        if (maxHops < 1
                || maxHops > 255) {
            throw new IllegalArgumentException(
                    "maxHops must be 1..255"
            );
        }

        this.traceId =
                traceId;

        this.destinationIp =
                destinationIp;

        this.maxHops =
                maxHops;
    }

    public boolean needsProbe() {
        return running
                && !waiting;
    }

    public ProbeRequest beginProbe(
            long nowMicros
    ) {
        if (!needsProbe()) {
            throw new IllegalStateException(
                    "Traceroute is not ready for another probe"
            );
        }

        currentAttempt++;
        sentMicros =
                nowMicros;

        waiting =
                true;

        return new ProbeRequest(
                traceId,
                destinationIp,
                currentTtl,
                currentAttempt
        );
    }

    public boolean matchesQuotedProbe(
            String quotedSourceIp,
            String quotedDestinationIp,
            int quotedProtocol
    ) {
        return running
                && waiting
                && quotedProtocol == 1
                && quotedSourceIp != null
                && !quotedSourceIp.isBlank()
                && destinationIp.equals(
                quotedDestinationIp
        );
    }

    public boolean onTimeExceeded(
            String responderIp,
            long nowMicros
    ) {
        if (!running
                || !waiting
                || responderIp == null
                || responderIp.isBlank()) {
            return false;
        }

        hops.add(
                new WifiTracerouteHop(
                        currentTtl,
                        responderIp,
                        currentAttempt,
                        rttMs(
                                nowMicros
                        ),
                        "TIME_EXCEEDED"
                )
        );

        waiting =
                false;

        currentAttempt =
                0;

        advanceHop();

        return true;
    }

    public boolean onDestinationUnreachable(
            String responderIp,
            int code,
            long nowMicros
    ) {
        if (!running
                || !waiting) {
            return false;
        }

        hops.add(
                new WifiTracerouteHop(
                        currentTtl,
                        responderIp == null
                                || responderIp.isBlank()
                                ? "*"
                                : responderIp,
                        currentAttempt,
                        rttMs(
                                nowMicros
                        ),
                        "DESTINATION_UNREACHABLE code="
                                + code
                )
        );

        waiting =
                false;

        running =
                false;

        finalStatus =
                "DESTINATION_UNREACHABLE";

        return true;
    }

    public boolean matchesEchoReply(
            long responseTraceId,
            int responseTtl,
            int responseAttempt,
            String responderIp
    ) {
        return running
                && waiting
                && responseTraceId == traceId
                && responseTtl == currentTtl
                && responseAttempt == currentAttempt
                && destinationIp.equals(
                responderIp
        );
    }

    public boolean onEchoReply(
            String responderIp,
            long nowMicros
    ) {
        if (!running
                || !waiting
                || !destinationIp.equals(
                responderIp
        )) {
            return false;
        }

        hops.add(
                new WifiTracerouteHop(
                        currentTtl,
                        responderIp,
                        currentAttempt,
                        rttMs(
                                nowMicros
                        ),
                        "DESTINATION"
                )
        );

        waiting =
                false;

        running =
                false;

        destinationReached =
                true;

        finalStatus =
                "DESTINATION_REACHED";

        return true;
    }

    public TimeoutAction onTimeout() {
        if (!running
                || !waiting) {
            return TimeoutAction.NONE;
        }

        waiting =
                false;

        if (currentAttempt
                < MAX_ATTEMPTS_PER_HOP) {
            return TimeoutAction.RETRY;
        }

        hops.add(
                new WifiTracerouteHop(
                        currentTtl,
                        "*",
                        currentAttempt,
                        Double.NaN,
                        "TIMEOUT"
                )
        );

        currentAttempt =
                0;

        boolean canContinue =
                currentTtl
                        < maxHops;

        if (canContinue) {
            currentTtl++;
            return TimeoutAction.NEXT_HOP;
        }

        running =
                false;

        finalStatus =
                "MAX_HOPS_REACHED";

        return TimeoutAction.FINISHED;
    }

    public WifiTracerouteSnapshot snapshot() {
        return new WifiTracerouteSnapshot(
                traceId,
                destinationIp,
                maxHops,
                currentTtl,
                currentAttempt,
                running,
                destinationReached,
                finalStatus,
                hops
        );
    }

    public boolean waiting() {
        return waiting;
    }

    public long sentMicros() {
        return sentMicros;
    }

    public long traceId() {
        return traceId;
    }

    public String destinationIp() {
        return destinationIp;
    }

    public int currentTtl() {
        return currentTtl;
    }

    public int currentAttempt() {
        return currentAttempt;
    }

    public boolean running() {
        return running;
    }

    private void advanceHop() {
        if (currentTtl
                >= maxHops) {
            running =
                    false;

            finalStatus =
                    "MAX_HOPS_REACHED";

            return;
        }

        currentTtl++;
    }

    private double rttMs(
            long nowMicros
    ) {
        return Math.max(
                0L,
                nowMicros
                        - sentMicros
        ) / 1000.0;
    }

    public record ProbeRequest(
            long traceId,
            String destinationIp,
            int ttl,
            int attempt
    ) {
    }

    public enum TimeoutAction {
        NONE,
        RETRY,
        NEXT_HOP,
        FINISHED
    }
}
