package com.k1ngtle.vsia.weapon.client.gameplay;

import com.k1ngtle.vsia.weapon.state.WeaponActionController;
public final class ClientWeaponContext {
    private static final ClientWeaponContext INSTANCE = new ClientWeaponContext();
    private final WeaponActionController actions = new WeaponActionController();
    private boolean aiming;
    private float aimProgress;
    private float previousAimProgress;
    private long lastShotGameTime = Long.MIN_VALUE;
    private float recoilPitch;
    private float recoilYaw;

    private ClientWeaponContext() {}
    public static ClientWeaponContext getInstance() { return INSTANCE; }
    public WeaponActionController actions() { return actions; }
    public boolean isAiming() { return aiming; }
    public void setAiming(boolean aiming) { this.aiming = aiming; }
    public float getAimProgress() { return aimProgress; }
    public float getPreviousAimProgress() { return previousAimProgress; }
    public float getInterpolatedAimProgress(float partialTick) {
        return previousAimProgress + (aimProgress - previousAimProgress) * partialTick;
    }
    public void setAimProgress(float value) {
        previousAimProgress = aimProgress;
        aimProgress = Math.max(0.0F, Math.min(1.0F, value));
    }
    public long getLastShotGameTime() { return lastShotGameTime; }
    public void setLastShotGameTime(long value) { lastShotGameTime = value; }
    public float getRecoilPitch() { return recoilPitch; }
    public float getRecoilYaw() { return recoilYaw; }
    public void setRecoil(float pitch, float yaw) { recoilPitch = pitch; recoilYaw = yaw; }
    public void reset() {
        aiming = false;
        aimProgress = previousAimProgress = recoilPitch = recoilYaw = 0.0F;
        lastShotGameTime = Long.MIN_VALUE;
        actions.reset();
    }
}
