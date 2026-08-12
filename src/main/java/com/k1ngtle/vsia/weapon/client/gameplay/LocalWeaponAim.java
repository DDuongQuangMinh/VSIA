package com.k1ngtle.vsia.weapon.client.gameplay;

public final class LocalWeaponAim {
    public void setAiming(boolean aiming) {
        ClientWeaponContext.getInstance().setAiming(aiming);
        com.k1ngtle.vsia.weapon.network.WeaponNetwork.sendToServer(
                new com.k1ngtle.vsia.weapon.network.c2s.AimPacket(
                        net.minecraft.world.InteractionHand.MAIN_HAND, aiming));
    }

    public void tick(int aimTicks) {
        ClientWeaponContext context = ClientWeaponContext.getInstance();
        float step = aimTicks <= 0 ? 1.0F : 1.0F / aimTicks;
        float target = context.isAiming() ? 1.0F : 0.0F;
        float value = context.getAimProgress();
        context.setAimProgress(value < target ? Math.min(target, value + step) : Math.max(target, value - step));
    }
}
