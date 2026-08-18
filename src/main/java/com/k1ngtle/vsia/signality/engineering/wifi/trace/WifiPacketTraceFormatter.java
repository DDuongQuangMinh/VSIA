package com.k1ngtle.vsia.signality.engineering.wifi.trace;

import java.util.Locale;

public final class WifiPacketTraceFormatter {
    private WifiPacketTraceFormatter() {
    }

    public static String compact(
            WifiPacketTraceEvent event
    ) {
        return String.format(
                Locale.ROOT,
                "#%d %s %-6s %s>%s MCS%d %s %.1fdB %s",
                event.sequence(),
                event.direction(),
                event.frameType(),
                shortMac(event.sourceMac()),
                shortMac(event.destinationMac()),
                event.mcsIndex(),
                bytes(event.frameBytes()),
                event.snrDb(),
                event.outcome()
        );
    }

    public static String detail(
            WifiPacketTraceEvent event
    ) {
        return String.format(
                Locale.ROOT,
                "t=%dus %s subtype=%d %s -> %s | %d B | airtime=%dus | RSSI=%s | SNR=%s | SINR=%s | %s | %s",
                event.timestampMicros(),
                event.frameType(),
                event.subtype(),
                event.sourceMac(),
                event.destinationMac(),
                event.frameBytes(),
                event.airtimeMicros(),
                number(event.rssiDbm()),
                number(event.snrDb()),
                number(event.sinrDb()),
                event.detailedPhyPath(),
                event.detail()
        );
    }

    private static String shortMac(String value) {
        if (value == null || value.isBlank()) {
            return "--";
        }

        return value.length() <= 8
                ? value
                : value.substring(
                        Math.max(0, value.length() - 8)
                );
    }

    private static String bytes(int value) {
        return value + "B";
    }

    private static String number(double value) {
        if (!Double.isFinite(value)) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.2f",
                value
        );
    }
}
