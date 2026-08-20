package com.k1ngtle.vsia.signality.engineering.wifi.ip.traceroute;

import java.util.ArrayList;
import java.util.List;

public final class WifiTracerouteTestSuite {
    private WifiTracerouteTestSuite() {
    }

    public static List<WifiTracerouteTestResult> runAll() {
        List<WifiTracerouteTestResult> out =
                new ArrayList<>();

        WifiTracerouteSession session =
                new WifiTracerouteSession(
                        1001L,
                        "192.168.2.20",
                        8
                );

        WifiTracerouteSession.ProbeRequest p1 =
                session.beginProbe(
                        1_000_000L
                );

        out.add(
                result(
                        "wifi-w1108-first-probe",
                        p1.ttl() == 1
                                && p1.attempt() == 1,
                        "First traceroute probe must start at TTL 1"
                )
        );

        out.add(
                result(
                        "wifi-w1108-quote-match",
                        session.matchesQuotedProbe(
                                "192.168.1.100",
                                "192.168.2.20",
                                1
                        ),
                        "ICMP quote must match active destination/protocol"
                )
        );

        session.onTimeExceeded(
                "192.168.1.1",
                1_012_500L
        );

        out.add(
                result(
                        "wifi-w1108-hop1",
                        session.snapshot().hops().size() == 1
                                && session.snapshot().hops().get(0).ttl() == 1
                                && "192.168.1.1".equals(
                                session.snapshot().hops().get(0).responderIp()
                        )
                                && Math.abs(
                                session.snapshot().hops().get(0).rttMs()
                                        - 12.5
                        ) < 0.0001,
                        "Hop 1 should record responder and RTT"
                )
        );

        WifiTracerouteSession.ProbeRequest p2 =
                session.beginProbe(
                        2_000_000L
                );

        out.add(
                result(
                        "wifi-w1108-ttl-increment",
                        p2.ttl() == 2,
                        "TTL must increment after Time Exceeded"
                )
        );

        session.onTimeExceeded(
                "10.0.0.2",
                2_027_000L
        );

        WifiTracerouteSession.ProbeRequest p3 =
                session.beginProbe(
                        3_000_000L
                );

        out.add(
                result(
                        "wifi-w1108-echo-correlation",
                        session.matchesEchoReply(
                                1001L,
                                3,
                                1,
                                "192.168.2.20"
                        )
                                && !session.matchesEchoReply(
                                1001L,
                                2,
                                1,
                                "192.168.2.20"
                        )
                                && !session.matchesEchoReply(
                                9999L,
                                3,
                                1,
                                "192.168.2.20"
                        ),
                        "Echo Reply must match trace-id, TTL, attempt and destination"
                )
        );

        session.onEchoReply(
                "192.168.2.20",
                3_041_250L
        );

        out.add(
                result(
                        "wifi-w1108-destination",
                        p3.ttl() == 3
                                && session.snapshot().destinationReached()
                                && !session.snapshot().running()
                                && session.snapshot().hops().size() == 3,
                        "TTL 3 echo reply must terminate trace"
                )
        );

        WifiTracerouteSession retry =
                new WifiTracerouteSession(
                        1002L,
                        "203.0.113.9",
                        4
                );

        retry.beginProbe(
                1L
        );

        out.add(
                result(
                        "wifi-w1108-timeout-retry1",
                        retry.onTimeout()
                                == WifiTracerouteSession.TimeoutAction.RETRY,
                        "First timeout must retry same TTL"
                )
        );

        WifiTracerouteSession.ProbeRequest retry2 =
                retry.beginProbe(
                        2L
                );

        retry.onTimeout();

        WifiTracerouteSession.ProbeRequest retry3 =
                retry.beginProbe(
                        3L
                );

        out.add(
                result(
                        "wifi-w1108-retry-same-ttl",
                        retry2.ttl() == 1
                                && retry2.attempt() == 2
                                && retry3.ttl() == 1
                                && retry3.attempt() == 3,
                        "Retries must preserve TTL and increment attempt"
                )
        );

        out.add(
                result(
                        "wifi-w1108-timeout-hop",
                        retry.onTimeout()
                                == WifiTracerouteSession.TimeoutAction.NEXT_HOP
                                && retry.snapshot().hops().size() == 1
                                && "*".equals(
                                retry.snapshot().hops().get(0).responderIp()
                        )
                                && retry.snapshot().currentTtl() == 2,
                        "Three timeouts must emit * and advance TTL"
                )
        );

        WifiTracerouteSession max =
                new WifiTracerouteSession(
                        1003L,
                        "198.51.100.5",
                        1
                );

        max.beginProbe(1L);
        max.onTimeout();
        max.beginProbe(2L);
        max.onTimeout();
        max.beginProbe(3L);

        out.add(
                result(
                        "wifi-w1108-max-hop",
                        max.onTimeout()
                                == WifiTracerouteSession.TimeoutAction.FINISHED
                                && !max.snapshot().running()
                                && "MAX_HOPS_REACHED".equals(
                                max.snapshot().finalStatus()
                        ),
                        "Final hop timeout must stop at max-hop limit"
                )
        );

        WifiTracerouteSession unreachable =
                new WifiTracerouteSession(
                        1004L,
                        "198.51.100.77",
                        30
                );

        unreachable.beginProbe(
                10L
        );

        unreachable.onDestinationUnreachable(
                "10.0.0.254",
                0,
                15_000L
        );

        out.add(
                result(
                        "wifi-w1108-unreachable",
                        !unreachable.snapshot().running()
                                && "DESTINATION_UNREACHABLE".equals(
                                unreachable.snapshot().finalStatus()
                        ),
                        "Destination Unreachable must terminate trace"
                )
        );

        WifiTracerouteSession mismatch =
                new WifiTracerouteSession(
                        1005L,
                        "192.168.2.20",
                        5
                );

        mismatch.beginProbe(
                100L
        );

        out.add(
                result(
                        "wifi-w1108-quote-reject",
                        !mismatch.matchesQuotedProbe(
                                "192.168.1.100",
                                "192.168.99.99",
                                1
                        )
                                && !mismatch.matchesQuotedProbe(
                                "192.168.1.100",
                                "192.168.2.20",
                                17
                        ),
                        "Wrong quoted destination/protocol must be rejected"
                )
        );

        return List.copyOf(
                out
        );
    }

    private static WifiTracerouteTestResult result(
            String name,
            boolean passed,
            String detail
    ) {
        return new WifiTracerouteTestResult(
                name,
                passed,
                detail
        );
    }
}
