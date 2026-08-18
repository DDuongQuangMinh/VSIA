package com.k1ngtle.vsia.signality.engineering.wifi.ip;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WifiIpFlowTracker {
    private static final int MAX_PENDING =
            128;

    private final Map<Long, Pending> pending =
            new LinkedHashMap<>();

    private int txPackets;
    private int rxPackets;
    private long txBytes;
    private long rxBytes;
    private int lostPackets;

    private double lastRttMs =
            Double.NaN;

    private double averageRttMs =
            Double.NaN;

    private double jitterMs =
            Double.NaN;

    private long firstRxMicros =
            -1L;

    private long lastRxMicros =
            -1L;

    private String lastProtocol =
            "";

    private String lastStatus =
            "IDLE";

    public synchronized void recordTx(
            long id,
            long nowMicros,
            int bytes,
            String protocol
    ) {
        txPackets++;

        txBytes +=
                Math.max(
                        0,
                        bytes
                );

        lastProtocol =
                protocol == null
                        ? ""
                        : protocol;

        lastStatus =
                "TX "
                        + lastProtocol;

        pending.put(
                id,
                new Pending(
                        nowMicros,
                        Math.max(
                                0,
                                bytes
                        ),
                        lastProtocol
                )
        );

        while (pending.size() > MAX_PENDING) {
            Long oldest =
                    pending.keySet()
                            .iterator()
                            .next();

            pending.remove(
                    oldest
            );

            lostPackets++;
        }
    }

    public synchronized void recordRx(
            long id,
            long nowMicros,
            int bytes,
            String protocol
    ) {
        rxPackets++;

        rxBytes +=
                Math.max(
                        0,
                        bytes
                );

        lastProtocol =
                protocol == null
                        ? ""
                        : protocol;

        lastStatus =
                "RX "
                        + lastProtocol;

        if (firstRxMicros < 0L) {
            firstRxMicros =
                    nowMicros;
        }

        lastRxMicros =
                nowMicros;

        Pending tx =
                pending.remove(
                        id
                );

        if (tx == null) {
            return;
        }

        double rtt =
                Math.max(
                        0L,
                        nowMicros
                                - tx.sentMicros()
                )
                        / 1000.0;

        if (Double.isFinite(
                lastRttMs
        )) {
            double delta =
                    Math.abs(
                            rtt
                                    - lastRttMs
                    );

            jitterMs =
                    Double.isFinite(
                            jitterMs
                    )
                            ? jitterMs
                            + (
                            delta
                                    - jitterMs
                    )
                            / 16.0
                            : delta;
        }

        lastRttMs =
                rtt;

        int completed =
                Math.max(
                        1,
                        rxPackets
                );

        averageRttMs =
                Double.isFinite(
                        averageRttMs
                )
                        ? averageRttMs
                        + (
                        rtt
                                - averageRttMs
                )
                        / completed
                        : rtt;
    }

    public synchronized void expire(
            long nowMicros,
            long timeoutMicros
    ) {
        pending.entrySet()
                .removeIf(
                        entry -> {
                            boolean expired =
                                    nowMicros
                                            - entry.getValue()
                                            .sentMicros()
                                            >= timeoutMicros;

                            if (expired) {
                                lostPackets++;
                            }

                            return expired;
                        }
                );
    }

    public synchronized WifiIpFlowSnapshot snapshot(
            String localIp,
            String peerIp,
            String peerMac
    ) {
        double goodputKbps =
                0.0;

        if (firstRxMicros >= 0L
                && lastRxMicros > firstRxMicros
                && rxBytes > 0L) {
            double seconds =
                    (
                            lastRxMicros
                                    - firstRxMicros
                    )
                            / 1_000_000.0;

            goodputKbps =
                    (
                            rxBytes
                                    * 8.0
                    )
                            / seconds
                            / 1000.0;
        }

        return new WifiIpFlowSnapshot(
                localIp == null
                        ? ""
                        : localIp,
                peerIp == null
                        ? ""
                        : peerIp,
                peerMac == null
                        ? ""
                        : peerMac,
                txPackets,
                rxPackets,
                txBytes,
                rxBytes,
                lostPackets,
                lastRttMs,
                averageRttMs,
                jitterMs,
                goodputKbps,
                lastProtocol,
                lastStatus
        );
    }

    public synchronized void clear() {
        pending.clear();
        txPackets = 0;
        rxPackets = 0;
        txBytes = 0L;
        rxBytes = 0L;
        lostPackets = 0;
        lastRttMs = Double.NaN;
        averageRttMs = Double.NaN;
        jitterMs = Double.NaN;
        firstRxMicros = -1L;
        lastRxMicros = -1L;
        lastProtocol = "";
        lastStatus = "CLEARED";
    }

    private record Pending(
            long sentMicros,
            int bytes,
            String protocol
    ) {
    }
}
