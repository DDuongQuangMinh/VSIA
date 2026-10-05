package com.k1ngtle.vsia.item;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.registry.ModCreativeTabs;
import com.k1ngtle.vsia.registry.ModItems;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds the helmet without replacing other creative-tab content. */
@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class F35HelmetCreativeEvents {
    private F35HelmetCreativeEvents(){ }
    @SubscribeEvent public static void contents(BuildCreativeModeTabContentsEvent event){if(event.getTabKey().equals(ModCreativeTabs.VSIA_TAB.getKey()))event.accept(ModItems.F35_HELMET);}
}
