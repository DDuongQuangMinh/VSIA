package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.weapon.item.ModernGunItem;
import com.k1ngtle.vsia.weapon.state.FireMode;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public static final RegistryObject<CreativeModeTab> VSIA_WEAPON_TAB =
            CREATIVE_MODE_TABS.register("vsia_weapon_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(ModItems.GENERIC_GUN.get()))
                            .title(Component.translatable("creativetab.vsia_weapon_tab"))
                            .displayItems((parameters, output) -> {
                                ItemStack m4a1 =
                                        new ItemStack(ModItems.GENERIC_GUN.get());

                                if (m4a1.getItem() instanceof ModernGunItem gun) {
                                    gun.setWeaponId(
                                            m4a1,
                                            new ResourceLocation(Vsia.MOD_ID, "m4a1")
                                    );

                                    WeaponRuntimeState state =
                                            gun.getRuntimeState(m4a1);

                                    state.setMagazineAmmo(29);
                                    state.setChamberLoaded(true);
                                    state.setFireMode(FireMode.SEMI);
                                    state.setBoltLocked(false);
                                }

                                output.accept(m4a1);
                                ItemStack ak74m = new ItemStack(ModItems.GENERIC_GUN.get());
                                if (ak74m.getItem() instanceof ModernGunItem gun) {
                                    gun.setWeaponId(ak74m, new ResourceLocation(Vsia.MOD_ID, "ak74m"));
                                    WeaponRuntimeState state = gun.getRuntimeState(ak74m);
                                    state.setMagazineAmmo(29);
                                    state.setChamberLoaded(true);
                                    state.setFireMode(FireMode.SEMI);
                                    state.setBoltLocked(false);
                                }
                                output.accept(ak74m);
                                output.accept(ModItems.AMMO_545.get());
                                output.accept(ModItems.AMMO_556.get());
                            })
                            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}