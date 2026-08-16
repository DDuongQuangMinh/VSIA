package com.k1ngtle.vsia.signality;

import com.k1ngtle.vsia.Vsia;
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
import com.k1ngtle.vsia.signality.internet.server.NetworkCableItem;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlock;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchItem;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlock;
import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.FirewallItem;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlock;
import com.k1ngtle.vsia.signality.internet.server.ServerRackBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.ServerRackItem;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlock;
import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.StorageServerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SignalityBlocks {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Vsia.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Vsia.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Vsia.MOD_ID);

    // --- Core Network Devices ---

    public static final RegistryObject<Block> SERVER_RACK = BLOCKS.register(
            "server_rack",
            () -> new ServerRackBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 8.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> SERVER_RACK_ITEM = ITEMS.register(
            "server_rack",
            () -> new ServerRackItem(
                    SERVER_RACK.get(),
                    new Item.Properties()
            )
    );

    public static final RegistryObject<BlockEntityType<ServerRackBlockEntity>> SERVER_RACK_BE = BLOCK_ENTITIES.register(
            "server_rack",
            () -> BlockEntityType.Builder.of(
                    ServerRackBlockEntity::new,
                    SERVER_RACK.get()
            ).build(null)
    );

    public static final RegistryObject<Block> STORAGE_SERVER = BLOCKS.register(
            "storage_server",
            () -> new StorageServerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 8.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> STORAGE_SERVER_ITEM = ITEMS.register(
            "storage_server",
            () -> new StorageServerItem(
                    STORAGE_SERVER.get(),
                    new Item.Properties()
            )
    );

    public static final RegistryObject<BlockEntityType<StorageServerBlockEntity>> STORAGE_SERVER_BE = BLOCK_ENTITIES.register(
            "storage_server",
            () -> BlockEntityType.Builder.of(
                    StorageServerBlockEntity::new,
                    STORAGE_SERVER.get()
            ).build(null)
    );

    public static final RegistryObject<Block> NETWORK_SWITCH = BLOCKS.register(
            "network_switch",
            () -> new NetworkSwitchBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> NETWORK_SWITCH_ITEM = ITEMS.register(
            "network_switch",
            () -> new NetworkSwitchItem(
                    NETWORK_SWITCH.get(),
                    new Item.Properties()
            )
    );

    public static final RegistryObject<BlockEntityType<NetworkSwitchBlockEntity>> NETWORK_SWITCH_BE = BLOCK_ENTITIES.register(
            "network_switch",
            () -> BlockEntityType.Builder.of(
                    NetworkSwitchBlockEntity::new,
                    NETWORK_SWITCH.get()
            ).build(null)
    );

    public static final RegistryObject<Block> FIREWALL = BLOCKS.register(
            "firewall",
            () -> new FirewallBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(3.0F, 8.0F)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Item> FIREWALL_ITEM = ITEMS.register(
            "firewall",
            () -> new FirewallItem(
                    FIREWALL.get(),
                    new Item.Properties()
            )
    );

    public static final RegistryObject<BlockEntityType<FirewallBlockEntity>> FIREWALL_BE = BLOCK_ENTITIES.register(
            "firewall",
            () -> BlockEntityType.Builder.of(
                    FirewallBlockEntity::new,
                    FIREWALL.get()
            ).build(null)
    );

    // --- Utilities & Cables ---

    public static final RegistryObject<Item> NETWORK_CABLE = ITEMS.register(
            "network_cable",
            () -> new NetworkCableItem(new Item.Properties())
    );

    // --- Signality Example & Debug Blocks ---

    public static final RegistryObject<Block> RADAR_BEACON = BLOCKS.register(
            "radar_beacon",
            () -> new RadarBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<RadarBeaconBlockEntity>> RADAR_BEACON_BE = BLOCK_ENTITIES.register(
            "radar_beacon",
            () -> BlockEntityType.Builder.of(
                    RadarBeaconBlockEntity::new,
                    RADAR_BEACON.get()
            ).build(null)
    );

    public static final RegistryObject<Block> SIGNAL_TEST_EMITTER = BLOCKS.register(
            "signal_test_emitter",
            () -> new SignalTestEmitterBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<SignalTestEmitterBlockEntity>> SIGNAL_TEST_EMITTER_BE = BLOCK_ENTITIES.register(
            "signal_test_emitter",
            () -> BlockEntityType.Builder.of(
                    SignalTestEmitterBlockEntity::new,
                    SIGNAL_TEST_EMITTER.get()
            ).build(null)
    );

    public static final RegistryObject<Block> SIGNAL_TEST_RECEIVER = BLOCKS.register(
            "signal_test_receiver",
            () -> new SignalTestReceiverBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<SignalTestReceiverBlockEntity>> SIGNAL_TEST_RECEIVER_BE = BLOCK_ENTITIES.register(
            "signal_test_receiver",
            () -> BlockEntityType.Builder.of(
                    SignalTestReceiverBlockEntity::new,
                    SIGNAL_TEST_RECEIVER.get()
            ).build(null)
    );

    public static final RegistryObject<Block> PULSE_DOPPLER_RADAR = BLOCKS.register(
            "pulse_doppler_radar",
            () -> new PulseDopplerRadarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<PulseDopplerRadarBlockEntity>> PULSE_DOPPLER_RADAR_BE = BLOCK_ENTITIES.register(
            "pulse_doppler_radar",
            () -> BlockEntityType.Builder.of(
                    PulseDopplerRadarBlockEntity::new,
                    PULSE_DOPPLER_RADAR.get()
            ).build(null)
    );

    public static final RegistryObject<Block> SCANNING_RADAR = BLOCKS.register(
            "scanning_radar",
            () -> new ScanningRadarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<ScanningRadarBlockEntity>> SCANNING_RADAR_BE = BLOCK_ENTITIES.register(
            "scanning_radar",
            () -> BlockEntityType.Builder.of(
                    ScanningRadarBlockEntity::new,
                    SCANNING_RADAR.get()
            ).build(null)
    );

    public static final RegistryObject<Block> SEARCH_RADAR = BLOCKS.register(
            "search_radar",
            () -> new SearchRadarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<SearchRadarBlockEntity>> SEARCH_RADAR_BE = BLOCK_ENTITIES.register(
            "search_radar",
            () -> BlockEntityType.Builder.of(
                    SearchRadarBlockEntity::new,
                    SEARCH_RADAR.get()
            ).build(null)
    );

    public static final RegistryObject<Block> TRACKING_RADAR = BLOCKS.register(
            "tracking_radar",
            () -> new TrackingRadarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5F))
    );

    public static final RegistryObject<BlockEntityType<TrackingRadarBlockEntity>> TRACKING_RADAR_BE = BLOCK_ENTITIES.register(
            "tracking_radar",
            () -> BlockEntityType.Builder.of(
                    TrackingRadarBlockEntity::new,
                    TRACKING_RADAR.get()
            ).build(null)
    );

    private SignalityBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}