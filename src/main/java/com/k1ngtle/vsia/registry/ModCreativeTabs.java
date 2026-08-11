package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Vsia.MOD_ID);

    public static final RegistryObject<CreativeModeTab> VSIA_TAB = CREATIVE_MODE_TABS.register("vsia_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.BASE_HELMET.get()))
                    .title(Component.translatable("creativetab.vsia_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.BASE_HELMET.get());
                        pOutput.accept(ModItems.GHILLIE_HELMET.get());
                        pOutput.accept(ModItems.SAND_HELMET.get());
                        pOutput.accept(ModItems.SNOW_HELMET.get());
                        pOutput.accept(ModItems.PVS31_HELMET.get());
                        pOutput.accept(ModItems.PVS31_GHILLIE_HELMET.get());
                        pOutput.accept(ModItems.PVS31_SAND_HELMET.get());
                        pOutput.accept(ModItems.PVS31_SNOW_HELMET.get());
                        pOutput.accept(ModItems.GPNVG18_HELMET.get());
                        pOutput.accept(ModItems.GPNVG18_GHILLIE_HELMET.get());
                        pOutput.accept(ModItems.GPNVG18_SAND_HELMET.get());
                        pOutput.accept(ModItems.GPNVG18_SNOW_HELMET.get());
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> VSIA_WEAPON_TAB = CREATIVE_MODE_TABS.register("vsia_weapon_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(WeaponItems.AK74SU_VSOP.get()))
                    .title(Component.translatable("creativetab.vsia_weapon_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(WeaponItems.AK74SU_VSOP.get());
                        pOutput.accept(WeaponItems.AMMO_556.get());
                        pOutput.accept(WeaponItems.AMMO_545.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}