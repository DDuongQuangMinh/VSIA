package com.k1ngtle.vsia.weapon.network;

import com.k1ngtle.vsia.weapon.network.c2s.CycleFireModePacket;
import com.k1ngtle.vsia.weapon.network.c2s.TriggerPacket;
import com.k1ngtle.vsia.weapon.network.c2s.ReloadPacket;
import com.k1ngtle.vsia.weapon.network.c2s.AimPacket;
import com.k1ngtle.vsia.weapon.network.c2s.CancelReloadPacket;
import com.k1ngtle.vsia.weapon.network.s2c.WeaponEventPacket;
import com.k1ngtle.vsia.weapon.state.WeaponEventType;
import com.k1ngtle.vsia.weapon.network.s2c.WeaponStatePacket;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class WeaponNetwork {
    private static final String PROTOCOL = "1";
    private static SimpleChannel channel;

    private WeaponNetwork() {}

    public static void initialize(String modId) {
        if (channel != null) return;
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(modId, "weapon"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        int id = 0;
        channel.registerMessage(id++, TriggerPacket.class, TriggerPacket::encode, TriggerPacket::decode,
                TriggerPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, ReloadPacket.class, ReloadPacket::encode, ReloadPacket::decode,
                ReloadPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, CycleFireModePacket.class, CycleFireModePacket::encode,
                CycleFireModePacket::decode, CycleFireModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, AimPacket.class, AimPacket::encode, AimPacket::decode,
                AimPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, CancelReloadPacket.class, CancelReloadPacket::encode, CancelReloadPacket::decode,
                CancelReloadPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(id++, WeaponStatePacket.class, WeaponStatePacket::encode,
                WeaponStatePacket::decode, WeaponStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(id, WeaponEventPacket.class, WeaponEventPacket::encode,
                WeaponEventPacket::decode, WeaponEventPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(Object packet) {
        requireInitialized().sendToServer(packet);
    }

    public static void syncState(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        requireInitialized().send(PacketDistributor.PLAYER.with(() -> player),
                new WeaponStatePacket(hand, WeaponRuntimeState.get(stack).copyTag()));
    }

    public static void broadcastEvent(ServerPlayer player, InteractionHand hand, WeaponEventType type, String detail) {
        requireInitialized().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                WeaponEventPacket.simple(player.getId(), hand, type, detail));
    }

    public static void broadcastShot(ServerPlayer player, net.minecraft.world.phys.Vec3 start,
                                     net.minecraft.world.phys.Vec3 end) {
        requireInitialized().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new WeaponEventPacket(player.getId(), InteractionHand.MAIN_HAND, WeaponEventType.SHOT,
                        start, end, ""));
    }

    private static SimpleChannel requireInitialized() {
        if (channel == null) throw new IllegalStateException("WeaponNetwork.initialize must be called first");
        return channel;
    }
}
