package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WifiEngineeringFormatter {
    private WifiEngineeringFormatter() {
    }

    public static List<String> lines(
            WifiEngineeringSnapshot snapshot
    ) {
        List<String> lines =
                new ArrayList<>();

        lines.add(
                "Device "
                        + snapshot.deviceId()
        );

        lines.add(
                "Profile "
                        + snapshot.networkProfile()
                        + " | "
                        + formatFrequency(
                        snapshot.frequencyHz()
                )
        );

        lines.add(
                "MAC "
                        + snapshot.wifiMode()
                        + " | station "
                        + snapshot.stationState()
                        + " | security "
                        + snapshot.securityState()
        );

        lines.add(
                "PHY "
                        + generation(
                        snapshot
                )
                        + " | MCS "
                        + snapshot.mcsIndex()
                        + " | "
                        + snapshot.channelWidthMhz()
                        + " MHz | GI "
                        + formatNumber(
                        snapshot.guardIntervalUs(),
                        2
                )
                        + " us | "
                        + snapshot.spatialStreams()
                        + " SS"
        );

        lines.add(
                "Rate "
                        + formatRate(
                        snapshot.estimatedPhyRateBps()
                )
                        + " | Doppler ICI "
                        + formatPercent(
                        snapshot.dopplerIciFraction()
                )
        );

        lines.add(
                "RSSI "
                        + formatDb(
                        snapshot.receivedPowerDbm()
                )
                        + " dBm | SNR "
                        + formatDb(
                        snapshot.snrDb()
                )
                        + " dB | SINR "
                        + formatDb(
                        snapshot.correctedSinrDb()
                )
                        + " dB"
        );

        lines.add(
                "BER "
                        + formatScientific(
                        snapshot.bitErrorRate()
                )
                        + " | FER "
                        + formatScientific(
                        snapshot.frameErrorRate()
                )
        );

        lines.add(
                "Airtime "
                        + (
                        snapshot.airtimeMicros() < 0L
                                ? "n/a"
                                : snapshot.airtimeMicros()
                                + " us"
                )
                        + " | propagation "
                        + formatNumber(
                        snapshot.propagationDelayMicros(),
                        6
                )
                        + " us"
        );

        lines.add(
                "Interference overlap "
                        + formatPercent(
                        snapshot.temporalInterferenceFactor()
                )
                        + " | capture margin "
                        + formatDb(
                        snapshot.captureMarginDb()
                )
                        + " dB | captured "
                        + yesNo(
                        snapshot.captured()
                )
        );

        lines.add(
                "Medium "
                        + (
                        snapshot.mediumBusy()
                                ? "BUSY"
                                : "IDLE"
                )
                        + " | energy "
                        + formatDb(
                        snapshot.mediumEnergyDbm()
                )
                        + " dBm | overlapping TX "
                        + snapshot.overlappingTransmitters()
        );

        lines.add(
                "Live PHY "
                        + snapshot.liveMode()
                        + " | path "
                        + snapshot.livePath()
                        + " | evaluated "
                        + yesNo(
                        snapshot.liveEvaluated()
                )
                        + " | delivered "
                        + yesNo(
                        snapshot.liveDelivered()
                )
        );

        lines.add(
                "LDPC codewords "
                        + snapshot.liveCodewords()
                        + " | decoder iterations "
                        + snapshot.liveDecoderIterations()
        );

        lines.add(
                "Decision "
                        + snapshot.liveDetail()
        );

        return List.copyOf(
                lines
        );
    }

    private static String generation(
            WifiEngineeringSnapshot snapshot
    ) {
        return snapshot.generation() == null
                ? "UNKNOWN"
                : snapshot.generation().name();
    }

    private static String formatFrequency(
            double hz
    ) {
        if (!Double.isFinite(
                hz
        )) {
            return "n/a";
        }

        if (Math.abs(
                hz
        ) >= 1.0E9) {
            return formatNumber(
                    hz / 1.0E9,
                    6
            )
                    + " GHz";
        }

        if (Math.abs(
                hz
        ) >= 1.0E6) {
            return formatNumber(
                    hz / 1.0E6,
                    3
            )
                    + " MHz";
        }

        return formatNumber(
                hz,
                0
        )
                + " Hz";
    }

    private static String formatRate(
            double bps
    ) {
        if (!Double.isFinite(
                bps
        )) {
            return "n/a";
        }

        if (bps >= 1.0E9) {
            return formatNumber(
                    bps / 1.0E9,
                    3
            )
                    + " Gbit/s";
        }

        if (bps >= 1.0E6) {
            return formatNumber(
                    bps / 1.0E6,
                    3
            )
                    + " Mbit/s";
        }

        if (bps >= 1.0E3) {
            return formatNumber(
                    bps / 1.0E3,
                    3
            )
                    + " kbit/s";
        }

        return formatNumber(
                bps,
                0
        )
                + " bit/s";
    }

    private static String formatPercent(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return formatNumber(
                value * 100.0,
                3
        )
                + "%";
    }

    private static String formatScientific(
            double value
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%.3e",
                value
        );
    }

    private static String formatDb(
            double value
    ) {
        return formatNumber(
                value,
                3
        );
    }

    private static String formatNumber(
            double value,
            int decimals
    ) {
        if (!Double.isFinite(
                value
        )) {
            return "n/a";
        }

        return String.format(
                Locale.ROOT,
                "%."
                        + decimals
                        + "f",
                value
        );
    }

    private static String yesNo(
            boolean value
    ) {
        return value
                ? "yes"
                : "no";
    }
}
