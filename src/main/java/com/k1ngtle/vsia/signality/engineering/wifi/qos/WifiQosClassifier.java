package com.k1ngtle.vsia.signality.engineering.wifi.qos;

import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.Locale;

public final class WifiQosClassifier {
    private WifiQosClassifier() {
    }

    public static WifiAccessCategory classify(
            OSINetworkPacket packet
    ) {
        if (packet == null) {
            return WifiAccessCategory.BEST_EFFORT;
        }

        int dscp =
                packet.dscp;

        if (dscp > 0
                && dscp <= 63) {
            return fromDscp(
                    dscp
            );
        }

        return fromApplicationAndPorts(
                packet.applicationProtocol,
                packet.sourcePort,
                packet.targetPort
        );
    }

    public static WifiAccessCategory fromDscp(
            int dscp
    ) {
        if (dscp < 0
                || dscp > 63) {
            throw new IllegalArgumentException(
                    "DSCP must be in 0..63"
            );
        }

        return switch (dscp) {
            case 44, 46, 48, 56 ->
                    WifiAccessCategory.VOICE;

            case 32, 34, 36, 38 ->
                    WifiAccessCategory.VIDEO;

            case 1, 8 ->
                    WifiAccessCategory.BACKGROUND;

            default ->
                    WifiAccessCategory.BEST_EFFORT;
        };
    }

    public static WifiAccessCategory fromApplicationAndPorts(
            String applicationProtocol,
            int sourcePort,
            int targetPort
    ) {
        String application =
                applicationProtocol == null
                        ? ""
                        : applicationProtocol
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (application.contains(
                "VOICE"
        )
                || application.contains(
                "SIP"
        )
                || application.contains(
                "RTP"
        )
                || isVoicePort(
                sourcePort
        )
                || isVoicePort(
                targetPort
        )) {
            return WifiAccessCategory.VOICE;
        }

        if (application.contains(
                "VIDEO"
        )
                || application.contains(
                "RTSP"
        )
                || application.contains(
                "STREAM"
        )
                || isVideoPort(
                sourcePort
        )
                || isVideoPort(
                targetPort
        )) {
            return WifiAccessCategory.VIDEO;
        }

        if (application.contains(
                "BACKGROUND"
        )
                || application.contains(
                "BACKUP"
        )
                || application.contains(
                "FTP"
        )
                || application.contains(
                "SMTP"
        )
                || isBackgroundPort(
                sourcePort
        )
                || isBackgroundPort(
                targetPort
        )) {
            return WifiAccessCategory.BACKGROUND;
        }

        return WifiAccessCategory.BEST_EFFORT;
    }

    public static int userPriority(
            WifiAccessCategory category
    ) {
        WifiAccessCategory value =
                category == null
                        ? WifiAccessCategory.BEST_EFFORT
                        : category;

        return switch (value) {
            case VOICE -> 6;
            case VIDEO -> 5;
            case BEST_EFFORT -> 0;
            case BACKGROUND -> 1;
        };
    }

    public static WifiAccessCategory fromUserPriority(
            int userPriority
    ) {
        if (userPriority < 0
                || userPriority > 7) {
            throw new IllegalArgumentException(
                    "802.1D/WMM user priority must be in 0..7"
            );
        }

        return switch (userPriority) {
            case 6, 7 ->
                    WifiAccessCategory.VOICE;

            case 4, 5 ->
                    WifiAccessCategory.VIDEO;

            case 1, 2 ->
                    WifiAccessCategory.BACKGROUND;

            default ->
                    WifiAccessCategory.BEST_EFFORT;
        };
    }

    public static int defaultDscp(
            WifiAccessCategory category
    ) {
        WifiAccessCategory value =
                category == null
                        ? WifiAccessCategory.BEST_EFFORT
                        : category;

        return switch (value) {
            case VOICE -> 46;
            case VIDEO -> 34;
            case BEST_EFFORT -> 0;
            case BACKGROUND -> 8;
        };
    }

    public static int dscpEcnByte(
            int dscp,
            int ecn
    ) {
        if (dscp < 0
                || dscp > 63) {
            throw new IllegalArgumentException(
                    "DSCP must be in 0..63"
            );
        }

        if (ecn < 0
                || ecn > 3) {
            throw new IllegalArgumentException(
                    "ECN must be in 0..3"
            );
        }

        return (
                dscp << 2
        )
                | ecn;
    }

    private static boolean isVoicePort(
            int port
    ) {
        return port == 5060
                || port == 5061
                || (
                port >= 16384
                        && port <= 32767
        );
    }

    private static boolean isVideoPort(
            int port
    ) {
        return port == 554
                || port == 1935;
    }

    private static boolean isBackgroundPort(
            int port
    ) {
        return port == 20
                || port == 21
                || port == 25;
    }
}
