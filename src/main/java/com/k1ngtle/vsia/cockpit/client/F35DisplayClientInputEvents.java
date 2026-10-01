package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35SeatController;
import com.k1ngtle.vsia.cockpit.network.C2SF35DetectionFilterPacket;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = Vsia.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class F35DisplayClientInputEvents {
    private static UUID syncedPlayerId;
    private static boolean wasInCockpit;

    private F35DisplayClientInputEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.getConnection() == null) {
            syncedPlayerId = null;
            wasInCockpit = false;
            F35TargetLockClient.clear();
            return;
        }

        UUID playerId = minecraft.player.getUUID();

        if (!playerId.equals(syncedPlayerId)) {
            F35DisplayClientConfig.ensureLoaded();

            VsiaNetwork.sendToServer(
                    new C2SF35DetectionFilterPacket(
                            F35DisplayClientConfig.detectMobs(),
                            F35DisplayClientConfig.detectPlayers(),
                            F35DisplayClientConfig.detectShips(),
                            F35DisplayClientConfig.showMissiles()
                    )
            );

            syncedPlayerId = playerId;
        }

        boolean inCockpit = isInF35Cockpit(minecraft);

        if (inCockpit && !wasInCockpit) {
            minecraft.player.displayClientMessage(
                    Component.literal(
                            "F-35: [1] SMS  [2] SENSOR  [3] TSD  [4] HSI  [\\] CONFIG"
                    ),
                    true
            );
        }

        if (!inCockpit && wasInCockpit) {
            F35TargetLockClient.clear();
        }

        wasInCockpit = inCockpit;

        while (F35DisplayKeyMappings.CONFIGURE_DISPLAY.consumeClick()) {
            if (minecraft.screen instanceof F35DisplayConfigScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen instanceof F35DisplaySectionScreen) {
                minecraft.setScreen(new F35DisplayConfigScreen());
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new F35DisplayConfigScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(
            InputEvent.Key event
    ) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        int section = F35DisplaySectionScreen.sectionForKey(event.getKey());

        if (section == 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null
                || !isInF35Cockpit(minecraft)) {
            return;
        }

        minecraft.setScreen(
                new F35DisplaySectionScreen(section)
        );
    }

    private static boolean isInF35Cockpit(
            Minecraft minecraft
    ) {
        if (minecraft.player == null
                || !minecraft.player.isPassenger()) {
            return false;
        }

        Entity vehicle = minecraft.player.getVehicle();

        if (!(vehicle instanceof Minecart)) {
            return false;
        }

        return F35SeatController.isSeatRenderMarker(
                vehicle
        );
    }
}
