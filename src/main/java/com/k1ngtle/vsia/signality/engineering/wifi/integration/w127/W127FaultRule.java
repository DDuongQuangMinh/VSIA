package com.k1ngtle.vsia.signality.engineering.wifi.integration.w127;

import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.Locale;
import java.util.UUID;

public final class W127FaultRule {
    private final UUID id;
    private final W127FaultAction action;
    private final String sourceIp;
    private final String targetIp;
    private final String applicationProtocol;
    private final String pdnsKind;
    private final String xfrKind;
    private final Integer sequence;
    private final Boolean response;
    private final long delayTicks;
    private final long forcedSerial;

    private int remainingMatches;

    public W127FaultRule(
            W127FaultAction action,
            String sourceIp,
            String targetIp,
            String applicationProtocol,
            String pdnsKind,
            String xfrKind,
            Integer sequence,
            Boolean response,
            int remainingMatches,
            long delayTicks,
            long forcedSerial
    ) {
        this.id = UUID.randomUUID();
        this.action = action;
        this.sourceIp = normalize(sourceIp);
        this.targetIp = normalize(targetIp);
        this.applicationProtocol = normalize(applicationProtocol);
        this.pdnsKind = normalize(pdnsKind);
        this.xfrKind = normalize(xfrKind);
        this.sequence = sequence;
        this.response = response;
        this.remainingMatches = remainingMatches == 0 ? 1 : remainingMatches;
        this.delayTicks = W127ResiliencePolicy.boundedDelay(delayTicks);
        this.forcedSerial = forcedSerial;
    }

    public UUID id() {
        return id;
    }

    public W127FaultAction action() {
        return action;
    }

    public long delayTicks() {
        return delayTicks;
    }

    public long forcedSerial() {
        return forcedSerial;
    }

    public int remainingMatches() {
        return remainingMatches;
    }

    public boolean exhausted() {
        return remainingMatches == 0;
    }

    public boolean matches(OSINetworkPacket packet) {
        if (packet == null) {
            return false;
        }

        if (!match(sourceIp, packet.sourceIp)) {
            return false;
        }

        if (!match(targetIp, packet.targetIp)) {
            return false;
        }

        if (!match(applicationProtocol, packet.applicationProtocol)) {
            return false;
        }

        if (!match(
                pdnsKind,
                packet.payload.getString("pdns_kind")
        )) {
            return false;
        }

        if (!match(
                xfrKind,
                packet.payload.getString("xfr_kind")
        )) {
            return false;
        }

        if (sequence != null
                && packet.payload.getInt("sequence") != sequence) {
            return false;
        }

        return response == null
                || packet.isResponse == response;
    }

    public boolean consume() {
        if (remainingMatches < 0) {
            return true;
        }

        if (remainingMatches == 0) {
            return false;
        }

        remainingMatches--;
        return true;
    }

    public String compact() {
        return action
                + " "
                + printable(sourceIp)
                + "->"
                + printable(targetIp)
                + " app="
                + printable(applicationProtocol)
                + " pdns="
                + printable(pdnsKind)
                + " xfr="
                + printable(xfrKind)
                + " seq="
                + (sequence == null ? "*" : sequence)
                + " remaining="
                + remainingMatches;
    }

    private static boolean match(
            String expected,
            String actual
    ) {
        return expected.isBlank()
                || expected.equalsIgnoreCase(
                normalize(actual)
        );
    }

    private static String normalize(String value) {
        if (value == null || "*".equals(value)) {
            return "";
        }

        return value.trim().toUpperCase(
                Locale.ROOT
        );
    }

    private static String printable(String value) {
        return value.isBlank() ? "*" : value;
    }
}
