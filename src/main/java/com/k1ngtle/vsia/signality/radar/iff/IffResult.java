package com.k1ngtle.vsia.signality.radar.iff;

public record IffResult(
        IffAffiliation affiliation,
        IffReplyStatus replyStatus,
        String callsign,
        int squawkCode,
        int modeSAddress,
        boolean authenticated,
        double roundTripTimeMicros
) {
    public static IffResult unknown(IffReplyStatus status) {
        return new IffResult(
                IffAffiliation.UNKNOWN,
                status,
                "",
                0,
                0,
                false,
                0.0
        );
    }

    public static IffResult noTransponder() {
        return unknown(IffReplyStatus.NO_TRANSPONDER);
    }
}
