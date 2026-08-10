package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetItem;
import com.k1ngtle.vsia.item.GhillieHelmetItem;
import com.k1ngtle.vsia.item.SandHelmetItem;
import com.k1ngtle.vsia.item.SnowHelmetItem;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Vsia.MOD_ID);

    public static final RegistryObject<Item> BASE_HELMET = ITEMS.register("base_helmet",
            () -> new HelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> GHILLIE_HELMET = ITEMS.register("ghillie_helmet",
            () -> new GhillieHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> SAND_HELMET = ITEMS.register("sand_helmet",
            () -> new SandHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> SNOW_HELMET = ITEMS.register("snow_helmet",
            () -> new SnowHelmetItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> PVS31_HELMET = ITEMS.register("helmet_pvs31",
            () -> new HelmetPVS31Item(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> PVS31_GHILLIE_HELMET = ITEMS.register("helmet_pvs31_ghillie",
            () -> new GhillieHelmetPVS31Item(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> PVS31_SAND_HELMET = ITEMS.register("helmet_pvs31_sand",
            () -> new SandHelmetPVS31Item(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static final RegistryObject<Item> PVS31_SNOW_HELMET = ITEMS.register("helmet_pvs31_snow",
            () -> new SnowHelmetPVS31Item(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}