package com.k1ngtle.vsia.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.client.renderer.RtAc68uRouterRenderer;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class RtAc68uRouterClientEvents {
    private RtAc68uRouterClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                SignalityBlocks.RT_AC68U_ROUTER_BE.get(),
                context -> new RtAc68uRouterRenderer()
        );
    }
}
