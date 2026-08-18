package com.k1ngtle.vsia.signality;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.api.occlusion.WorldOcclusionProvider;
import com.k1ngtle.vsia.signality.api.radar.RadarRegistry;
import com.k1ngtle.vsia.signality.core.scan.RadarScanScheduler;
import com.k1ngtle.vsia.signality.debug.DebugVisualization;
import com.k1ngtle.vsia.signality.debug.RadarBeaconBlockEntity;
import com.k1ngtle.vsia.signality.debug.SignalityCommand;
import com.k1ngtle.vsia.signality.debug.SignalityTestCommand;
import com.k1ngtle.vsia.signality.engineering.channel.RfDiscreteEventScheduler;
import com.k1ngtle.vsia.signality.engineering.channel.RfTransmissionRegistry;
import com.k1ngtle.vsia.signality.engineering.wifi.WifiMacTimingScheduler;
import com.k1ngtle.vsia.signality.debug.SignalityLabCommand;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolProgramReloadListener;
import com.k1ngtle.vsia.signality.engineering.vm.ProtocolVmScheduler;
import com.k1ngtle.vsia.signality.integration.dh.DhCompat;
import com.k1ngtle.vsia.signality.integration.vs.VsCompat;
import com.k1ngtle.vsia.signality.internet.network.NetworkProfileReloadListener;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

public final class Signality {

   public static final String MODID =
           Vsia.MOD_ID;

   public static final Logger LOGGER =
           LogUtils.getLogger();

   private Signality(
           FMLJavaModLoadingContext context
   ) {
      IEventBus modBus =
              context.getModEventBus();

      SignalityBlocks.register(
              modBus
      );

      modBus.addListener(
              this::commonSetup
      );

      ModLoadingContext
              .get()
              .registerConfig(
                      Type.COMMON,
                      SignalityConfig.SPEC,
                      "signality-common.toml"
              );

      MinecraftForge.EVENT_BUS.register(
              this
      );
   }

   public static void initialize(
           FMLJavaModLoadingContext context
   ) {
      new Signality(
              context
      );
   }

   private void commonSetup(
           FMLCommonSetupEvent event
   ) {
      event.enqueueWork(() -> {
         RadarRegistry.addTargetSource(
                 RadarBeaconBlockEntity::beaconsIn
         );

         if (VsCompat.isLoaded()
                 && (Boolean) SignalityConfig
                 .ENABLE_VS_INTEGRATION
                 .get()) {

            RadarRegistry.addTargetSource(
                    VsCompat.hook()::shipTargetsIn
            );

            LOGGER.info(
                    "Registered VS target source with RadarRegistry."
            );
         }

         if (DhCompat.isLoaded()
                 && (Boolean) SignalityConfig
                 .ENABLE_DH_OCCLUSION
                 .get()) {

            RadarRegistry.addOcclusionProvider(
                    DhCompat.provider()
            );

            LOGGER.info(
                    "Registered DH height-occlusion provider with RadarRegistry."
            );
         }
      });
   }

   @SubscribeEvent
   public void onAddReloadListeners(
           AddReloadListenerEvent event
   ) {
      event.addListener(
              new NetworkProfileReloadListener()
      );

      event.addListener(
              new ProtocolProgramReloadListener()
      );
   }

   @SubscribeEvent
   public void onServerStarting(
           ServerStartingEvent event
   ) {
      RadarRegistry.addOcclusionProvider(
              WorldOcclusionProvider.INSTANCE
      );

      RadarScanScheduler.start(
              (Integer) SignalityConfig
                      .WORKER_COUNT
                      .get()
      );
   }

   @SubscribeEvent
   public void onServerStopped(
           ServerStoppedEvent event
   ) {
      RadarScanScheduler.stop();
      ProtocolVmScheduler.clear();
      RfDiscreteEventScheduler.clear();
      RfTransmissionRegistry.clear();
      WifiMacTimingScheduler.clear();
   }

   @SubscribeEvent
   public void onRegisterCommands(
           RegisterCommandsEvent event
   ) {
      SignalityCommand.register(
              event.getDispatcher()
      );

      SignalityTestCommand.register(
              event.getDispatcher()
      );

      SignalityLabCommand.register(
              event.getDispatcher()
      );
   }

   @SubscribeEvent
   public void onServerTick(
           ServerTickEvent event
   ) {
      if (event.phase == Phase.END) {
         try {
            ProtocolVmScheduler.tickAll();
         } catch (Throwable throwable) {
            LOGGER.warn(
                    "Signality protocol VM scheduler tick failed.",
                    throwable
            );
         }

         for (ServerLevel level
                 : event
                 .getServer()
                 .getAllLevels()) {

            try {
               RfDiscreteEventScheduler.tick(
                       level
               );

               WifiMacTimingScheduler.tick(
                       level.getGameTime()
               );

               RadarScanScheduler.onServerTick(
                       level
               );

               DebugVisualization.onServerTick(
                       level
               );
            } catch (Throwable throwable) {
               LOGGER.warn(
                       "RadarScanScheduler tick failed on level {}",
                       level.dimension(),
                       throwable
               );
            }
         }
      }
   }
}
