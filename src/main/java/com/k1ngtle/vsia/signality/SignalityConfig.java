package com.k1ngtle.vsia.signality;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public final class SignalityConfig {
   public static final ForgeConfigSpec SPEC;
   public static final IntValue WORKER_COUNT;
   public static final BooleanValue ENABLE_VS_INTEGRATION;
   public static final BooleanValue ENABLE_DH_OCCLUSION;
   public static final IntValue CONTACT_DELIVERY_BUDGET;

   private SignalityConfig() {
   }

   static {
      Builder b = new Builder();
      b.push("performance");
      int defaultWorkers = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
      WORKER_COUNT = b.comment(new String[]{"Number of off-thread radar worker threads.", "Default = max(1, availableProcessors / 2)."})
         .defineInRange("workerCount", defaultWorkers, 1, 64);
      CONTACT_DELIVERY_BUDGET = b.comment(
            new String[]{"Maximum completed scans drained from the worker pool per server tick.", "Reduce if radar-heavy worlds spike the server tick."}
         )
         .defineInRange("contactDeliveryBudget", 32, 1, 2048);
      b.pop().push("integration");
      ENABLE_VS_INTEGRATION = b.comment("Register the Valkyrien Skies target source. Only takes effect when VS is installed.")
         .define("enableValkyrienSkies", true);
      ENABLE_DH_OCCLUSION = b.comment("Register the Distant Horizons height-occlusion provider. Only takes effect when DH is installed.")
         .define("enableDistantHorizons", true);
      b.pop();
      SPEC = b.build();
   }
}
