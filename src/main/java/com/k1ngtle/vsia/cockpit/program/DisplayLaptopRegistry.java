package com.k1ngtle.vsia.cockpit.program;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The existing display_laptop item becomes a BlockItem; never register a second item ID. */
public final class DisplayLaptopRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Vsia.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Vsia.MOD_ID);
    public static final RegistryObject<Block> LAPTOP = BLOCKS.register("display_laptop", () ->
            new DisplayLaptopBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<BlockEntityType<DisplayLaptopBlockEntity>> LAPTOP_ENTITY = ENTITIES.register(
            "display_laptop", () -> BlockEntityType.Builder.of(DisplayLaptopBlockEntity::new, LAPTOP.get()).build(null));

    private DisplayLaptopRegistry() { }
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); }
}
