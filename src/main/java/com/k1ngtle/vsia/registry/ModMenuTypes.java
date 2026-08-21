package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import com.k1ngtle.vsia.world.inventory.RtAc68uRouterMenu;
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

    public static final RegistryObject<MenuType<com.k1ngtle.vsia.world.inventory.NetworkSwitchMenu>> NETWORK_SWITCH_MENU =
            MENUS.register("network_switch_menu", () -> IForgeMenuType.create(com.k1ngtle.vsia.world.inventory.NetworkSwitchMenu::new));

    public static final RegistryObject<MenuType<com.k1ngtle.vsia.world.inventory.FirewallMenu>> FIREWALL_MENU =
            MENUS.register("firewall_menu", () -> IForgeMenuType.create(com.k1ngtle.vsia.world.inventory.FirewallMenu::new));

    public static final RegistryObject<MenuType<RtAc68uRouterMenu>> RT_AC68U_ROUTER_MENU =
            MENUS.register("rt_ac68u_router_menu", () -> IForgeMenuType.create(RtAc68uRouterMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}