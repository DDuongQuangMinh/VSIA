package com.k1ngtle.vsia.registry;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab>
            CREATIVE_MODE_TABS =
            DeferredRegister.create(
                    Registries.CREATIVE_MODE_TAB,
                    Vsia.MOD_ID
            );

    public static final RegistryObject<CreativeModeTab> VSIA_TAB =
            CREATIVE_MODE_TABS.register(
                    "vsia_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(
                                    ModItems.BASE_HELMET.get()
                            ))
                            .title(Component.translatable(
                                    "creativetab.vsia_tab"
                            ))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.BASE_HELMET.get());
                                output.accept(ModItems.GHILLIE_HELMET.get());
                                output.accept(ModItems.SAND_HELMET.get());
                                output.accept(ModItems.SNOW_HELMET.get());

                                output.accept(ModItems.PVS31_HELMET.get());
                                output.accept(
                                        ModItems.PVS31_GHILLIE_HELMET.get()
                                );
                                output.accept(
                                        ModItems.PVS31_SAND_HELMET.get()
                                );
                                output.accept(
                                        ModItems.PVS31_SNOW_HELMET.get()
                                );

                                output.accept(ModItems.GPNVG18_HELMET.get());
                                output.accept(
                                        ModItems.GPNVG18_GHILLIE_HELMET.get()
                                );
                                output.accept(
                                        ModItems.GPNVG18_SAND_HELMET.get()
                                );
                                output.accept(
                                        ModItems.GPNVG18_SNOW_HELMET.get()
                                );
                            })
                            .build()
            );

    public static final RegistryObject<CreativeModeTab> VSIA_WEAPON_TAB =
            CREATIVE_MODE_TABS.register(
                    "vsia_weapon_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(
                                    ModItems.BASE_HELMET.get()
                            ))
                            .title(Component.translatable(
                                    "creativetab.vsia_weapon_tab"
                            ))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.BASE_HELMET.get());
                            })
                            .build()
            );

    public static final RegistryObject<CreativeModeTab> VSIA_NETWORK_TAB =
            CREATIVE_MODE_TABS.register(
                    "vsia_network_tab",
                    () -> CreativeModeTab.builder()
                            .icon(() -> new ItemStack(
                                    SignalityBlocks.SERVER_RACK_ITEM.get()
                            ))
                            .title(Component.translatable(
                                    "creativetab.vsia_network_tab"
                            ))
                            .displayItems((parameters, output) -> {
                                output.accept(SignalityBlocks.SERVER_RACK_ITEM.get());
                                output.accept(SignalityBlocks.STORAGE_SERVER_ITEM.get());
                                output.accept(SignalityBlocks.NETWORK_CABLE.get());
                            })
                            .build()
            );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
