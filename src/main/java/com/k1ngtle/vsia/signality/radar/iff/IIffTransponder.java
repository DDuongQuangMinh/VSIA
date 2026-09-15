package com.k1ngtle.vsia.signality.radar.iff;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface IIffTransponder {
    UUID iffTargetId();
    ServerLevel iffLevel();
    Vec3 iffPositionWorld();
    IffTransponderState iffState();
    String iffCallsign();
    int iffSquawkCode();
    int iffModeSAddress();
    byte[] respondToChallenge(long challengeNonce);
}
