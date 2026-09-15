package com.k1ngtle.vsia.signality.radar.iff;

import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkService;
import java.util.UUID;

public final class IffInterrogator {
    private static final double C = 299_792_458.0;
    private static final double TRANSPONDER_TURNAROUND_MICROS = 3.0;

    private IffInterrogator() {
    }

    public static IffResult interrogate(
            IRadarEmitter emitter,
            RadarContact contact,
            long gameTime
    ) {
        IIffTransponder transponder = IffRegistry.find(contact.targetId());
        if (transponder == null) {
            return IffResult.noTransponder();
        }
        if (transponder.iffLevel() != emitter.level()) {
            return IffResult.unknown(IffReplyStatus.NO_REPLY);
        }
        if (!transponder.iffState().repliesToInterrogation()) {
            return IffResult.unknown(IffReplyStatus.NO_REPLY);
        }

        double rangeMeters = Math.max(0.0, contact.rangeMeters());
        double interrogatorLimit = Math.min(
                emitter.profile().maxRangeMeters(),
                200_000.0
        );
        if (rangeMeters > interrogatorLimit) {
            return IffResult.unknown(IffReplyStatus.NO_REPLY);
        }

        long nonce = challengeNonce(
                emitter.id(),
                contact.targetId(),
                gameTime
        );
        byte[] reply = transponder.respondToChallenge(nonce);

        String networkId = RadarNetworkService.networkFor(emitter.id());
        String interrogatorKey = IffNetworkKeyRegistry.keyFor(networkId);
        byte[] expected = IffCrypto.response(
                interrogatorKey,
                nonce,
                transponder.iffModeSAddress(),
                transponder.iffSquawkCode()
        );

        boolean authenticated = IffCrypto.constantTimeEquals(reply, expected);
        double roundTripMicros =
                (rangeMeters * 2.0 / C) * 1_000_000.0
                        + TRANSPONDER_TURNAROUND_MICROS;

        if (!authenticated) {
            return new IffResult(
                    IffAffiliation.UNKNOWN,
                    IffReplyStatus.AUTH_FAILED,
                    transponder.iffCallsign(),
                    transponder.iffSquawkCode(),
                    transponder.iffModeSAddress(),
                    false,
                    roundTripMicros
            );
        }

        boolean emergency =
                transponder.iffState() == IffTransponderState.EMERGENCY
                        || isEmergencySquawk(transponder.iffSquawkCode());

        return new IffResult(
                emergency
                        ? IffAffiliation.FRIENDLY_EMERGENCY
                        : IffAffiliation.FRIENDLY,
                IffReplyStatus.AUTHENTICATED,
                transponder.iffCallsign(),
                transponder.iffSquawkCode(),
                transponder.iffModeSAddress(),
                true,
                roundTripMicros
        );
    }

    public static boolean isEmergencySquawk(int code) {
        return code == 7500 || code == 7600 || code == 7700;
    }

    private static long challengeNonce(
            UUID emitterId,
            UUID targetId,
            long tick
    ) {
        long value = emitterId.getMostSignificantBits()
                ^ emitterId.getLeastSignificantBits();
        value = mix64(value ^ targetId.getMostSignificantBits());
        value = mix64(value ^ targetId.getLeastSignificantBits());
        return mix64(value ^ tick ^ 0x4946465F56534941L);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}
