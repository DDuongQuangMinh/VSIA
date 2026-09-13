package com.k1ngtle.vsia.phone.subscriber;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PhoneSubscriberSavedData extends SavedData {
    public static final String DATA_NAME = "vsia_phone_subscribers";

    private final Map<String, PhoneSubscriberProfile> profiles =
            new LinkedHashMap<>();

    private final Map<UUID, DeviceState> devices =
            new LinkedHashMap<>();

    private long nextSubscriberNumber = 1L;

    public PhoneSubscriberSavedData() {
    }

    public PhoneSubscriberSavedData(CompoundTag tag) {
        loadFromTag(tag);
    }

    public static PhoneSubscriberSavedData get(ServerLevel level) {
        return level.getServer()
                .overworld()
                .getDataStorage()
                .computeIfAbsent(
                        PhoneSubscriberSavedData::new,
                        PhoneSubscriberSavedData::new,
                        DATA_NAME
                );
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag profileList = new ListTag();
        for (PhoneSubscriberProfile profile : profiles.values()) {
            profileList.add(profile.save());
        }
        tag.put("Profiles", profileList);

        ListTag deviceList = new ListTag();
        for (Map.Entry<UUID, DeviceState> entry : devices.entrySet()) {
            CompoundTag deviceTag = entry.getValue().save();
            deviceTag.putUUID("Player", entry.getKey());
            deviceList.add(deviceTag);
        }
        tag.put("Devices", deviceList);
        tag.putLong("NextSubscriberNumber", nextSubscriberNumber);
        return tag;
    }

    private void loadFromTag(CompoundTag tag) {
        profiles.clear();
        devices.clear();

        ListTag profileList = tag.getList("Profiles", Tag.TAG_COMPOUND);
        for (int i = 0; i < profileList.size(); i++) {
            PhoneSubscriberProfile profile =
                    PhoneSubscriberProfile.load(profileList.getCompound(i));
            if (!profile.iccid().isBlank()) {
                profiles.put(profile.iccid(), profile);
            }
        }

        ListTag deviceList = tag.getList("Devices", Tag.TAG_COMPOUND);
        for (int i = 0; i < deviceList.size(); i++) {
            CompoundTag deviceTag = deviceList.getCompound(i);
            if (deviceTag.hasUUID("Player")) {
                devices.put(
                        deviceTag.getUUID("Player"),
                        DeviceState.load(deviceTag)
                );
            }
        }

        nextSubscriberNumber = Math.max(
                1L,
                tag.getLong("NextSubscriberNumber")
        );
    }

    public synchronized DeviceState device(UUID playerId) {
        DeviceState state = devices.computeIfAbsent(
                playerId,
                ignored -> DeviceState.create()
        );
        setDirty();
        return state;
    }

    public synchronized Optional<PhoneSubscriberProfile> profile(String iccid) {
        return Optional.ofNullable(profiles.get(safe(iccid)));
    }

    public synchronized Optional<PhoneSubscriberProfile> profileByMsisdn(String msisdn) {
        String normalized = normalizeNumber(msisdn);
        return profiles.values()
                .stream()
                .filter(profile -> normalizeNumber(profile.msisdn()).equals(normalized))
                .findFirst();
    }

    public synchronized Optional<UUID> activeDeviceForIccid(String iccid) {
        String wanted = safe(iccid);
        return devices.entrySet()
                .stream()
                .filter(entry -> wanted.equals(entry.getValue().activeIccid()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public synchronized Optional<UUID> activeDeviceForMsisdn(String msisdn) {
        Optional<PhoneSubscriberProfile> profile = profileByMsisdn(msisdn);
        return profile.flatMap(value -> activeDeviceForIccid(value.iccid()));
    }

    public synchronized Collection<PhoneSubscriberProfile> profiles() {
        return List.copyOf(profiles.values());
    }

    public synchronized PhoneSubscriberProfile createProfile(
            String profileType,
            String eidBinding
    ) {
        long sequence = nextSubscriberNumber++;
        String suffix = String.format("%010d", sequence);
        String msisdn = "+99910" + String.format("%08d", sequence % 100_000_000L);
        String iccid = "890100" + suffix + checksumDigit(sequence);
        String imsi = "00101" + suffix;
        String secret = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        PhoneSubscriberProfile profile = new PhoneSubscriberProfile(
                iccid,
                imsi,
                msisdn,
                "VSIA Mobile",
                "00101",
                safe(profileType),
                safe(eidBinding),
                secret,
                true
        );

        profiles.put(iccid, profile);
        setDirty();
        return profile;
    }

    public synchronized void bindPhysical(UUID playerId, String iccid) {
        String normalized = safe(iccid);
        for (DeviceState state : devices.values()) {
            if (normalized.equals(state.physicalIccid())) {
                state.setPhysicalIccid("");
                if (normalized.equals(state.activeIccid())) {
                    state.setActiveIccid(state.esimIccid());
                }
            }
        }

        DeviceState state = device(playerId);
        state.setPhysicalIccid(normalized);
        state.setActiveIccid(normalized);
        setDirty();
    }

    public synchronized void bindEsim(UUID playerId, String iccid) {
        DeviceState state = device(playerId);
        state.setEsimIccid(safe(iccid));
        state.setActiveIccid(safe(iccid));
        setDirty();
    }

    public synchronized void selectPhysical(UUID playerId) {
        DeviceState state = device(playerId);
        if (!state.physicalIccid().isBlank()) {
            state.setActiveIccid(state.physicalIccid());
            setDirty();
        }
    }

    public synchronized void selectEsim(UUID playerId) {
        DeviceState state = device(playerId);
        if (!state.esimIccid().isBlank()) {
            state.setActiveIccid(state.esimIccid());
            setDirty();
        }
    }

    public synchronized String removePhysical(UUID playerId) {
        DeviceState state = device(playerId);
        String removed = state.physicalIccid();
        state.setPhysicalIccid("");
        if (removed.equals(state.activeIccid())) {
            state.setActiveIccid(state.esimIccid());
        }
        setDirty();
        return removed;
    }

    public synchronized void setDataEnabled(UUID playerId, boolean enabled) {
        device(playerId).setDataEnabled(enabled);
        setDirty();
    }

    public synchronized Optional<PhoneSubscriberProfile> activeProfile(UUID playerId) {
        DeviceState state = device(playerId);
        return profile(state.activeIccid());
    }

    public synchronized boolean ownsEsimBinding(UUID playerId) {
        DeviceState state = device(playerId);
        if (state.esimIccid().isBlank()) {
            return false;
        }
        return profile(state.esimIccid())
                .map(profile -> state.eid().equals(profile.eidBinding()))
                .orElse(false);
    }

    private static String checksumDigit(long value) {
        return Integer.toString((int) Math.floorMod(value * 7L + 3L, 10L));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static String normalizeNumber(String number) {
        if (number == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (Character.isDigit(c) || (c == '+' && out.length() == 0)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static final class DeviceState {
        private String eid;
        private String physicalIccid;
        private String esimIccid;
        private String activeIccid;
        private boolean dataEnabled;
        private String lastAuthStatus;

        private DeviceState(
                String eid,
                String physicalIccid,
                String esimIccid,
                String activeIccid,
                boolean dataEnabled,
                String lastAuthStatus
        ) {
            this.eid = safe(eid);
            this.physicalIccid = safe(physicalIccid);
            this.esimIccid = safe(esimIccid);
            this.activeIccid = safe(activeIccid);
            this.dataEnabled = dataEnabled;
            this.lastAuthStatus = safe(lastAuthStatus);
        }

        public static DeviceState create() {
            return new DeviceState(
                    "89049032" + UUID.randomUUID().toString().replace("-", "").substring(0, 24),
                    "",
                    "",
                    "",
                    true,
                    "No SIM"
            );
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("Eid", eid);
            tag.putString("PhysicalIccid", physicalIccid);
            tag.putString("EsimIccid", esimIccid);
            tag.putString("ActiveIccid", activeIccid);
            tag.putBoolean("DataEnabled", dataEnabled);
            tag.putString("LastAuthStatus", lastAuthStatus);
            return tag;
        }

        public static DeviceState load(CompoundTag tag) {
            String eid = tag.getString("Eid");
            if (eid.isBlank()) {
                eid = create().eid();
            }
            return new DeviceState(
                    eid,
                    tag.getString("PhysicalIccid"),
                    tag.getString("EsimIccid"),
                    tag.getString("ActiveIccid"),
                    !tag.contains("DataEnabled") || tag.getBoolean("DataEnabled"),
                    tag.getString("LastAuthStatus")
            );
        }

        public String eid() { return eid; }
        public String physicalIccid() { return physicalIccid; }
        public String esimIccid() { return esimIccid; }
        public String activeIccid() { return activeIccid; }
        public boolean dataEnabled() { return dataEnabled; }
        public String lastAuthStatus() { return lastAuthStatus; }

        public void setPhysicalIccid(String value) { physicalIccid = safe(value); }
        public void setEsimIccid(String value) { esimIccid = safe(value); }
        public void setActiveIccid(String value) { activeIccid = safe(value); }
        public void setDataEnabled(boolean value) { dataEnabled = value; }
        public void setLastAuthStatus(String value) { lastAuthStatus = safe(value); }
    }
}
