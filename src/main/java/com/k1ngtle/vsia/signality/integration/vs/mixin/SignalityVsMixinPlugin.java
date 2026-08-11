package com.k1ngtle.vsia.signality.integration.vs.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SignalityVsMixinPlugin implements IMixinConfigPlugin {
   private static final String VS_MOD_ID = "valkyrienskies";
   private boolean vsLoaded;

   public void onLoad(String mixinPackage) {
      this.vsLoaded = isModPresentEarly("valkyrienskies");
   }

   public List<String> getMixins() {
      return !this.vsLoaded ? Collections.emptyList() : List.of("ShipObjectServerMixin");
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      return this.vsLoaded;
   }

   public String getRefMapperConfig() {
      return null;
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public void preApply(String t, ClassNode tc, String m, IMixinInfo i) {
   }

   public void postApply(String t, ClassNode tc, String m, IMixinInfo i) {
   }

   private static boolean isModPresentEarly(String modId) {
      try {
         LoadingModList modList = LoadingModList.get();
         return modList != null && modList.getModFileById(modId) != null;
      } catch (Throwable var2) {
         return false;
      }
   }
}
