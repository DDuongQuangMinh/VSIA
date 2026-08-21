package com.k1ngtle.vsia;

import com.k1ngtle.vsia.client.screen.RtAc68uRouterScreen;
import net.minecraft.client.gui.screens.MenuScreens;

import com.k1ngtle.vsia.network.NVGNetwork;
import com.k1ngtle.vsia.registry.ModCreativeTabs;
import com.k1ngtle.vsia.registry.ModItems;
import com.k1ngtle.vsia.registry.ModMenuTypes;
import com.k1ngtle.vsia.signality.Signality;
import com.k1ngtle.vsia.signality.internet.server.ServerRackNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Vsia.MOD_ID)
public class Vsia {
    public static final String MOD_ID = "vsia";
    public static final Logger LOGGER = LogManager.getLogger();

    public Vsia() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Mod Assets
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Initialize sub-systems
        Signality.initialize(FMLJavaModLoadingContext.get());

        // Register legacy networks
        NVGNetwork.register();
        ServerRackNetwork.register();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("VSIA Common Setup: Preparing integrations...");

            // Registering the new Storage Server Network packets for file transfer
            com.k1ngtle.vsia.network.VsiaNetwork.register();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("VSIA Client Setup: Loading 3D Armor Renderers...");
        event.enqueueWork(() ->
                MenuScreens.register(
                        ModMenuTypes.RT_AC68U_ROUTER_MENU.get(),
                        RtAc68uRouterScreen::new
                )
        );
    }
}