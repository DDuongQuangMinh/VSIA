package com.k1ngtle.vsia.signality.api.signal;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface ISignalTransmitter {
   UUID id();

   ServerLevel level();

   Vec3 positionWorld();

   SignalBand band();
}
