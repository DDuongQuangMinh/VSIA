package com.k1ngtle.vsia.phone.subscriber;

import com.k1ngtle.vsia.phone.network.realism.PhoneWirelessServerService;
import com.k1ngtle.vsia.phone.subscriber.packet.S2CPhoneSubscriberSnapshotPacket;
import com.k1ngtle.vsia.registry.ModItems;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;

public final class PhoneSubscriberService {
    public static final String DEV_ESIM_ACTIVATION =
            "LPA:1$vsia.smdp$DEV";

    private PhoneSubscriberService() {
    }

    public static void refresh(ServerPlayer player) {
        if (player == null) {
            return;
        }
        sendSnapshot(player);
    }

    public static PhoneSubscriberSnapshot snapshot(ServerPlayer player) {
        if (player == null) {
            return PhoneSubscriberSnapshot.empty();
        }

        PhoneSubscriberSavedData data =
                PhoneSubscriberSavedData.get(player.serverLevel());

        PhoneSubscriberSavedData.DeviceState device =
                data.device(player.getUUID());

        Optional<PhoneSubscriberProfile> activeOptional =
                data.activeProfile(player.getUUID());

        boolean physicalPresent = !device.physicalIccid().isBlank();
        boolean esimPresent = !device.esimIccid().isBlank();

        if (activeOptional.isEmpty()) {
            device.setLastAuthStatus("No SIM or eSIM selected");
            data.setDirty();

            return new PhoneSubscriberSnapshot(
                    device.eid(),
                    physicalPresent,
                    esimPresent,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    false,
                    device.dataEnabled(),
                    "No SIM"
            );
        }

        PhoneSubscriberProfile profile = activeOptional.get();
        boolean bindingValid = !profile.isEsim()
                || device.eid().equals(profile.eidBinding());

        boolean auth = bindingValid
                && profile.enabled()
                && authenticate(profile, profile.plmn());

        String status;
        if (!bindingValid) {
            status = "eSIM is bound to another EID";
        } else if (!profile.enabled()) {
            status = "Subscriber profile disabled";
        } else if (!auth) {
            status = "USIM authentication failed";
        } else {
            status = "Authenticated";
        }

        device.setLastAuthStatus(status);
        data.setDirty();

        return new PhoneSubscriberSnapshot(
                device.eid(),
                physicalPresent,
                esimPresent,
                profile.profileType(),
                profile.carrier(),
                profile.plmn(),
                profile.msisdn(),
                maskIccid(profile.iccid()),
                maskImsi(profile.imsi()),
                auth,
                device.dataEnabled(),
                status
        );
    }

    public static boolean hasActiveSubscription(ServerPlayer player) {
        return snapshot(player).hasActiveSubscription();
    }

    public static boolean canUseCellularData(ServerPlayer player) {
        return snapshot(player).canUseCellularData();
    }

    public static Optional<PhoneSubscriberProfile> activeProfile(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        PhoneSubscriberSavedData data =
                PhoneSubscriberSavedData.get(player.serverLevel());
        return data.activeProfile(player.getUUID())
                .filter(PhoneSubscriberProfile::enabled)
                .filter(profile -> !profile.isEsim()
                        || data.device(player.getUUID())
                        .eid()
                        .equals(profile.eidBinding()));
    }

    public static void setDataEnabled(ServerPlayer player, boolean enabled) {
        if (player == null) {
            return;
        }
        PhoneSubscriberSavedData.get(player.serverLevel())
                .setDataEnabled(player.getUUID(), enabled);
        sendSnapshot(player);
    }

