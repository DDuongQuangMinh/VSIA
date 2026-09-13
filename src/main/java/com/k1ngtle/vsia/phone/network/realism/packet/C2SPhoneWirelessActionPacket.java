package com.k1ngtle.vsia.phone.network.realism.packet;

import com.k1ngtle.vsia.phone.network.realism.PhoneWirelessServerService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public final class C2SPhoneWirelessActionPacket {
    private static final int MAX_ACTION =
            32;

    private static final int MAX_VALUE =
            128;

    private static final int MAX_CREDENTIAL =
            128;

    private final String action;
    private final String value;
    private final String credential;
    private final boolean flag;

    public C2SPhoneWirelessActionPacket(
            String action,
            String value,
            String credential,
            boolean flag
    ) {
        this.action =
                action == null
                        ? ""
                        : action;

        this.value =
                value == null
                        ? ""
                        : value;

        this.credential =
                credential == null
                        ? ""
                        : credential;

        this.flag =
                flag;
    }

    public C2SPhoneWirelessActionPacket(
            FriendlyByteBuf buffer
    ) {
        action =
                buffer.readUtf(
                        MAX_ACTION
                );

        value =
                buffer.readUtf(
                        MAX_VALUE
                );

        credential =
                buffer.readUtf(
                        MAX_CREDENTIAL
                );

        flag =
                buffer.readBoolean();
    }

    public void toBytes(
            FriendlyByteBuf buffer
    ) {
        buffer.writeUtf(
                action,
                MAX_ACTION
        );

        buffer.writeUtf(
                value,
                MAX_VALUE
        );

        buffer.writeUtf(
                credential,
                MAX_CREDENTIAL
        );

        buffer.writeBoolean(
                flag
        );
    }

    public void handle(
            Supplier<NetworkEvent.Context> supplier
    ) {
        NetworkEvent.Context context =
                supplier.get();

        ServerPlayer player =
                context.getSender();

        if (player != null) {
            context.enqueueWork(
                    () ->
                            apply(
                                    player
                            )
            );
        }

        context.setPacketHandled(
                true
        );
    }

    private void apply(
            ServerPlayer player
    ) {
        String normalized =
                action
                        .toUpperCase(
                                Locale.ROOT
                        );

        switch (normalized) {
            case "REFRESH" ->
                    PhoneWirelessServerService
                            .refresh(
                                    player,
                                    true
                            );

            case "WIFI_ENABLE" ->
                    PhoneWirelessServerService
                            .setWifiEnabled(
                                    player,
                                    flag
                            );

            case "CELLULAR_ENABLE" ->
                    PhoneWirelessServerService
                            .setCellularEnabled(
                                    player,
                                    flag
                            );

            case "WIFI_CONNECT" ->
                    PhoneWirelessServerService
                            .connectWifi(
                                    player,
                                    value,
                                    credential
                            );

            case "WIFI_DISCONNECT" ->
                    PhoneWirelessServerService
                            .disconnectWifi(
                                    player
                            );

            case "WIFI_FORGET" ->
                    PhoneWirelessServerService
                            .forgetWifi(
                                    player,
                                    value
                            );

            case "WIFI_AUTOJOIN" ->
                    PhoneWirelessServerService
                            .setWifiAutoJoin(
                                    player,
                                    value,
                                    flag
                            );

            default ->
                    PhoneWirelessServerService
                            .refresh(
                                    player,
                                    true
                            );
        }
    }

    public static C2SPhoneWirelessActionPacket refresh() {
        return new C2SPhoneWirelessActionPacket(
                "REFRESH",
                "",
                "",
                false
        );
    }

    public static C2SPhoneWirelessActionPacket wifiEnabled(
            boolean enabled
    ) {
        return new C2SPhoneWirelessActionPacket(
                "WIFI_ENABLE",
                "",
                "",
                enabled
        );
    }

    public static C2SPhoneWirelessActionPacket cellularEnabled(
            boolean enabled
    ) {
        return new C2SPhoneWirelessActionPacket(
                "CELLULAR_ENABLE",
                "",
                "",
                enabled
        );
    }

    public static C2SPhoneWirelessActionPacket wifiConnect(
            String bssid,
            String passphrase
    ) {
        return new C2SPhoneWirelessActionPacket(
                "WIFI_CONNECT",
                bssid,
                passphrase,
                false
        );
    }

    public static C2SPhoneWirelessActionPacket wifiDisconnect() {
        return new C2SPhoneWirelessActionPacket(
                "WIFI_DISCONNECT",
                "",
                "",
                false
        );
    }

    public static C2SPhoneWirelessActionPacket wifiForget(
            String bssid
    ) {
        return new C2SPhoneWirelessActionPacket(
                "WIFI_FORGET",
                bssid,
                "",
                false
        );
    }

    public static C2SPhoneWirelessActionPacket wifiAutoJoin(
            String bssid,
            boolean enabled
    ) {
        return new C2SPhoneWirelessActionPacket(
                "WIFI_AUTOJOIN",
                bssid,
                "",
                enabled
        );
    }
}
