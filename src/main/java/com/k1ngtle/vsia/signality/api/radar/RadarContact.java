package com.k1ngtle.vsia.signality.api.radar;

import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record RadarContact(
   UUID emitterId,
   UUID targetId,
   Vec3 positionWorld,
   Vec3 velocityWorld,
   double rangeMeters,
   double bearingRad,
   double elevationRad,
   double closureRateMps,
   double signalToNoiseRatio,
   double signalToClutterRatio,
   boolean trackQuality,
   @Nullable Object vsShipHandle
) {
}
