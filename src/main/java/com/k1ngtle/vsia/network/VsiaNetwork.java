package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringModePacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotPacket;
import com.k1ngtle.vsia.network.wifi.WifiEngineeringSnapshotRequestPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class VsiaNetwork {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Vsia.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(UploadFilePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(UploadFilePacket::new)
                .encoder(UploadFilePacket::toBytes)
                .consumerMainThread(UploadFilePacket::handle)
                .add();

        net.messageBuilder(DeleteFilePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeleteFilePacket::new)
                .encoder(DeleteFilePacket::toBytes)
                .consumerMainThread(DeleteFilePacket::handle)
                .add();

        net.messageBuilder(DeviceCommandPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(DeviceCommandPacket::new)
                .encoder(DeviceCommandPacket::toBytes)
                .consumerMainThread(DeviceCommandPacket::handle)
                .add();

        net.messageBuilder(
                        WifiEngineeringSnapshotRequestPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(WifiEngineeringSnapshotRequestPacket::new)
                .encoder(WifiEngineeringSnapshotRequestPacket::toBytes)
                .consumerMainThread(WifiEngineeringSnapshotRequestPacket::handle)
                .add();

        net.messageBuilder(
                        WifiEngineeringModePacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(WifiEngineeringModePacket::new)
                .encoder(WifiEngineeringModePacket::toBytes)
                .consumerMainThread(WifiEngineeringModePacket::handle)
                .add();

        net.messageBuilder(
                        WifiEngineeringSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(WifiEngineeringSnapshotPacket::new)
                .encoder(WifiEngineeringSnapshotPacket::toBytes)
                .consumerMainThread(WifiEngineeringSnapshotPacket::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(
            ServerPlayer player,
            MSG message
    ) {
        INSTANCE.send(
                PacketDistributor.PLAYER.with(
                        () -> player
                ),
                message
        );
    }
}
