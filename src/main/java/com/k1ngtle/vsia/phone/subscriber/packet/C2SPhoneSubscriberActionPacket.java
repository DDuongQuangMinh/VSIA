package com.k1ngtle.vsia.phone.subscriber.packet;

import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Locale;
import java.util.function.Supplier;

public final class C2SPhoneSubscriberActionPacket {
    private static final int MAX_ACTION = 32;
    private static final int MAX_VALUE = 192;

    private final String action;
    private final String value;
    private final boolean flag;

    public C2SPhoneSubscriberActionPacket(
            String action,
            String value,
            boolean flag
    ) {
        this.action = action == null ? "" : action;
        this.value = value == null ? "" : value;
        this.flag = flag;
    }

    public C2SPhoneSubscriberActionPacket(FriendlyByteBuf buffer) {
        action = buffer.readUtf(MAX_ACTION);
        value = buffer.readUtf(MAX_VALUE);
        flag = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeUtf(action, MAX_ACTION);
        buffer.writeUtf(value, MAX_VALUE);
        buffer.writeBoolean(flag);
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
            case "REFRESH" -> PhoneSubscriberService.refresh(player);
            case "PROVISION_TEST_SIM" -> PhoneSubscriberService.provisionTestPhysicalSim(player);
            case "INSERT_PHYSICAL" -> PhoneSubscriberService.insertFirstPhysicalSim(player);
            case "REMOVE_PHYSICAL" -> PhoneSubscriberService.removePhysicalSim(player);
            case "ACTIVATE_ESIM" -> PhoneSubscriberService.activateEsim(player, value);
            case "SELECT_PHYSICAL" -> PhoneSubscriberService.selectPhysical(player);
            case "SELECT_ESIM" -> PhoneSubscriberService.selectEsim(player);
            case "DATA_ENABLE" -> PhoneSubscriberService.setDataEnabled(player, flag);
            default -> PhoneSubscriberService.refresh(player);
        }
    }

    public static C2SPhoneSubscriberActionPacket refresh() {
        return new C2SPhoneSubscriberActionPacket("REFRESH", "", false);
    }

    public static C2SPhoneSubscriberActionPacket provisionTestSim() {
        return new C2SPhoneSubscriberActionPacket("PROVISION_TEST_SIM", "", false);
    }

    public static C2SPhoneSubscriberActionPacket insertPhysical() {
        return new C2SPhoneSubscriberActionPacket("INSERT_PHYSICAL", "", false);
    }

    public static C2SPhoneSubscriberActionPacket removePhysical() {
        return new C2SPhoneSubscriberActionPacket("REMOVE_PHYSICAL", "", false);
    }

    public static C2SPhoneSubscriberActionPacket activateEsim(String activationCode) {
        return new C2SPhoneSubscriberActionPacket("ACTIVATE_ESIM", activationCode, false);
    }

    public static C2SPhoneSubscriberActionPacket selectPhysical() {
        return new C2SPhoneSubscriberActionPacket("SELECT_PHYSICAL", "", false);
    }

    public static C2SPhoneSubscriberActionPacket selectEsim() {
        return new C2SPhoneSubscriberActionPacket("SELECT_ESIM", "", false);
    }

    public static C2SPhoneSubscriberActionPacket dataEnabled(boolean enabled) {
        return new C2SPhoneSubscriberActionPacket("DATA_ENABLE", "", enabled);
    }
}
