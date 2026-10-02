package com.k1ngtle.vsia.signality.radar.iff;

public record IffResult(
        IffAffiliation affiliation,
        IffReplyStatus replyStatus,
        String callsign,
        int squawkCode,
        int modeSAddress,
        boolean authenticated,
        double roundTripTimeMicros,
        String authenticatedTelemetry
) {
    public IffResult { authenticatedTelemetry=authenticatedTelemetry==null?"":authenticatedTelemetry.substring(0,Math.min(256,authenticatedTelemetry.length())); }
    public IffResult(IffAffiliation affiliation,IffReplyStatus replyStatus,String callsign,int squawkCode,int modeSAddress,boolean authenticated,double roundTripTimeMicros){this(affiliation,replyStatus,callsign,squawkCode,modeSAddress,authenticated,roundTripTimeMicros,"");}
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
