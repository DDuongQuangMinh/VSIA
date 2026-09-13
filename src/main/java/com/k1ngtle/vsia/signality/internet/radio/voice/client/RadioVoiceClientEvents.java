package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.Vsia;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class RadioVoiceClientEvents {
    private RadioVoiceClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase
                != TickEvent.Phase.END) {
            return;
        }

        RadioVoiceClient.get()
                .tick();
    }
}
