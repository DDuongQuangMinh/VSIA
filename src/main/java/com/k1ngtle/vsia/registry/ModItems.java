package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetItem;
import com.k1ngtle.vsia.item.GhillieHelmetItem;
import com.k1ngtle.vsia.item.SandHelmetItem;
import com.k1ngtle.vsia.item.SnowHelmetItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Vsia.MOD_ID);

    // Base Helmet
    public static final RegistryObject<Item> BASE_HELMET = ITEMS.register("base_helmet",
            () -> new HelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    // Ghillie Helmet
    public static final RegistryObject<Item> GHILLIE_HELMET = ITEMS.register("ghillie_helmet",
            () -> new GhillieHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    // Sand Helmet
    public static final RegistryObject<Item> SAND_HELMET = ITEMS.register("sand_helmet",
            () -> new SandHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    // Snow Helmet
    public static final RegistryObject<Item> SNOW_HELMET = ITEMS.register("snow_helmet",
            () -> new SnowHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}