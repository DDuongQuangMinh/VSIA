package com.k1ngtle.vsia.weapon.server;

import com.k1ngtle.vsia.weapon.api.VSIAWeaponAPI;
import com.k1ngtle.vsia.weapon.data.WeaponDefinition;
import com.k1ngtle.vsia.weapon.network.WeaponNetwork;
import com.k1ngtle.vsia.weapon.state.FireMode;
import com.k1ngtle.vsia.weapon.state.WeaponRuntimeState;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ServerWeaponOperations {
    private static ShotExecutor shotExecutor = (player, weapon, definition) -> {};

    private ServerWeaponOperations() {}

    public static void setShotExecutor(ShotExecutor executor) {
        shotExecutor = java.util.Objects.requireNonNull(executor);
    }

    public static boolean tryFire(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        if (definition == null) return false;
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        long now = player.level().getGameTime();
        FireMode mode = state.getFireMode();
        if (state.isReloading()) cancelReload(player, hand);
        if (mode == FireMode.SAFE || !definition.fireControl().fireModes().contains(mode)
                || now < state.getNextAllowedShotTick()) return false;
        if (state.getAvailableRounds() <= 0) {
            WeaponNetwork.broadcastEvent(player, hand, com.k1ngtle.vsia.weapon.state.WeaponEventType.DRY_FIRE, "");
            state.setNextAllowedShotTick(now + 4.0D);
            return false;
        }

        if (!state.isChamberLoaded()) chamberNext(state);
        if (!state.isChamberLoaded()) return false;
        state.setChamberLoaded(false);
        chamberNext(state);
        state.setBoltLocked(state.getAvailableRounds() == 0);
        state.setLastShotGameTime(now);
        double interval = 1200.0D / definition.fireControl().roundsPerMinute();
        state.setNextAllowedShotTick(Math.max(now, state.getNextAllowedShotTick()) + interval);
        shotExecutor.execute(player, stack, definition);
        WeaponNetwork.syncState(player, hand, stack);
        return true;
    }

    public static boolean startReload(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        if (definition == null) return false;
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        if (state.isReloading() || state.getMagazineAmmo() >= definition.ammo().magazineCapacity()
                || countAmmo(player.getInventory(), definition) <= 0) return false;
        boolean empty = state.getAvailableRounds() == 0;
        state.setReloading(true);
        state.setReloadEndGameTime(player.level().getGameTime()
                + (empty ? definition.reload().emptyTicks() : definition.reload().tacticalTicks()));
        WeaponNetwork.syncState(player, hand, stack);
        WeaponNetwork.broadcastEvent(player, hand,
                com.k1ngtle.vsia.weapon.state.WeaponEventType.RELOAD_STARTED, empty ? "empty" : "tactical");
        return true;
    }

    public static void tickReload(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        if (definition == null) return;
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        if (!state.isReloading() || player.level().getGameTime() < state.getReloadEndGameTime()) return;
        int needed = definition.ammo().magazineCapacity() - state.getMagazineAmmo();
        int consumed = consumeAmmo(player.getInventory(), definition, needed);
        state.setMagazineAmmo(state.getMagazineAmmo() + consumed);
        if (definition.ammo().chambered() && !state.isChamberLoaded()) chamberNext(state);
        state.setBoltLocked(false);
        state.setReloading(false);
        WeaponNetwork.syncState(player, hand, stack);
        WeaponNetwork.broadcastEvent(player, hand,
                com.k1ngtle.vsia.weapon.state.WeaponEventType.RELOAD_COMPLETED, "");
    }

    public static boolean cancelReload(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        if (!state.isReloading()) return false;
        state.setReloading(false);
        state.setReloadEndGameTime(0L);
        WeaponNetwork.syncState(player, hand, stack);
        WeaponNetwork.broadcastEvent(player, hand,
                com.k1ngtle.vsia.weapon.state.WeaponEventType.RELOAD_CANCELLED, "");
        return true;
    }

    public static boolean cancelReloadStack(ServerPlayer player, ItemStack stack, InteractionHand eventHand) {
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        if (!state.isReloading()) return false;
        state.setReloading(false);
        state.setReloadEndGameTime(0L);
        WeaponNetwork.broadcastEvent(player, eventHand,
                com.k1ngtle.vsia.weapon.state.WeaponEventType.RELOAD_CANCELLED, "weapon_changed");
        return true;
    }

    public static boolean cycleFireMode(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WeaponDefinition definition = VSIAWeaponAPI.getWeapon(stack).orElse(null);
        if (definition == null) return false;
        WeaponRuntimeState state = WeaponRuntimeState.get(stack);
        if (state.isReloading()) return false;
        List<FireMode> modes = definition.fireControl().fireModes();
        int current = modes.indexOf(state.getFireMode());
        state.setFireMode(modes.get((current + 1 + modes.size()) % modes.size()));
        WeaponNetwork.syncState(player, hand, stack);
        WeaponNetwork.broadcastEvent(player, hand,
                com.k1ngtle.vsia.weapon.state.WeaponEventType.FIRE_MODE_CHANGED, state.getFireMode().name());
        return true;
    }

    private static void chamberNext(WeaponRuntimeState state) {
        if (state.getMagazineAmmo() > 0) {
            state.setMagazineAmmo(state.getMagazineAmmo() - 1);
            state.setChamberLoaded(true);
        }
    }

    private static int countAmmo(Inventory inventory, WeaponDefinition definition) {
        Item item = ForgeRegistries.ITEMS.getValue(definition.ammo().ammoId());
        if (item == null) return 0;
        int count = 0;
        for (ItemStack stack : inventory.items) if (stack.is(item)) count += stack.getCount();
        return count;
    }

    private static int consumeAmmo(Inventory inventory, WeaponDefinition definition, int requested) {
        Item item = ForgeRegistries.ITEMS.getValue(definition.ammo().ammoId());
        if (item == null || requested <= 0) return 0;
        int remaining = requested;
        for (ItemStack stack : inventory.items) {
            if (!stack.is(item)) continue;
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining == 0) break;
        }
        return requested - remaining;
    }
}
