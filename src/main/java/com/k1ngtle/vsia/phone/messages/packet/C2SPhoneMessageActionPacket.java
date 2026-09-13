package com.k1ngtle.vsia.phone.messages.packet;

import com.k1ngtle.vsia.phone.messages.PhoneMessageService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public final class C2SPhoneMessageActionPacket {
    private static final int MAX_ACTION = 24;
    private static final int MAX_NUMBER = 32;
    private static final int MAX_BODY = 320;

    private final String action;
    private final String number;
    private final String body;

    public C2SPhoneMessageActionPacket(
            String action,
            String number,
            String body
    ) {
        this.action = action == null ? "" : action;
        this.number = number == null ? "" : number;
        this.body = body == null ? "" : body;
    }

    public C2SPhoneMessageActionPacket(FriendlyByteBuf buffer) {
        action = buffer.readUtf(MAX_ACTION);
        number = buffer.readUtf(MAX_NUMBER);
        body = buffer.readUtf(MAX_BODY);
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(action, MAX_ACTION);
        buffer.writeUtf(number, MAX_NUMBER);
        buffer.writeUtf(body, MAX_BODY);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        ServerPlayer player = context.getSender();

        if (player != null) {
            context.enqueueWork(() -> apply(player));
        }

        context.setPacketHandled(true);
    }

    private void apply(ServerPlayer player) {
        switch (action.toUpperCase(Locale.ROOT)) {
            case "SEND" -> PhoneMessageService.sendSms(player, number, body);
            default -> PhoneMessageService.refresh(player);
        }
    }

    public static C2SPhoneMessageActionPacket refresh() {
        return new C2SPhoneMessageActionPacket("REFRESH", "", "");
    }

    public static C2SPhoneMessageActionPacket send(String number, String body) {
        return new C2SPhoneMessageActionPacket("SEND", number, body);
    }
}
