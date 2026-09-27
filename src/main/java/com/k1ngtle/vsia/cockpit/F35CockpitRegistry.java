package com.k1ngtle.vsia.cockpit;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class F35CockpitRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    ForgeRegistries.BLOCKS,
                    Vsia.MOD_ID
            );

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(
                    ForgeRegistries.ITEMS,
                    Vsia.MOD_ID
            );

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    Vsia.MOD_ID
            );

    public static final RegistryObject<Block> F35_COCKPIT_SEAT =
            BLOCKS.register(
                    "f35_cockpit_seat",
                    () ->
                            new F35CockpitSeatBlock(
                                    BlockBehaviour.Properties
                                            .of()
                                            .strength(
                                                    3.0F
                                            )
                                            .sound(
                                                    SoundType.METAL
                                            )
                                            .noOcclusion()
                            )
            );

    public static final RegistryObject<Item> F35_COCKPIT_SEAT_ITEM =
            ITEMS.register(
                    "f35_cockpit_seat",
                    () ->
                            new BlockItem(
                                    F35_COCKPIT_SEAT.get(),
                                    new Item.Properties()
                            )
            );

    public static final RegistryObject<BlockEntityType<F35CockpitSeatBlockEntity>>
            F35_COCKPIT_SEAT_BE =
            BLOCK_ENTITIES.register(
                    "f35_cockpit_seat",
                    () ->
                            BlockEntityType.Builder
                                    .of(
                                            F35CockpitSeatBlockEntity::new,
                                            F35_COCKPIT_SEAT.get()
                                    )
                                    .build(
                                            null
                                    )
            );

    private F35CockpitRegistry() {
    }

    public static void register(
            IEventBus bus
    ) {
        BLOCKS.register(
                bus
        );

        ITEMS.register(
                bus
        );

        BLOCK_ENTITIES.register(
                bus
        );
    }
}
