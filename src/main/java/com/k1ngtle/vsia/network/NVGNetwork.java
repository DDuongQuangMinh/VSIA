package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import com.k1ngtle.vsia.item.HelmetGPNVG18Item;
import com.k1ngtle.vsia.item.GhillieHelmetGPNVG18Item;
import com.k1ngtle.vsia.item.HelmetGPNVG18SandItem;
import com.k1ngtle.vsia.item.HelmetGPNVG18SnowItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class NVGNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Vsia.MOD_ID, "nvg_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, TogglePacket.class, TogglePacket::encode, TogglePacket::new, TogglePacket::handle);
    }

    public static class TogglePacket {
        public TogglePacket() {}

        public TogglePacket(FriendlyByteBuf buf) {}

        public void encode(FriendlyByteBuf buf) {}

        public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);

                    if (headSlot.getItem() instanceof HelmetPVS31Item ||
                            headSlot.getItem() instanceof GhillieHelmetPVS31Item ||
                            headSlot.getItem() instanceof SandHelmetPVS31Item ||
                            headSlot.getItem() instanceof SnowHelmetPVS31Item ||
                            headSlot.getItem() instanceof HelmetGPNVG18Item ||
                            headSlot.getItem() instanceof GhillieHelmetGPNVG18Item ||
                            headSlot.getItem() instanceof HelmetGPNVG18SandItem ||
                            headSlot.getItem() instanceof HelmetGPNVG18SnowItem) {

                        boolean isActive = headSlot.hasTag() && headSlot.getTag().getBoolean("nvg_active");
                        headSlot.getOrCreateTag().putBoolean("nvg_active", !isActive);

                        // Play a physical click sound to the player
                        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 1.0f, isActive ? 0.8f : 1.2f);

                        // If turning off, instantly clear the potion effect
                        if (isActive) {
                            player.removeEffect(MobEffects.NIGHT_VISION);
                        }
                    }
                }
            });
            context.setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = Vsia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvents {
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
                ItemStack headSlot = event.player.getItemBySlot(EquipmentSlot.HEAD);

                // If wearing NVG and it's active, give them actual Night Vision
                if ((headSlot.getItem() instanceof HelmetPVS31Item ||
                        headSlot.getItem() instanceof GhillieHelmetPVS31Item ||
                        headSlot.getItem() instanceof SandHelmetPVS31Item ||
                        headSlot.getItem() instanceof SnowHelmetPVS31Item ||
                        headSlot.getItem() instanceof HelmetGPNVG18Item ||
                        headSlot.getItem() instanceof GhillieHelmetGPNVG18Item ||
                        headSlot.getItem() instanceof HelmetGPNVG18SandItem ||
                        headSlot.getItem() instanceof HelmetGPNVG18SnowItem) &&
                        headSlot.hasTag() && headSlot.getTag().getBoolean("nvg_active")) {

                    // Duration is 250 ticks. This handles the actual illumination of dark areas natively.
                    event.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 250, 0, false, false, false));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Vsia.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientSetupEvents {
        public static final KeyMapping TOGGLE_NVG_KEY = new KeyMapping(
                "key.vsia.toggle_nvg",
                GLFW.GLFW_KEY_N,
                "key.categories.vsia"
        );

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_NVG_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = Vsia.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {

        // Exact paths required by Forge 1.20.1 to prevent FileNotFoundException!
        private static final ResourceLocation GREEN_SHADER_PVS31 = new ResourceLocation(Vsia.MOD_ID, "shaders/post/nv_green_pvs31.json");
        private static final ResourceLocation GREEN_SHADER_GPNVG18 = new ResourceLocation(Vsia.MOD_ID, "shaders/post/nv_green_gpnvg18.json");

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (ClientSetupEvents.TOGGLE_NVG_KEY.consumeClick()) {
                INSTANCE.sendToServer(new TogglePacket());
            }
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            ItemStack headSlot = mc.player.getItemBySlot(EquipmentSlot.HEAD);

            boolean isPVS31 = (headSlot.getItem() instanceof HelmetPVS31Item ||
                    headSlot.getItem() instanceof GhillieHelmetPVS31Item ||
                    headSlot.getItem() instanceof SandHelmetPVS31Item ||
                    headSlot.getItem() instanceof SnowHelmetPVS31Item);

            boolean isGPNVG18 = (headSlot.getItem() instanceof HelmetGPNVG18Item ||
                    headSlot.getItem() instanceof GhillieHelmetGPNVG18Item ||
                    headSlot.getItem() instanceof HelmetGPNVG18SandItem ||
                    headSlot.getItem() instanceof HelmetGPNVG18SnowItem);

            boolean isActive = (isPVS31 || isGPNVG18) && headSlot.hasTag() && headSlot.getTag().getBoolean("nvg_active");

            // Turn Shader ON - NO First Person Check! This forces it to work in Third Person!
            if (isActive) {
                ResourceLocation targetShader = isGPNVG18 ? GREEN_SHADER_GPNVG18 : GREEN_SHADER_PVS31;

                // Keep the shader active regardless of camera mode
                if (mc.gameRenderer.currentEffect() == null || !targetShader.toString().equals(mc.gameRenderer.currentEffect().getName())) {
                    mc.gameRenderer.loadEffect(targetShader);
                }
            }
            // Turn Shader OFF safely
            else {
                if (mc.gameRenderer.currentEffect() != null &&
                        (GREEN_SHADER_PVS31.toString().equals(mc.gameRenderer.currentEffect().getName()) ||
                                GREEN_SHADER_GPNVG18.toString().equals(mc.gameRenderer.currentEffect().getName()))) {
                    mc.gameRenderer.shutdownEffect();
                }
            }
        }
    }
}