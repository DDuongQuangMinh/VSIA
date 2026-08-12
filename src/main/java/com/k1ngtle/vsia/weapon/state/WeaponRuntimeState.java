package com.k1ngtle.vsia.weapon.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class WeaponRuntimeState {
    private static final String ROOT = "VSIAWeaponState";
    private static final String MAGAZINE = "MagazineAmmo";
    private static final String CHAMBERED = "ChamberLoaded";
    private static final String FIRE_MODE = "FireMode";
    private static final String BOLT_LOCKED = "BoltLocked";
    private static final String HEAT = "Heat";
    private static final String LAST_SHOT = "LastShotGameTime";
    private static final String NEXT_SHOT = "NextAllowedShotTick";
    private static final String RELOAD_END = "ReloadEndGameTime";
    private static final String RELOADING = "Reloading";

    private final ItemStack stack;

    private WeaponRuntimeState(ItemStack stack) {
        this.stack = stack;
    }

    public static WeaponRuntimeState get(ItemStack stack) {
        return new WeaponRuntimeState(stack);
    }

    private CompoundTag read() {
        CompoundTag root = stack.getTag();
        return root == null ? new CompoundTag() : root.getCompound(ROOT);
    }

    private CompoundTag write() {
        CompoundTag root = stack.getOrCreateTag();
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) root.put(ROOT, new CompoundTag());
        return root.getCompound(ROOT);
    }

    public int getMagazineAmmo() { return Math.max(0, read().getInt(MAGAZINE)); }
    public void setMagazineAmmo(int value) { write().putInt(MAGAZINE, Math.max(0, value)); }
    public boolean isChamberLoaded() { return read().getBoolean(CHAMBERED); }
    public void setChamberLoaded(boolean value) { write().putBoolean(CHAMBERED, value); }
    public boolean isBoltLocked() { return read().getBoolean(BOLT_LOCKED); }
    public void setBoltLocked(boolean value) { write().putBoolean(BOLT_LOCKED, value); }
    public float getHeat() { return Math.max(0.0F, read().getFloat(HEAT)); }
    public void setHeat(float value) { write().putFloat(HEAT, Math.max(0.0F, value)); }
    public long getLastShotGameTime() {
        CompoundTag state = read();
        return state.contains(LAST_SHOT) ? state.getLong(LAST_SHOT) : Long.MIN_VALUE / 2;
    }
    public void setLastShotGameTime(long value) { write().putLong(LAST_SHOT, value); }
    public double getNextAllowedShotTick() {
        CompoundTag state = read();
        return state.contains(NEXT_SHOT) ? state.getDouble(NEXT_SHOT) : -1.0D;
    }
    public void setNextAllowedShotTick(double value) { write().putDouble(NEXT_SHOT, value); }
    public long getReloadEndGameTime() { return read().getLong(RELOAD_END); }
    public void setReloadEndGameTime(long value) { write().putLong(RELOAD_END, value); }
    public boolean isReloading() { return read().getBoolean(RELOADING); }
    public void setReloading(boolean value) { write().putBoolean(RELOADING, value); }

    public FireMode getFireMode() {
        String value = read().getString(FIRE_MODE);
        try { return FireMode.valueOf(value); }
        catch (IllegalArgumentException exception) { return FireMode.SEMI; }
    }

    public void setFireMode(FireMode mode) { write().putString(FIRE_MODE, mode.name()); }
    public int getAvailableRounds() { return getMagazineAmmo() + (isChamberLoaded() ? 1 : 0); }

    public CompoundTag copyTag() { return read().copy(); }

    public void loadTag(CompoundTag state) {
        stack.getOrCreateTag().put(ROOT, state.copy());
    }
}
