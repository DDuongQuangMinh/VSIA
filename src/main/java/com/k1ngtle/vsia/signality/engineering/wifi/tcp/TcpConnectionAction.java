package com.k1ngtle.vsia.signality.engineering.wifi.tcp;

import java.util.List;

public record TcpConnectionAction(
        List<TcpSegment> outbound,
        boolean retransmitEarliest,
        boolean resetConnection,
        String event
) {
    public TcpConnectionAction {
        outbound =
                outbound == null
                        ? List.of()
                        : List.copyOf(
                                outbound
                        );

        event =
                event == null
                        ? ""
                        : event;
    }

    public static TcpConnectionAction none(
            String event
    ) {
        return new TcpConnectionAction(
                List.of(),
                false,
                false,
                event
        );
    }
}
