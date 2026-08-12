package com.k1ngtle.vsia.weapon.server;

import com.k1ngtle.vsia.weapon.api.VSIAWeaponAPI;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.state.FireMode;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ServerTriggerController {
    private static final Map<UUID, EnumMap<InteractionHand, TriggerState>> STATES = new ConcurrentHashMap<>();
    private ServerTriggerController() {}

    public static void setPressed(ServerPlayer player, InteractionHand hand, boolean pressed) {
        TriggerState trigger = state(player, hand);
        boolean risingEdge = pressed && !trigger.pressed;
        trigger.pressed = pressed;
        if (!risingEdge) return;
        ItemStack stack = player.getItemInHand(hand);
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        if (definition == null) return;
        trigger.weaponId = definition.id();
        FireMode mode = WeaponRuntimeState.get(stack).getFireMode();
        if (mode == FireMode.BURST) {
            trigger.burstRemaining = definition.fireControl().burstSize();
            attemptBurstShot(player, hand, trigger);
        } else if (mode == FireMode.SEMI || mode == FireMode.AUTO) {
            ServerWeaponOperations.tryFire(player, hand);
        }
    }

    public static void tick(ServerPlayer player) {
        EnumMap<InteractionHand, TriggerState> hands = STATES.get(player.getUUID());
        if (hands == null) return;
        for (InteractionHand hand : InteractionHand.values()) {
            TriggerState trigger = hands.get(hand);
            if (trigger == null) continue;
            ItemStack stack = player.getItemInHand(hand);
            ResourceLocation currentId = VSIAWeaponAPI.getWeapon(stack)
                    .map(WeaponDefinition::id).orElse(null);
            if (!java.util.Objects.equals(currentId, trigger.weaponId)) {
                trigger.pressed = false;
                trigger.burstRemaining = 0;
                continue;
            }
            FireMode mode = WeaponRuntimeState.get(stack).getFireMode();
            if (mode == FireMode.AUTO && trigger.pressed) ServerWeaponOperations.tryFire(player, hand);
            if (trigger.burstRemaining > 0) attemptBurstShot(player, hand, trigger);
        }
    }

    public static void remove(ServerPlayer player) { STATES.remove(player.getUUID()); }

    private static void attemptBurstShot(ServerPlayer player, InteractionHand hand, TriggerState trigger) {
        if (ServerWeaponOperations.tryFire(player, hand)) trigger.burstRemaining--;
    }

    private static TriggerState state(ServerPlayer player, InteractionHand hand) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new EnumMap<>(InteractionHand.class))
                .computeIfAbsent(hand, ignored -> new TriggerState());
    }

    private static final class TriggerState {
        private boolean pressed;
        private int burstRemaining;
        private ResourceLocation weaponId;
    }
}
