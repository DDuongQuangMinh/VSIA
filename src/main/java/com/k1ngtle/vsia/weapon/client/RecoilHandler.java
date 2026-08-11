package com.k1ngtle.vsia.weapon.client;

import com.k1ngtle.vsia.weapon.registry.WeaponItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Accumulates a pitch/yaw offset on fire and decays it back to zero
 * each tick, applied to the camera via ViewportEvent.ComputeCameraAngles.
 *
 * NOTE: ComputeCameraAngles is a Forge 1.20.1 event - confirm the exact
 * package/class still matches your Forge version. If it's moved, this
 * is the only class that needs to change; everything else in the gun
 * system is decoupled from it.
 */
@Mod.EventBusSubscriber(modid = WeaponItems.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecoilHandler {

    private static double pitchOffset = 0.0;
    private static double yawOffset = 0.0;
    private static double recoverySpeed = 0.15;

    private RecoilHandler() {}

    /** Called from ClientGunFeedback when this client's own shot lands. */
    public static void applyKick(double pitchKick, double yawKick, double recovery) {
        pitchOffset += pitchKick;
        yawOffset += (Math.random() - 0.5) * 2.0 * yawKick;
        recoverySpeed = recovery;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        pitchOffset *= (1.0 - recoverySpeed);
        yawOffset *= (1.0 - recoverySpeed);
        if (Math.abs(pitchOffset) < 0.001) pitchOffset = 0.0;
        if (Math.abs(yawOffset) < 0.001) yawOffset = 0.0;
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (pitchOffset == 0.0 && yawOffset == 0.0) return;
        event.setPitch((float) (event.getPitch() - pitchOffset));
        event.setYaw((float) (event.getYaw() + yawOffset));
    }
}