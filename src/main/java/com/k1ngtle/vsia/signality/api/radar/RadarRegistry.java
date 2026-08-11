package com.k1ngtle.vsia.signality.api.radar;

import com.k1ngtle.vsia.signality.api.occlusion.CompositeOcclusionProvider;
import com.k1ngtle.vsia.signality.api.occlusion.IOcclusionProvider;
import com.k1ngtle.vsia.signality.api.rcs.DefaultRcsProviders;
import com.k1ngtle.vsia.signality.api.rcs.IRcsProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;

public final class RadarRegistry {
   private static final ConcurrentHashMap<UUID, IRadarEmitter> EMITTERS = new ConcurrentHashMap<>();
   private static final CopyOnWriteArrayList<RadarRegistry.TargetSource> TARGET_SOURCES = new CopyOnWriteArrayList<>();
   private static final CopyOnWriteArrayList<IRcsProvider> RCS_PROVIDERS = new CopyOnWriteArrayList<>();
   private static final CompositeOcclusionProvider OCCLUSION = new CompositeOcclusionProvider();

   private RadarRegistry() {
   }

   public static void registerEmitter(IRadarEmitter emitter) {
      EMITTERS.put(emitter.id(), emitter);
   }

   public static void unregisterEmitter(UUID id) {
      EMITTERS.remove(id);
   }

   public static List<IRadarEmitter> emittersIn(ServerLevel level) {
      List<IRadarEmitter> out = new ArrayList<>();

      for (IRadarEmitter e : EMITTERS.values()) {
         if (e.level() == level) {
            out.add(e);
         }
      }

      return out;
   }

   public static void addTargetSource(RadarRegistry.TargetSource source) {
      TARGET_SOURCES.add(source);
   }

   public static List<RadarRegistry.TargetSource> targetSources() {
      return Collections.unmodifiableList(TARGET_SOURCES);
   }

   public static void addRcsProvider(IRcsProvider provider) {
      RCS_PROVIDERS.add(0, provider);
   }

   public static double resolveRcs(IRadarTarget target, IRadarEmitter emitter, double aspectRad) {
      for (IRcsProvider p : RCS_PROVIDERS) {
         double v = p.computeRcs(target, emitter, aspectRad);
         if (!Double.isNaN(v) && Double.isFinite(v) && v >= 0.0) {
            return v;
         }
      }

      return 1.0E-6;
   }

   public static CompositeOcclusionProvider occlusionProviders() {
      return OCCLUSION;
   }

   public static void addOcclusionProvider(IOcclusionProvider provider) {
      OCCLUSION.add(provider);
   }

   static {
      RCS_PROVIDERS.add(DefaultRcsProviders.TARGET_DELEGATE);
      RCS_PROVIDERS.add(DefaultRcsProviders.HEURISTIC);
   }

   @FunctionalInterface
   public interface TargetSource extends Function<ServerLevel, Stream<IRadarTarget>> {
   }
}
