package com.k1ngtle.vsia.signality.debug;

import com.k1ngtle.vsia.signality.api.events.RadarScanEvent;
import com.k1ngtle.vsia.signality.api.radar.IRadarEmitter;
import com.k1ngtle.vsia.signality.api.radar.RadarContact;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.Vsia;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.joml.Vector3f;

@EventBusSubscriber(
   modid = Vsia.MOD_ID
)
public final class DebugVisualization {
   private static final int TICK_INTERVAL = 10;
   private static final double AXIS_RANGE_M = 96.0;
   private static final int AXIS_SAMPLES = 24;
   private static final int RIM_RINGS = 4;
   private static final int RIM_PER_RING = 8;
   private static final double VIEW_RANGE_M = 128.0;
   private static final DustParticleOptions RIM_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.2F), 1.0F);
   private static final DustParticleOptions CONTACT_DUST = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.0F), 1.4F);
   private static final Set<UUID> VISUALIZED = ConcurrentHashMap.newKeySet();
   private static final Map<UUID, List<RadarContact>> LAST_CONTACTS = new ConcurrentHashMap<>();
   private static long lastDraw = 0L;

   private DebugVisualization() {
   }

   @SubscribeEvent
   public static void onScanPost(RadarScanEvent.Post event) {
      LAST_CONTACTS.put(event.emitter().id(), event.contacts());
   }

   public static boolean toggle(UUID emitterId) {
      if (VISUALIZED.remove(emitterId)) {
         return false;
      } else {
         VISUALIZED.add(emitterId);
         return true;
      }
   }

   public static boolean isVisualized(UUID emitterId) {
      return VISUALIZED.contains(emitterId);
   }

   public static void onServerTick(ServerLevel level) {
      long t = level.getGameTime();
      if (t - lastDraw >= 10L) {
         lastDraw = t;

         for (IRadarEmitter emitter : RadarRegistry.emittersIn(level)) {
            if (VISUALIZED.contains(emitter.id())) {
               drawCone(level, emitter);
               drawContacts(level, emitter);
            }
         }
      }
   }

   private static void drawCone(ServerLevel level, IRadarEmitter emitter) {
      Vec3 apex = emitter.originWorld();
      Vec3 axis = emitter.axisWorld().normalize();
      double halfAngle = emitter.profile().halfBeamWidthRad() * emitter.mode().beamWidthMultiplier();
      double range = Math.min(96.0, emitter.profile().maxRangeMeters());

      for (int i = 1; i <= 24; i++) {
         double t = (double)i / 24.0;
         Vec3 p = apex.add(axis.scale(range * t));
         level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
      }

      Vec3[] basis = orthonormalBasis(axis);
      Vec3 u = basis[0];
      Vec3 v = basis[1];
      double tanHalf = Math.tan(halfAngle);

      for (int r = 1; r <= 4; r++) {
         double along = range * (double)r / 4.0;
         double ringRadius = along * tanHalf;
         Vec3 ringCenter = apex.add(axis.scale(along));

         for (int k = 0; k < 8; k++) {
            double angle = (Math.PI * 2) * (double)k / 8.0;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vec3 offset = u.scale(cos * ringRadius).add(v.scale(sin * ringRadius));
            Vec3 p = ringCenter.add(offset);
            level.sendParticles(RIM_DUST, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
         }
      }
   }

   private static void drawContacts(ServerLevel level, IRadarEmitter emitter) {
      List<RadarContact> contacts = LAST_CONTACTS.get(emitter.id());
      if (contacts != null && !contacts.isEmpty()) {
         for (RadarContact c : contacts) {
            Vec3 p = c.positionWorld();
            level.sendParticles(CONTACT_DUST, p.x, p.y + 1.0, p.z, 4, 0.3, 0.3, 0.3, 0.0);
         }
      }
   }

   private static Vec3[] orthonormalBasis(Vec3 axis) {
      Vec3 ref = Math.abs(axis.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 u = axis.cross(ref).normalize();
      Vec3 v = axis.cross(u).normalize();
      return new Vec3[]{u, v};
   }
}
