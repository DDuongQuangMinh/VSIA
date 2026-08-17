package com.k1ngtle.vsia.network;

import com.k1ngtle.vsia.Vsia;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
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
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
}