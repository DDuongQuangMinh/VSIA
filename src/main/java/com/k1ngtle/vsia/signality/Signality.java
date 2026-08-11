package com.k1ngtle.vsia.signality;

import com.mojang.logging.LogUtils;
import com.k1ngtle.vsia.signality.api.occlusion.WorldOcclusionProvider;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.core.scan.RadarScanScheduler;
import com.k1ngtle.vsia.signality.debug.DebugVisualization;
import com.k1ngtle.vsia.signality.debug.RadarBeaconBlockEntity;
import com.k1ngtle.vsia.signality.debug.SignalityCommand;
import com.k1ngtle.vsia.signality.integration.dh.DhCompat;
import com.k1ngtle.vsia.signality.integration.vs.VsCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.k1ngtle.vsia.Vsia;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import net.minecraftforge.fml.ModLoadingContext;

public final class Signality {
   public static final String MODID = Vsia.MOD_ID;
   public static final Logger LOGGER = LogUtils.getLogger();

   private Signality(FMLJavaModLoadingContext context) {
      IEventBus modBus = context.getModEventBus();
      SignalityBlocks.register(modBus);
      modBus.addListener(this::commonSetup);
      ModLoadingContext.get().registerConfig(
              Type.COMMON,
              SignalityConfig.SPEC,
              "signality-common.toml"
      );
      MinecraftForge.EVENT_BUS.register(this);
   }

   public static void initialize(FMLJavaModLoadingContext context) {
      new Signality(context);
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> {
         RadarRegistry.addTargetSource(RadarBeaconBlockEntity::beaconsIn);
         if (VsCompat.isLoaded() && (Boolean)SignalityConfig.ENABLE_VS_INTEGRATION.get()) {
            RadarRegistry.addTargetSource(VsCompat.hook()::shipTargetsIn);
            LOGGER.info("Registered VS target source with RadarRegistry.");
         }

         if (DhCompat.isLoaded() && (Boolean)SignalityConfig.ENABLE_DH_OCCLUSION.get()) {
            RadarRegistry.addOcclusionProvider(DhCompat.provider());
            LOGGER.info("Registered DH height-occlusion provider with RadarRegistry.");
         }
      });
   }

   @SubscribeEvent
   public void onServerStarting(ServerStartingEvent event) {
      RadarRegistry.addOcclusionProvider(WorldOcclusionProvider.INSTANCE);
      RadarScanScheduler.start((Integer)SignalityConfig.WORKER_COUNT.get());
   }

   @SubscribeEvent
   public void onServerStopped(ServerStoppedEvent event) {
      RadarScanScheduler.stop();
   }

   @SubscribeEvent
   public void onRegisterCommands(RegisterCommandsEvent event) {
      SignalityCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END) {
         for (ServerLevel level : event.getServer().getAllLevels()) {
            try {
               RadarScanScheduler.onServerTick(level);
               DebugVisualization.onServerTick(level);
            } catch (Throwable var5) {
               LOGGER.warn("RadarScanScheduler tick failed on level {}", level.dimension(), var5);
            }
         }
      }
   }
}
