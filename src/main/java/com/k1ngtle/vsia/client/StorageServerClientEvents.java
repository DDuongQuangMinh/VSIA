package com.k1ngtle.vsia.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.client.renderer.StorageServerRenderer;
import com.k1ngtle.vsia.client.renderer.NetworkSwitchRenderer;
import com.k1ngtle.vsia.client.renderer.FirewallRenderer;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.k1ngtle.vsia.registry.ModMenuTypes;
import com.k1ngtle.vsia.client.screen.StorageServerScreen;
import com.k1ngtle.vsia.client.screen.NetworkSwitchScreen;
import com.k1ngtle.vsia.client.screen.FirewallScreen;

@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StorageServerClientEvents {
    private StorageServerClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SignalityBlocks.STORAGE_SERVER_BE.get(), context -> new StorageServerRenderer());
        event.registerBlockEntityRenderer(SignalityBlocks.NETWORK_SWITCH_BE.get(), context -> new NetworkSwitchRenderer());
        event.registerBlockEntityRenderer(SignalityBlocks.FIREWALL_BE.get(), context -> new FirewallRenderer());
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.STORAGE_SERVER_MENU.get(), StorageServerScreen::new);
            MenuScreens.register(ModMenuTypes.NETWORK_SWITCH_MENU.get(), NetworkSwitchScreen::new);
            MenuScreens.register(ModMenuTypes.FIREWALL_MENU.get(), FirewallScreen::new);
        });
    }
}