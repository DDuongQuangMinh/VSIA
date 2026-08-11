package com.k1ngtle.vsia.signality.integration.vs;

import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.api.geom.Obb;
import com.k1ngtle.vsia.signality.api.radar.IRadarTarget;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

public final class VsCompat {
   public static final String VS_MOD_ID = "valkyrienskies";
   private static final boolean LOADED;
   private static final VsCompat.VsHook HOOK;

   private VsCompat() {
   }

   public static boolean isLoaded() {
      return LOADED;
   }

   public static VsCompat.VsHook hook() {
      return HOOK;
   }

   private static VsCompat.VsHook loadHookImpl() {
      try {
         Class<?> implCls = Class.forName("com.k1ngtle.vsia.signality.integration.vs.VsHookImpl");
         return (VsCompat.VsHook)implCls.getDeclaredConstructor().newInstance();
      } catch (Throwable var1) {
         Signality.LOGGER.error("VS detected but VsHookImpl failed to load â€” disabling integration.", var1);
         return VsCompat.VsHook.NOOP;
      }
   }

   static {
      boolean loaded = false;

      try {
         loaded = ModList.get() != null && ModList.get().isLoaded("valkyrienskies");
      } catch (Throwable var2) {
      }

      LOADED = loaded;
      HOOK = LOADED ? loadHookImpl() : VsCompat.VsHook.NOOP;
      if (LOADED) {
         Signality.LOGGER.info("Valkyrien Skies detected â€” ship integration active.");
      } else {
         Signality.LOGGER.info("Valkyrien Skies not present â€” running standalone radar core.");
      }
   }

   public interface VsHook {
      VsCompat.VsHook NOOP = new VsCompat.VsHook() {
         @Override
         public Object shipManagingPos(ServerLevel level, BlockPos pos) {
            return null;
         }

         @Override
         public Vec3 transformShipToWorld(Object ship, Vec3 shipPos) {
            return shipPos;
         }

         @Override
         public Vec3 transformDirShipToWorld(Object ship, Vec3 shipDir) {
            return shipDir;
         }

         @Override
         public Vec3 transformWorldToShip(Object ship, Vec3 worldPos) {
            return worldPos;
         }

         @Override
         public Vec3 transformDirWorldToShip(Object ship, Vec3 worldDir) {
            return worldDir;
         }

         @Override
         public Vec3 shipCenter(Object ship) {
            return Vec3.ZERO;
         }

         @Override
         public Vec3 shipVelocity(Object ship) {
            return Vec3.ZERO;
         }

         @Override
         public Obb shipObb(Object ship) {
            return new Obb(Vec3.ZERO, Vec3.ZERO, new Quaterniond());
         }

         @Override
         public Stream<IRadarTarget> shipTargetsIn(ServerLevel level) {
            return Stream.empty();
         }
      };

      @Nullable
      Object shipManagingPos(ServerLevel var1, BlockPos var2);

      Vec3 transformShipToWorld(Object var1, Vec3 var2);

      Vec3 transformDirShipToWorld(Object var1, Vec3 var2);

      Vec3 transformWorldToShip(Object var1, Vec3 var2);

      Vec3 transformDirWorldToShip(Object var1, Vec3 var2);

      Vec3 shipCenter(Object var1);

      Vec3 shipVelocity(Object var1);

      Obb shipObb(Object var1);

      Stream<IRadarTarget> shipTargetsIn(ServerLevel var1);
   }
}
