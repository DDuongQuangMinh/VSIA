package com.k1ngtle.vsia.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.client.renderer.ServerRackRenderer;
import com.k1ngtle.vsia.signality.SignalityBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ServerRackClientEvents {
    private ServerRackClientEvents() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SignalityBlocks.SERVER_RACK_BE.get(), context -> new ServerRackRenderer());
    }
}
