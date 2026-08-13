package com.k1ngtle.vsia.signality;

import com.k1ngtle.vsia.signality.debug.RadarBeaconBlock;
import com.k1ngtle.vsia.signality.debug.RadarBeaconBlockEntity;
import com.k1ngtle.vsia.signality.debug.SignalTestEmitterBlock;
import com.k1ngtle.vsia.signality.debug.SignalTestEmitterBlockEntity;
import com.k1ngtle.vsia.signality.debug.SignalTestReceiverBlock;
import com.k1ngtle.vsia.signality.debug.SignalTestReceiverBlockEntity;
import com.k1ngtle.vsia.signality.example.PulseDopplerRadarBlock;
import com.k1ngtle.vsia.signality.example.PulseDopplerRadarBlockEntity;
import com.k1ngtle.vsia.signality.example.ScanningRadarBlock;
import com.k1ngtle.vsia.signality.example.ScanningRadarBlockEntity;
import com.k1ngtle.vsia.signality.example.SearchRadarBlock;
import com.k1ngtle.vsia.signality.example.SearchRadarBlockEntity;
import com.k1ngtle.vsia.signality.example.TrackingRadarBlock;
import com.k1ngtle.vsia.signality.example.TrackingRadarBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlock;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SignalityBlocks {
   public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Signality.MODID);
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Signality.MODID);
   public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Signality.MODID);
   public static final RegistryObject<Block> PULSE_DOPPLER_RADAR = BLOCKS.register(
           "pulse_doppler_radar", () -> new PulseDopplerRadarBlock(Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).noOcclusion())
   );
   public static final RegistryObject<Item> PULSE_DOPPLER_RADAR_ITEM = ITEMS.register(
           "pulse_doppler_radar", () -> new BlockItem((Block)PULSE_DOPPLER_RADAR.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<PulseDopplerRadarBlockEntity>> PULSE_DOPPLER_RADAR_BE = BLOCK_ENTITIES.register(
           "pulse_doppler_radar", () -> Builder.of(PulseDopplerRadarBlockEntity::new, new Block[]{(Block)PULSE_DOPPLER_RADAR.get()}).build(null)
   );
   public static final RegistryObject<Block> SEARCH_RADAR = BLOCKS.register(
           "search_radar", () -> new SearchRadarBlock(Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).noOcclusion())
   );
   public static final RegistryObject<Item> SEARCH_RADAR_ITEM = ITEMS.register(
           "search_radar", () -> new BlockItem((Block)SEARCH_RADAR.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<SearchRadarBlockEntity>> SEARCH_RADAR_BE = BLOCK_ENTITIES.register(
           "search_radar", () -> Builder.of(SearchRadarBlockEntity::new, new Block[]{(Block)SEARCH_RADAR.get()}).build(null)
   );
   public static final RegistryObject<Block> SCANNING_RADAR = BLOCKS.register(
           "scanning_radar", () -> new ScanningRadarBlock(Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).noOcclusion())
   );
   public static final RegistryObject<Item> SCANNING_RADAR_ITEM = ITEMS.register(
           "scanning_radar", () -> new BlockItem((Block)SCANNING_RADAR.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<ScanningRadarBlockEntity>> SCANNING_RADAR_BE = BLOCK_ENTITIES.register(
           "scanning_radar", () -> Builder.of(ScanningRadarBlockEntity::new, new Block[]{(Block)SCANNING_RADAR.get()}).build(null)
   );
   public static final RegistryObject<Block> TRACKING_RADAR = BLOCKS.register(
           "tracking_radar", () -> new TrackingRadarBlock(Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).noOcclusion())
   );
   public static final RegistryObject<Item> TRACKING_RADAR_ITEM = ITEMS.register(
           "tracking_radar", () -> new BlockItem((Block)TRACKING_RADAR.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<TrackingRadarBlockEntity>> TRACKING_RADAR_BE = BLOCK_ENTITIES.register(
           "tracking_radar", () -> Builder.of(TrackingRadarBlockEntity::new, new Block[]{(Block)TRACKING_RADAR.get()}).build(null)
   );
   public static final RegistryObject<Block> RADAR_BEACON = BLOCKS.register(
           "radar_beacon", () -> new RadarBeaconBlock(Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F).lightLevel(s -> 5).noOcclusion())
   );
   public static final RegistryObject<Item> RADAR_BEACON_ITEM = ITEMS.register(
           "radar_beacon", () -> new BlockItem((Block)RADAR_BEACON.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<RadarBeaconBlockEntity>> RADAR_BEACON_BE = BLOCK_ENTITIES.register(
           "radar_beacon", () -> Builder.of(RadarBeaconBlockEntity::new, new Block[]{(Block)RADAR_BEACON.get()}).build(null)
   );
   public static final RegistryObject<Block> SIGNAL_TEST_EMITTER = BLOCKS.register(
           "signal_test_emitter", () -> new SignalTestEmitterBlock(Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.0F).lightLevel(s -> 7).noOcclusion())
   );
   public static final RegistryObject<Item> SIGNAL_TEST_EMITTER_ITEM = ITEMS.register(
           "signal_test_emitter", () -> new BlockItem((Block)SIGNAL_TEST_EMITTER.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<SignalTestEmitterBlockEntity>> SIGNAL_TEST_EMITTER_BE = BLOCK_ENTITIES.register(
           "signal_test_emitter", () -> Builder.of(SignalTestEmitterBlockEntity::new, new Block[]{(Block)SIGNAL_TEST_EMITTER.get()}).build(null)
   );
   public static final RegistryObject<Block> SIGNAL_TEST_RECEIVER = BLOCKS.register(
           "signal_test_receiver",
           () -> new SignalTestReceiverBlock(Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.0F).lightLevel(s -> 7).noOcclusion())
   );
   public static final RegistryObject<Item> SIGNAL_TEST_RECEIVER_ITEM = ITEMS.register(
           "signal_test_receiver", () -> new BlockItem((Block)SIGNAL_TEST_RECEIVER.get(), new net.minecraft.world.item.Item.Properties())
   );
   public static final RegistryObject<BlockEntityType<SignalTestReceiverBlockEntity>> SIGNAL_TEST_RECEIVER_BE = BLOCK_ENTITIES.register(
           "signal_test_receiver", () -> Builder.of(SignalTestReceiverBlockEntity::new, new Block[]{(Block)SIGNAL_TEST_RECEIVER.get()}).build(null)
   );

   public static final RegistryObject<Block> SERVER_RACK = BLOCKS.register(
           "server_rack",
           () -> new ServerRackBlock(
                   Properties.of()
                           .mapColor(MapColor.METAL)
                           .strength(3.0F, 8.0F)
                           .noOcclusion()
           )
   );

   public static final RegistryObject<Item> SERVER_RACK_ITEM =
           ITEMS.register(
                   "server_rack",
                   () -> new com.k1ngtle.vsia.signality.internet.server.ServerRackItem(
                           SERVER_RACK.get(),
                           new Item.Properties()
                   )
           );

   public static final RegistryObject<BlockEntityType<ServerRackBlockEntity>>
           SERVER_RACK_BE = BLOCK_ENTITIES.register(
           "server_rack",
           () -> Builder.of(
                   ServerRackBlockEntity::new,
                   SERVER_RACK.get()
           ).build(null)
   );

   private SignalityBlocks() {
   }

   public static void register(IEventBus modBus) {
      BLOCKS.register(modBus);
      ITEMS.register(modBus);
      BLOCK_ENTITIES.register(modBus);
   }
}
