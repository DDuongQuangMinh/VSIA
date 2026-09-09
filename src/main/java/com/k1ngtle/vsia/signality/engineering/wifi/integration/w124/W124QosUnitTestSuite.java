package com.k1ngtle.vsia.signality.engineering.wifi.integration.w124;

import com.k1ngtle.vsia.signality.engineering.wifi.EdcaController;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiAccessCategory;
import com.k1ngtle.vsia.signality.engineering.wifi.qos.WifiQosClassifier;
import com.k1ngtle.vsia.signality.internet.OSINetworkPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class W124QosUnitTestSuite {
    public record Result(
            String name,
            boolean passed,
            String detail
    ) {
    }

    private W124QosUnitTestSuite() {
    }

    public static List<Result> runAll() {
        List<Result> results =
                new ArrayList<>();

        results.add(
                wmmEdcaProfile()
        );

        results.add(
                userPriorityMapping()
        );

        results.add(
                dscpMapping()
        );

        results.add(
                portFallback()
        );

        results.add(
                bidirectionalPortFallback()
        );

        results.add(
                dscpOverridesPortFallback()
        );

        results.add(
                packetMarkingPersistence()
        );

        results.add(
                edcaPriorityOrdering()
        );

        return List.copyOf(
                results
        );
    }

    private static Result wmmEdcaProfile() {
        boolean passed =
                WifiAccessCategory.VOICE.aifsn() == 2
                        && WifiAccessCategory.VOICE.cwMin() == 3
                        && WifiAccessCategory.VOICE.cwMax() == 7
                        && WifiAccessCategory.VOICE.txopMicroseconds() == 1504
                        && WifiAccessCategory.VIDEO.aifsn() == 2
                        && WifiAccessCategory.VIDEO.cwMin() == 7
                        && WifiAccessCategory.VIDEO.cwMax() == 15
                        && WifiAccessCategory.VIDEO.txopMicroseconds() == 3008
                        && WifiAccessCategory.BEST_EFFORT.aifsn() == 3
                        && WifiAccessCategory.BEST_EFFORT.cwMin() == 15
                        && WifiAccessCategory.BEST_EFFORT.cwMax() == 1023
                        && WifiAccessCategory.BACKGROUND.aifsn() == 7
                        && WifiAccessCategory.BACKGROUND.cwMin() == 15
                        && WifiAccessCategory.BACKGROUND.cwMax() == 1023;

        return new Result(
                "w124-wmm-edca-profile",
                passed,
                passed
                        ? "VOICE/VIDEO/BEST_EFFORT/BACKGROUND expose differentiated AIFS, CW and TXOP policy"
                        : "WMM/EDCA access-category parameters do not match the W1.24 profile"
        );
    }

    private static Result userPriorityMapping() {
        boolean passed =
                WifiQosClassifier.fromUserPriority(
                        6
                ) == WifiAccessCategory.VOICE
                        && WifiQosClassifier.fromUserPriority(
                        5
                ) == WifiAccessCategory.VIDEO
                        && WifiQosClassifier.fromUserPriority(
                        0
                ) == WifiAccessCategory.BEST_EFFORT
                        && WifiQosClassifier.fromUserPriority(
                        1
                ) == WifiAccessCategory.BACKGROUND
                        && WifiQosClassifier.userPriority(
                        WifiAccessCategory.VOICE
                ) == 6
                        && WifiQosClassifier.userPriority(
                        WifiAccessCategory.VIDEO
                ) == 5
                        && WifiQosClassifier.userPriority(
                        WifiAccessCategory.BEST_EFFORT
                ) == 0
                        && WifiQosClassifier.userPriority(
                        WifiAccessCategory.BACKGROUND
                ) == 1;

        return new Result(
                "w124-wmm-user-priority",
                passed,
                passed
                        ? "802.1D/WMM UP values map to the four Wi-Fi access categories"
                        : "WMM user-priority mapping failed"
        );
    }

    private static Result dscpMapping() {
        boolean passed =
                WifiQosClassifier.fromDscp(
                        46
                ) == WifiAccessCategory.VOICE
                        && WifiQosClassifier.fromDscp(
                        34
                ) == WifiAccessCategory.VIDEO
                        && WifiQosClassifier.fromDscp(
                        0
                ) == WifiAccessCategory.BEST_EFFORT
                        && WifiQosClassifier.fromDscp(
                        8
                ) == WifiAccessCategory.BACKGROUND;

        return new Result(
                "w124-dscp-to-wmm",
                passed,
                passed
                        ? "EF/AF41/Default/CS1 map to VO/VI/BE/BK"
                        : "DSCP to WMM access-category mapping failed"
        );
    }

    private static Result portFallback() {
        OSINetworkPacket voice =
                new OSINetworkPacket();

        voice.targetPort =
                5060;

        OSINetworkPacket video =
                new OSINetworkPacket();

        video.targetPort =
                554;

        OSINetworkPacket background =
                new OSINetworkPacket();

        background.targetPort =
                21;

        OSINetworkPacket bestEffort =
                new OSINetworkPacket();

        bestEffort.targetPort =
                80;

        boolean passed =
                WifiQosClassifier.classify(
                        voice
                ) == WifiAccessCategory.VOICE
                        && WifiQosClassifier.classify(
                        video
                ) == WifiAccessCategory.VIDEO
                        && WifiQosClassifier.classify(
                        background
                ) == WifiAccessCategory.BACKGROUND
                        && WifiQosClassifier.classify(
                        bestEffort
                ) == WifiAccessCategory.BEST_EFFORT;

        return new Result(
                "w124-port-fallback",
                passed,
                passed
                        ? "Unmarked packets retain application/port fallback classification"
                        : "Port fallback classification failed"
        );
    }

    private static Result bidirectionalPortFallback() {
        OSINetworkPacket sipResponse =
                new OSINetworkPacket();

        sipResponse.sourcePort =
                5060;

        sipResponse.targetPort =
                49152;

        OSINetworkPacket videoResponse =
                new OSINetworkPacket();

        videoResponse.sourcePort =
                554;

        videoResponse.targetPort =
                49153;

        boolean passed =
                WifiQosClassifier.classify(
                        sipResponse
                ) == WifiAccessCategory.VOICE
                        && WifiQosClassifier.classify(
                        videoResponse
                ) == WifiAccessCategory.VIDEO;

        return new Result(
                "w124-bidirectional-classification",
                passed,
                passed
                        ? "Service-port classification works for requests and responses"
                        : "Response traffic lost its service class"
        );
    }

    private static Result dscpOverridesPortFallback() {
        OSINetworkPacket packet =
                new OSINetworkPacket();

        packet.dscp =
                8;

        packet.targetPort =
                5060;

        boolean passed =
                WifiQosClassifier.classify(
                        packet
                ) == WifiAccessCategory.BACKGROUND;

        return new Result(
                "w124-explicit-marking-precedence",
                passed,
                passed
                        ? "Explicit DSCP marking takes precedence over heuristic port classification"
                        : "Port heuristic incorrectly overrode explicit DSCP marking"
        );
    }

    private static Result packetMarkingPersistence() {
        OSINetworkPacket original =
                new OSINetworkPacket();

        original.sourceMac =
                "02:00:00:00:00:01";

        original.targetMac =
                "02:00:00:00:00:02";

        original.sourceIp =
                "192.168.1.10";

        original.targetIp =
                "192.168.1.20";

        original.dscp =
                46;

        original.ecn =
                2;

        original.sourcePort =
                5060;

        original.targetPort =
                5060;

        OSINetworkPacket decoded =
                OSINetworkPacket.deserializeNBT(
                        original.serializeNBT()
                );

        boolean passed =
                decoded.dscp == 46
                        && decoded.ecn == 2
                        && decoded.dscpEcnByte()
                        == WifiQosClassifier.dscpEcnByte(
                        46,
                        2
                )
                        && WifiQosClassifier.classify(
                        decoded
                ) == WifiAccessCategory.VOICE;

        return new Result(
                "w124-qos-marking-persistence",
                passed,
                passed
                        ? "DSCP/ECN survives VSIA logical-packet serialization"
                        : "DSCP/ECN marking was not preserved"
        );
    }

    private static Result edcaPriorityOrdering() {
        int samples =
                20000;

        double voice =
                averageLogicalSlots(
                        WifiAccessCategory.VOICE,
                        samples,
                        12401L
                );

        double video =
                averageLogicalSlots(
                        WifiAccessCategory.VIDEO,
                        samples,
                        12402L
                );

        double bestEffort =
                averageLogicalSlots(
                        WifiAccessCategory.BEST_EFFORT,
                        samples,
                        12403L
                );

        double background =
                averageLogicalSlots(
                        WifiAccessCategory.BACKGROUND,
                        samples,
                        12404L
                );

        boolean passed =
                voice < video
                        && video < bestEffort
                        && bestEffort < background;

        return new Result(
                "w124-edca-priority-ordering",
                passed,
                String.format(
                        java.util.Locale.ROOT,
                        "Average logical contention slots VO=%.3f VI=%.3f BE=%.3f BK=%.3f",
                        voice,
                        video,
                        bestEffort,
                        background
                )
        );
    }

    private static double averageLogicalSlots(
            WifiAccessCategory category,
            int samples,
            long seed
    ) {
        EdcaController controller =
                new EdcaController(
                        new Random(
                                seed
                        )
                );

        long total =
                0L;

        for (int index = 0;
             index < samples;
             index++) {
            total +=
                    controller.acquireLogicalMedium(
                            category
                    );

            controller.onSuccess(
                    category
            );
        }

        return total
                / (double) samples;
    }
}
