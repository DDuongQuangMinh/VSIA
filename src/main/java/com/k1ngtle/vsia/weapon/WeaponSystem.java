package com.k1ngtle.vsia.weapon;

import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Call WeaponSystem.init(modEventBus) once from your main @Mod
 * constructor, alongside your other registries. Everything else
 * (server tick handling, client keybindings) wires itself up via
 * the @Mod.EventBusSubscriber annotations on the individual classes,
 * so nothing else needs to be called manually.
 */
public final class WeaponSystem {

    private WeaponSystem() {}

    public static void init(IEventBus modEventBus) {
        WeaponItems.register(modEventBus);
        WeaponNetwork.register();
    }
}