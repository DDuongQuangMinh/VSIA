package com.k1ngtle.vsia.signality.internet.field;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.phone.messages.packet.C2SPhoneMessageActionPacket;
import com.k1ngtle.vsia.phone.messages.packet.S2CPhoneMessageSnapshotPacket;
import com.k1ngtle.vsia.phone.network.realism.packet.C2SPhoneWirelessActionPacket;
import com.k1ngtle.vsia.phone.network.realism.packet.S2CPhoneWirelessSnapshotPacket;
import com.k1ngtle.vsia.phone.subscriber.packet.C2SPhoneSubscriberActionPacket;
import com.k1ngtle.vsia.phone.subscriber.packet.S2CPhoneSubscriberSnapshotPacket;
import com.k1ngtle.vsia.signality.internet.radio.network.C2SRadioGuiActionPacket;
import com.k1ngtle.vsia.signality.internet.radio.network.C2SRadioGuiRequestPacket;
import com.k1ngtle.vsia.signality.internet.radio.network.S2CRadioGuiSnapshotPacket;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.C2SRadioVoiceFramePacket;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.C2SRadioVoicePttPacket;
import com.k1ngtle.vsia.signality.internet.radio.voice.network.S2CRadioVoiceFramePacket;
import com.k1ngtle.vsia.signality.internet.satellite.network.C2SSatelliteGuiActionPacket;
import com.k1ngtle.vsia.signality.internet.satellite.network.C2SSatelliteGuiRequestPacket;
import com.k1ngtle.vsia.signality.internet.satellite.network.S2CSatelliteGuiSnapshotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class FieldDeviceNetwork {
    private static final String PROTOCOL = "1.6";

    private static SimpleChannel channel;
    private static int packetId;
    private static boolean registered;

    private FieldDeviceNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Vsia.MOD_ID, "field_devices"))
                .networkProtocolVersion(() -> PROTOCOL)
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .simpleChannel();

        channel.messageBuilder(
                        C2SRadioGuiRequestPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SRadioGuiRequestPacket::new)
                .encoder(C2SRadioGuiRequestPacket::toBytes)
                .consumerMainThread(C2SRadioGuiRequestPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SRadioGuiActionPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SRadioGuiActionPacket::new)
                .encoder(C2SRadioGuiActionPacket::toBytes)
                .consumerMainThread(C2SRadioGuiActionPacket::handle)
                .add();

        channel.messageBuilder(
                        S2CRadioGuiSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CRadioGuiSnapshotPacket::new)
                .encoder(S2CRadioGuiSnapshotPacket::toBytes)
                .consumerMainThread(S2CRadioGuiSnapshotPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SRadioVoicePttPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SRadioVoicePttPacket::new)
                .encoder(C2SRadioVoicePttPacket::toBytes)
                .consumerMainThread(C2SRadioVoicePttPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SRadioVoiceFramePacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SRadioVoiceFramePacket::new)
                .encoder(C2SRadioVoiceFramePacket::toBytes)
                .consumerMainThread(C2SRadioVoiceFramePacket::handle)
                .add();

        channel.messageBuilder(
                        S2CRadioVoiceFramePacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CRadioVoiceFramePacket::new)
                .encoder(S2CRadioVoiceFramePacket::toBytes)
                .consumerMainThread(S2CRadioVoiceFramePacket::handle)
                .add();

        channel.messageBuilder(
                        C2SSatelliteGuiRequestPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SSatelliteGuiRequestPacket::new)
                .encoder(C2SSatelliteGuiRequestPacket::toBytes)
                .consumerMainThread(C2SSatelliteGuiRequestPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SSatelliteGuiActionPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SSatelliteGuiActionPacket::new)
                .encoder(C2SSatelliteGuiActionPacket::toBytes)
                .consumerMainThread(C2SSatelliteGuiActionPacket::handle)
                .add();

        channel.messageBuilder(
                        S2CSatelliteGuiSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CSatelliteGuiSnapshotPacket::new)
                .encoder(S2CSatelliteGuiSnapshotPacket::toBytes)
                .consumerMainThread(S2CSatelliteGuiSnapshotPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SPhoneWirelessActionPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SPhoneWirelessActionPacket::new)
                .encoder(C2SPhoneWirelessActionPacket::toBytes)
                .consumerMainThread(C2SPhoneWirelessActionPacket::handle)
                .add();

        channel.messageBuilder(
                        S2CPhoneWirelessSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CPhoneWirelessSnapshotPacket::new)
                .encoder(S2CPhoneWirelessSnapshotPacket::toBytes)
                .consumerMainThread(S2CPhoneWirelessSnapshotPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SPhoneSubscriberActionPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SPhoneSubscriberActionPacket::new)
                .encoder(C2SPhoneSubscriberActionPacket::toBytes)
                .consumerMainThread(C2SPhoneSubscriberActionPacket::handle)
                .add();

        channel.messageBuilder(
                        S2CPhoneSubscriberSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CPhoneSubscriberSnapshotPacket::new)
                .encoder(S2CPhoneSubscriberSnapshotPacket::toBytes)
                .consumerMainThread(S2CPhoneSubscriberSnapshotPacket::handle)
                .add();

        channel.messageBuilder(
                        C2SPhoneMessageActionPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_SERVER
                )
                .decoder(C2SPhoneMessageActionPacket::new)
                .encoder(C2SPhoneMessageActionPacket::toBytes)
                .consumerMainThread(C2SPhoneMessageActionPacket::handle)
                .add();

        channel.messageBuilder(
                        S2CPhoneMessageSnapshotPacket.class,
                        id(),
                        NetworkDirection.PLAY_TO_CLIENT
                )
                .decoder(S2CPhoneMessageSnapshotPacket::new)
                .encoder(S2CPhoneMessageSnapshotPacket::toBytes)
                .consumerMainThread(S2CPhoneMessageSnapshotPacket::handle)
                .add();

        registered = true;
    }

    private static int id() {
        return packetId++;
    }

    public static <MSG> void sendToServer(MSG message) {
        if (channel != null) {
            channel.sendToServer(message);
        }
    }

    public static <MSG> void sendToPlayer(
            ServerPlayer player,
            MSG message
    ) {
        if (channel == null || player == null) {
            return;
        }

        channel.send(
                PacketDistributor.PLAYER.with(() -> player),
                message
        );
    }
}
