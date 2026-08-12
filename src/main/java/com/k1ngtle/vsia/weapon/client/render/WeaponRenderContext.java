package com.k1ngtle.vsia.weapon.client.render;

import net.minecraft.world.item.ItemStack;

final class WeaponRenderContext {
    private static final ThreadLocal<ItemStack> STACK = ThreadLocal.withInitial(() -> ItemStack.EMPTY);
    private WeaponRenderContext() {}
    static ItemStack stack() { return STACK.get(); }
    static void set(ItemStack stack) { STACK.set(stack); }
    static void clear() { STACK.remove(); }
}
