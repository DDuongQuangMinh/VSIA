package com.k1ngtle.vsia.client.wifi;

import com.k1ngtle.vsia.client.screen.WifiEngineeringScreen;
import com.k1ngtle.vsia.client.screen.WifiMultiEngineeringScreen;
import com.k1ngtle.vsia.network.wifi.WifiMultiEngineeringDeviceSnapshotPacket;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.instrument.WifiEngineeringTestLinkResult;
import com.k1ngtle.vsia.signality.engineering.wifi.ip.WifiIpEngineeringSnapshot;
import com.k1ngtle.vsia.signality.engineering.wifi.trace.WifiPacketTraceEvent;
import com.k1ngtle.vsia.signality.engineering.wifi.workflow.WifiEngineeringWorkflowSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

public final class WifiEngineeringClientPacketHandler {
    private WifiEngineeringClientPacketHandler() {
    }

    public static void handleSnapshot(
            BlockPos pos,
            WifiEngineeringSnapshot snapshot,
            boolean openScreen
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (openScreen) {
            minecraft.setScreen(
                    new WifiEngineeringScreen(pos, snapshot)
            );
            return;
        }

        if (minecraft.screen instanceof WifiEngineeringScreen screen
                && screen.targetPos().equals(pos)) {
            screen.acceptSnapshot(snapshot);
        }
    }

    public static void handleMultiOpen(List<UUID> deviceIds) {
        Minecraft.getInstance().setScreen(
                new WifiMultiEngineeringScreen(deviceIds)
        );
    }

    public static void handleMultiDeviceSnapshot(
            WifiMultiEngineeringDeviceSnapshotPacket packet
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof WifiMultiEngineeringScreen screen) {
            screen.acceptDeviceSnapshot(packet);
        }
    }

    public static void handleTestLinkResult(
            BlockPos pos,
            WifiEngineeringTestLinkResult result
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof WifiEngineeringScreen screen
                && screen.targetPos().equals(pos)) {
            screen.acceptTestLinkResult(result);
        }
    }

    public static void handlePacketTrace(
            BlockPos pos,
            List<WifiPacketTraceEvent> events
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof WifiEngineeringScreen screen
                && screen.targetPos().equals(pos)) {
            screen.acceptPacketTrace(events);
        }
    }

    public static void handleWorkflowSnapshot(
            BlockPos pos,
            WifiEngineeringWorkflowSnapshot snapshot
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof WifiEngineeringScreen screen
                && screen.targetPos().equals(pos)) {
            screen.acceptWorkflowSnapshot(snapshot);
        }
    }

    public static void handleIpSnapshot(
            BlockPos pos,
            WifiIpEngineeringSnapshot snapshot
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen instanceof WifiEngineeringScreen screen
                && screen.targetPos().equals(pos)) {
            screen.acceptIpSnapshot(snapshot);
        }
    }
}
