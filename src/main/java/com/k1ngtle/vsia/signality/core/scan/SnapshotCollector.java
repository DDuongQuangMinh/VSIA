package com.k1ngtle.vsia.signality.core.scan;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarProfile;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.api.rcs.DefaultRcsProviders;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SnapshotCollector {
   private static final IRadarEmitter NULL_EMITTER_FOR_HEURISTIC = new IRadarEmitter() {
      private final UUID nullId = new UUID(0L, 0L);
      private final RadarProfile placeholder = RadarProfile.builder().build();

      @Override
      public UUID id() {
         return this.nullId;
      }

      @Override
      public ServerLevel level() {
         throw new UnsupportedOperationException();
      }

      @Override
      public Vec3 originWorld() {
         return Vec3.ZERO;
      }

      @Override
      public Vec3 axisWorld() {
         return new Vec3(1.0, 0.0, 0.0);
      }

      @Override
      public RadarProfile profile() {
         return this.placeholder;
      }

      @Override
      public void onContacts(List<RadarContact> contacts) {
      }
   };

   private SnapshotCollector() {
   }

   public static RadarSnapshot collect(ServerLevel level) {
      List<IRadarEmitter> emitters = RadarRegistry.emittersIn(level);
      List<RadarSnapshot.EmitterSnap> emitterSnaps = new ArrayList<>(emitters.size());
      double maxRange = 0.0;

      for (IRadarEmitter e : emitters) {
         if (e.shouldScan()) {
            emitterSnaps.add(snapEmitter(e));
            if (e.profile().maxRangeMeters() > maxRange) {
               maxRange = e.profile().maxRangeMeters();
            }
         }
      }

      if (emitterSnaps.isEmpty()) {
         return new RadarSnapshot(level, level.getGameTime(), List.of(), List.of());
      } else {
         Set<UUID> seen = new HashSet<>();
         List<RadarSnapshot.TargetSnap> targetSnaps = new ArrayList<>();
         AABB envelope = envelopeFor(emitterSnaps, maxRange);

         for (Entity entity : level.getEntities().getAll()) {
            if (entity.isAlive() && !entity.isRemoved() && envelope.intersects(entity.getBoundingBox())) {
               UUID id = entity.getUUID();
               if (seen.add(id)) {
                  targetSnaps.add(snapEntity(entity));
               }
            }
         }

         for (RadarRegistry.TargetSource src : RadarRegistry.targetSources()) {
            try (Stream<IRadarTarget> stream = src.apply(level)) {
               if (stream != null) {
                  stream.forEach(t -> {
                     if (t != null && t.detectable()) {
                        if (seen.add(t.id())) {
                           targetSnaps.add(snapTarget(t));
                        }
                     }
                  });
               }
            } catch (Throwable var15) {
               Signality.LOGGER.warn("Target source {} threw during snapshot collection", src, var15);
            }
         }

         return new RadarSnapshot(level, level.getGameTime(), emitterSnaps, targetSnaps);
      }
   }

   private static AABB envelopeFor(List<RadarSnapshot.EmitterSnap> emitters, double maxRange) {
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double minZ = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      for (RadarSnapshot.EmitterSnap e : emitters) {
         double r = e.profile().maxRangeMeters();
         Vec3 o = e.originWorld();
         if (o.x - r < minX) {
            minX = o.x - r;
         }

         if (o.y - r < minY) {
            minY = o.y - r;
         }

         if (o.z - r < minZ) {
            minZ = o.z - r;
         }

         if (o.x + r > maxX) {
            maxX = o.x + r;
         }

         if (o.y + r > maxY) {
            maxY = o.y + r;
         }

         if (o.z + r > maxZ) {
            maxZ = o.z + r;
         }
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   private static RadarSnapshot.EmitterSnap snapEmitter(IRadarEmitter e) {
      return new RadarSnapshot.EmitterSnap(e.id(), e.level(), e.originWorld(), e.axisWorld(), e.velocityWorld(), e.profile(), e.mode(), e.vsShip(), e);
   }

   private static RadarSnapshot.TargetSnap snapEntity(Entity entity) {
      AABB box = entity.getBoundingBox();
      double radius = 0.5 * Math.sqrt(square(box.getXsize()) + square(box.getYsize()) + square(box.getZsize()));
      Vec3 vel = entity.getDeltaMovement().scale(20.0);
      return new RadarSnapshot.TargetSnap(entity.getUUID(), entity.position(), vel, radius, null, null, new SnapshotCollector.EntityTargetView(entity, radius));
   }

   private static RadarSnapshot.TargetSnap snapTarget(IRadarTarget t) {
      return new RadarSnapshot.TargetSnap(t.id(), t.positionWorld(), t.velocityWorld(), t.boundingRadius(), null, t.vsShip(), t);
   }

   private static double square(double v) {
      return v * v;
   }

   private static final class EntityTargetView implements IRadarTarget {
      private final Entity entity;
      private final double radius;
      private final UUID id;

      EntityTargetView(Entity entity, double radius) {
         this.entity = entity;
         this.radius = radius;
         this.id = entity.getUUID();
      }

      @Override
      public UUID id() {
         return this.id;
      }

      @Override
      public ServerLevel level() {
         return (ServerLevel)this.entity.level();
      }

      @Override
      public Vec3 positionWorld() {
         return this.entity.position();
      }

      @Override
      public Vec3 velocityWorld() {
         return this.entity.getDeltaMovement().scale(20.0);
      }

      @Override
      public double boundingRadius() {
         return this.radius;
      }

      @Override
      public double radarCrossSection(double aspectRad, double waveM) {
         return DefaultRcsProviders.HEURISTIC.computeRcs(this, SnapshotCollector.NULL_EMITTER_FOR_HEURISTIC, aspectRad);
      }

      @Override
      public boolean detectable() {
         return this.entity.isAlive() && !this.entity.isInvisible();
      }
   }
}
