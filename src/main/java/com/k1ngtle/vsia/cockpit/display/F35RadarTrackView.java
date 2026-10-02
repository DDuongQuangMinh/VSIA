package com.k1ngtle.vsia.cockpit.display;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public record F35RadarTrackView(
        UUID trackId,
        String trackState,
        Vec3 position,
        Vec3 velocity,
        double quality,
        double uncertaintyMeters,
        double bestSnrLinear,
        int sensorCount,
        int hits,
        String iffAffiliation,
        String iffReplyStatus,
        String iffCallsign,
        int iffSquawk,
        boolean iffAuthenticated,
        String iffTelemetry
) {
    public F35RadarTrackView(UUID trackId,String trackState,Vec3 position,Vec3 velocity,double quality,double uncertaintyMeters,double bestSnrLinear,int sensorCount,int hits,String iffAffiliation,String iffReplyStatus,String iffCallsign,int iffSquawk,boolean iffAuthenticated){this(trackId,trackState,position,velocity,quality,uncertaintyMeters,bestSnrLinear,sensorCount,hits,iffAffiliation,iffReplyStatus,iffCallsign,iffSquawk,iffAuthenticated,"");}
    public double speedMps() {
        return velocity.length();
    }

    public String shortId() {
        String raw =
                trackId.toString()
                        .replace(
                                "-",
                                ""
                        );

        return raw.substring(
                0,
                Math.min(
                        4,
                        raw.length()
                )
        )
                .toUpperCase();
    }
}