    public static void provisionTestPhysicalSim(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PhoneSubscriberSavedData data =
                PhoneSubscriberSavedData.get(player.serverLevel());

        PhoneSubscriberProfile profile =
                data.createProfile("PHYSICAL", "");

        ItemStack stack = createPhysicalSimStack(profile);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "[VSIA PHONE] Test physical SIM provisioned: "
                                + profile.msisdn()
                )
        );
        sendSnapshot(player);
    }

    public static void insertFirstPhysicalSim(ServerPlayer player) {
        if (player == null) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(ModItems.PHYSICAL_SIM_CARD.get())) {
                continue;
            }

            PhoneSubscriberProfile profile = ensurePhysicalProfile(
                    player.serverLevel(),
                    stack
            );

            if (profile == null) {
                continue;
            }

            PhoneSubscriberSavedData.get(player.serverLevel())
                    .bindPhysical(player.getUUID(), profile.iccid());

            stack.shrink(1);
            player.getInventory().setChanged();
            sendSnapshot(player);
            return;
        }

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "[VSIA PHONE] No physical SIM card is in your inventory."
                )
        );
        sendSnapshot(player);
    }

    public static void insertPhysicalSimFromStack(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }

        PhoneSubscriberProfile profile =
                ensurePhysicalProfile(player.serverLevel(), stack);

        if (profile == null) {
            return;
        }

        PhoneSubscriberSavedData.get(player.serverLevel())
                .bindPhysical(player.getUUID(), profile.iccid());

        stack.shrink(1);
        sendSnapshot(player);
    }

    public static void removePhysicalSim(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PhoneSubscriberSavedData data =
                PhoneSubscriberSavedData.get(player.serverLevel());

        String iccid = data.removePhysical(player.getUUID());
        if (iccid.isBlank()) {
            sendSnapshot(player);
            return;
        }

        PhoneSubscriberProfile profile =
                data.profile(iccid).orElse(null);

        if (profile != null) {
            ItemStack stack = createPhysicalSimStack(profile);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        sendSnapshot(player);
    }

    public static void activateEsim(
            ServerPlayer player,
            String activationCode
    ) {
        if (player == null) {
            return;
        }

        if (!DEV_ESIM_ACTIVATION.equalsIgnoreCase(
                activationCode == null ? "" : activationCode.trim()
        )) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[VSIA PHONE] eSIM activation code rejected."
                    )
            );
            sendSnapshot(player);
            return;
        }

        PhoneWirelessServerService.AccessDecision wifi =
                PhoneWirelessServerService.validateDataAccess(
                        player,
                        "WIFI"
                );

        if (!wifi.allowed()) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal(
                            "[VSIA PHONE] eSIM download needs working Wi-Fi: "
                                    + wifi.detail()
                    )
            );
            sendSnapshot(player);
            return;
        }

        PhoneSubscriberSavedData data =
                PhoneSubscriberSavedData.get(player.serverLevel());
        PhoneSubscriberSavedData.DeviceState device =
                data.device(player.getUUID());

        if (!device.esimIccid().isBlank()) {
            data.selectEsim(player.getUUID());
            sendSnapshot(player);
            return;
        }

        PhoneSubscriberProfile profile =
                data.createProfile("ESIM", device.eid());
        data.bindEsim(player.getUUID(), profile.iccid());

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "[VSIA PHONE] eSIM downloaded from VSIA SM-DP+: "
                                + profile.msisdn()
                )
        );
        sendSnapshot(player);
    }

    public static void selectPhysical(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PhoneSubscriberSavedData.get(player.serverLevel())
                .selectPhysical(player.getUUID());
        sendSnapshot(player);
    }

    public static void selectEsim(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PhoneSubscriberSavedData.get(player.serverLevel())
                .selectEsim(player.getUUID());
        sendSnapshot(player);
    }

    public static boolean normalCellularServiceAvailable(ServerPlayer player) {
        if (!hasActiveSubscription(player)) {
            return false;
        }

        PhoneWirelessServerService.AccessDecision radio =
                PhoneWirelessServerService.validateDataAccess(
                        player,
                        "CELLULAR"
                );

        return radio.allowed();
    }

    public static ItemStack createPhysicalSimStack(PhoneSubscriberProfile profile) {
        ItemStack stack = new ItemStack(ModItems.PHYSICAL_SIM_CARD.get());
        if (profile != null) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("VSIA_SIM_ICCID", profile.iccid());
            tag.putString("VSIA_SIM_MSISDN", profile.msisdn());
            tag.putString("VSIA_SIM_CARRIER", profile.carrier());
        }
        return stack;
    }

    public static PhoneSubscriberProfile ensurePhysicalProfile(
            ServerLevel level,
            ItemStack stack
    ) {
        if (level == null || stack == null || stack.isEmpty()) {
            return null;
        }

        PhoneSubscriberSavedData data = PhoneSubscriberSavedData.get(level);
        CompoundTag tag = stack.getOrCreateTag();
        String iccid = tag.getString("VSIA_SIM_ICCID");

        PhoneSubscriberProfile existing = data.profile(iccid).orElse(null);
        if (existing != null) {
            return existing;
        }

        PhoneSubscriberProfile created = data.createProfile("PHYSICAL", "");
        tag.putString("VSIA_SIM_ICCID", created.iccid());
        tag.putString("VSIA_SIM_MSISDN", created.msisdn());
        tag.putString("VSIA_SIM_CARRIER", created.carrier());
        return created;
    }

    private static void sendSnapshot(ServerPlayer player) {
        FieldDeviceNetwork.sendToPlayer(
                player,
                new S2CPhoneSubscriberSnapshotPacket(snapshot(player))
        );
    }

    private static boolean authenticate(
            PhoneSubscriberProfile profile,
            String visitedPlmn
    ) {
        if (profile == null || !profile.enabled()) {
            return false;
        }

        if (!profile.plmn().equals(visitedPlmn)) {
            return false;
        }

        try {
            String rand = profile.iccid()
                    + ':'
                    + profile.imsi()
                    + ':'
                    + visitedPlmn;

            byte[] expected = hmac(
                    profile.authSecret(),
                    rand
            );

            byte[] response = hmac(
                    profile.authSecret(),
                    rand
            );

            return MessageDigest.isEqual(expected, response);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static byte[] hmac(String secret, String message) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        ));
        return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
    }

    private static String maskIccid(String value) {
        if (value == null || value.length() <= 8) {
            return value == null ? "" : value;
        }
        return value.substring(0, 4)
                + "••••••"
                + value.substring(value.length() - 4);
    }

    private static String maskImsi(String value) {
        if (value == null || value.length() <= 8) {
            return value == null ? "" : value;
        }
        return value.substring(0, 5)
                + "••••••"
                + value.substring(value.length() - 3);
    }
}
