package com.k1ngtle.vsia.signality.internet.radio.voice.client;

import com.k1ngtle.vsia.Vsia;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class RadioVoiceKeyMappings {
    public static final KeyMapping PTT =
            new KeyMapping(
                    "key.vsia.radio_ptt",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    "key.categories.vsia"
            );

    private RadioVoiceKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(
                PTT
        );
    }
}
