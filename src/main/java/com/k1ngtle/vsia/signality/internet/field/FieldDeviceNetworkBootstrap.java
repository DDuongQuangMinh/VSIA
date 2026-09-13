package com.k1ngtle.vsia.signality.internet.field;

import com.k1ngtle.vsia.Vsia;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class FieldDeviceNetworkBootstrap {
    private FieldDeviceNetworkBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(
            FMLCommonSetupEvent event
    ) {
        event.enqueueWork(
                FieldDeviceNetwork::register
        );
    }
}
