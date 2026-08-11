package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.valkyrienskies.core.api.ships.ServerShip;

public final class VsShipTargetView implements IRadarTarget {
   private static final double SHIP_HULL_RCS_GAIN = 2.0;
   private final ServerLevel level;
   private final ServerShip ship;
   private final VsHookImpl hook;
   private final UUID id;

   public VsShipTargetView(ServerLevel level, ServerShip ship, VsHookImpl hook) {
      this.level = level;
      this.ship = ship;
      this.hook = hook;
      this.id = new UUID(5864145284199612416L, ship.getId());
   }

   @Override
   public UUID id() {
      return this.id;
   }

   @Override
   public ServerLevel level() {
      return this.level;
   }

   @Override
   public Vec3 positionWorld() {
      return this.hook.shipObb(this.ship).centerWorld();
   }

   @Override
   public Vec3 velocityWorld() {
      return this.hook.shipVelocity(this.ship);
   }

   @Override
   public double boundingRadius() {
      return this.hook.shipObb(this.ship).enclosingRadius();
   }

   @Override
   public double radarCrossSection(double aspectAngleRad, double wavelengthMeters) {
      double r = this.boundingRadius();
      double baseRcs = Math.PI * r * r;
      double aspectMod = 0.3 + 0.7 * Math.abs(Math.sin(aspectAngleRad));
      return baseRcs * aspectMod * 2.0;
   }

   @Override
   public Object vsShip() {
      return this.ship;
   }
}
