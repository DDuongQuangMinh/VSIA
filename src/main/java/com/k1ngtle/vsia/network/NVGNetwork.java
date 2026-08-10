package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.item.HelmetPVS31Item;
import com.k1ngtle.vsia.item.GhillieHelmetPVS31Item;
import com.k1ngtle.vsia.item.SandHelmetPVS31Item;
import com.k1ngtle.vsia.item.SnowHelmetPVS31Item;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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

                    // Check if they are wearing ANY of the PVS-31 variants
                    if (headSlot.getItem() instanceof HelmetPVS31Item ||
                            headSlot.getItem() instanceof GhillieHelmetPVS31Item ||
                            headSlot.getItem() instanceof SandHelmetPVS31Item ||
                            headSlot.getItem() instanceof SnowHelmetPVS31Item) {

                        boolean isActive = headSlot.hasTag() && headSlot.getTag().getBoolean("nvg_active");

                        // Flip the tag
                        headSlot.getOrCreateTag().putBoolean("nvg_active", !isActive);

                        // Play a physical click sound
                        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                }
            });
            context.setPacketHandled(true);
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
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (ClientSetupEvents.TOGGLE_NVG_KEY.consumeClick()) {
                INSTANCE.sendToServer(new TogglePacket());
            }
        }
    }
}