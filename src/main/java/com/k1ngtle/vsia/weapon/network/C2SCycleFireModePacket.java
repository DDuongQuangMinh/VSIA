package com.k1ngtle.vsia.weapon.network;

import com.k1ngtle.vsia.weapon.GunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * No payload needed and no explicit sync packet back - this mutates the
 * NBT on the player's own held ItemStack server-side, and vanilla's
 * per-tick inventory broadcast (ServerPlayer's container menu change
 * detection) picks up and syncs the change automatically.
 */
public class C2SCycleFireModePacket {

    public static void encode(C2SCycleFireModePacket msg, FriendlyByteBuf buf) {}

    public static C2SCycleFireModePacket decode(FriendlyByteBuf buf) {
        return new C2SCycleFireModePacket();
    }

    public static void handle(C2SCycleFireModePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (stack.getItem() instanceof GunItem gun) {
                gun.cycleFireMode(stack);
            }
        });
        ctx.setPacketHandled(true);
    }
}