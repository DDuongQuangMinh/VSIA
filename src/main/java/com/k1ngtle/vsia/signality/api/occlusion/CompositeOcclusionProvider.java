package com.k1ngtle.vsia.signality.api.occlusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class CompositeOcclusionProvider implements IOcclusionProvider {
   private final List<IOcclusionProvider> providers = new ArrayList<>();

   public synchronized void add(IOcclusionProvider provider) {
      if (provider == null) {
         throw new NullPointerException("provider");
      } else {
         this.providers.add(provider);
      }
   }

   public synchronized void remove(IOcclusionProvider provider) {
      this.providers.remove(provider);
   }

   public synchronized List<IOcclusionProvider> snapshot() {
      return Collections.unmodifiableList(new ArrayList<>(this.providers));
   }

   @Override
   public boolean isOccluded(ServerLevel level, Vec3 from, Vec3 to) {
      for (IOcclusionProvider p : this.snapshot()) {
         if (p.isOccluded(level, from, to)) {
            return true;
         }
      }

      return false;
   }

   public boolean isOccludedThreadSafe(ServerLevel level, Vec3 from, Vec3 to) {
      for (IOcclusionProvider p : this.snapshot()) {
         if (p.threadSafe() && p.isOccluded(level, from, to)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean threadSafe() {
      return false;
   }
}
