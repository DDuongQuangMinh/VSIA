package com.k1ngtle.vsia.weapon.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "vsia", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class WeaponKeyMappings {
    private static final String CATEGORY = "key.categories.vsia.weapon";
    public static final KeyMapping RELOAD = key("key.vsia.reload", GLFW.GLFW_KEY_R);
    public static final KeyMapping FIRE_MODE = key("key.vsia.fire_mode", GLFW.GLFW_KEY_B);
    public static final KeyMapping DEBUG_HUD = key("key.vsia.debug_hud", GLFW.GLFW_KEY_F8);

    private WeaponKeyMappings() {}
    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, key, CATEGORY);
    }
    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(RELOAD); event.register(FIRE_MODE); event.register(DEBUG_HUD);
    }
}
