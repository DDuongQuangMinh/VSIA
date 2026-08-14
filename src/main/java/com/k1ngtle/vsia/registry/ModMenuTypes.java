package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    // Registering against your main mod ID
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Vsia.MOD_ID);

    // Creates the forge menu type for our custom storage server GUI
    public static final RegistryObject<MenuType<StorageServerMenu>> STORAGE_SERVER_MENU =
            MENUS.register("storage_server_menu", () -> IForgeMenuType.create(StorageServerMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}