package com.k1ngtle.vsia.cockpit.client;

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
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class F35DisplayKeyMappings {
    public static final KeyMapping CONFIGURE_DISPLAY =
            new KeyMapping(
                    "key.vsia.f35_display_config",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_BACKSLASH,
                    "key.categories.vsia"
            );

    private F35DisplayKeyMappings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(
            RegisterKeyMappingsEvent event
    ) {
        event.register(
                CONFIGURE_DISPLAY
        );
    }
}
