package com.k1ngtle.vsia.cockpit.iff;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;

/** Server-owned transponder configuration. Secret bytes never enter update tags or packets. */
public final class F35IffConfig {
    public enum Master { OFF, STBY, NORM, EMER }

    private Master master = Master.STBY;
    private String mode1 = "00";
    private String mode2 = "0000";
    private String mode3a = "1200";
    private boolean mode1Enabled = true, mode2Enabled = true, mode3aEnabled = true;
    private boolean mode4Enabled = true, mode5Enabled = true;
    private boolean sendPosition = true, sendVelocity = true, sendHeading = true, sendMission = true;
    private int activeSlot;
    private final KeySlot[] slots = { new KeySlot(), new KeySlot() };
    private long sequence;

    public Master master() { return master; }
    public String mode1() { return mode1; }
    public String mode2() { return mode2; }
    public String mode3a() { return mode3a; }
    public boolean mode1Enabled() { return mode1Enabled; }
    public boolean mode2Enabled() { return mode2Enabled; }
    public boolean mode3aEnabled() { return mode3aEnabled; }
    public boolean mode4Enabled() { return mode4Enabled; }
    public boolean mode5Enabled() { return mode5Enabled; }
    public boolean sendPosition() { return sendPosition; }
    public boolean sendVelocity() { return sendVelocity; }
    public boolean sendHeading() { return sendHeading; }
    public boolean sendMission() { return sendMission; }
    public int activeSlot() { return activeSlot; }
    public KeySlot activeKey() { return slots[activeSlot]; }
    public long nextSequence() { return ++sequence; }

    public void cycleMaster() { master = Master.values()[(master.ordinal() + 1) % Master.values().length]; }
    public void setCodes(String m1, String m2, String m3a) {
        mode1 = decimal(m1, 2); mode2 = decimal(m2, 4); mode3a = octal(m3a, 4);
    }
    public void toggleMode(int mode) {
        switch (mode) { case 1 -> mode1Enabled = !mode1Enabled; case 2 -> mode2Enabled = !mode2Enabled;
            case 3 -> mode3aEnabled = !mode3aEnabled; case 4 -> mode4Enabled = !mode4Enabled;
            case 5 -> mode5Enabled = !mode5Enabled; default -> { } }
    }
    public void toggleTelemetry(int field) {
        switch (field) { case 0 -> sendPosition = !sendPosition; case 1 -> sendVelocity = !sendVelocity;
            case 2 -> sendHeading = !sendHeading; case 3 -> sendMission = !sendMission; default -> { } }
    }
    public void selectSlot(int slot) { activeSlot = Math.max(0, Math.min(1, slot)); }
    public void generateKey(int slot, String id, long lifetimeSeconds) {
        byte[] secret = new byte[32]; new SecureRandom().nextBytes(secret);
        slots[Math.max(0, Math.min(1, slot))] = new KeySlot(id, secret,
                Instant.now().getEpochSecond() + Math.max(60, lifetimeSeconds));
    }
    public void loadKey(int slot, String id, byte[] secret, long expiresAt) {
        if (secret == null || secret.length < 16) throw new IllegalArgumentException("Mission key must be at least 128 bits");
        slots[Math.max(0, Math.min(1, slot))] = new KeySlot(id, Arrays.copyOf(secret, secret.length), expiresAt);
    }
    public void zeroize() { for (KeySlot slot : slots) slot.zeroize(); sequence = 0; master = Master.OFF; }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag(); t.putString("Master", master.name()); t.putString("M1", mode1);
        t.putString("M2", mode2); t.putString("M3A", mode3a); t.putBoolean("E1", mode1Enabled);
        t.putBoolean("E2", mode2Enabled); t.putBoolean("E3", mode3aEnabled); t.putBoolean("E4", mode4Enabled);
        t.putBoolean("E5", mode5Enabled); t.putBoolean("TP", sendPosition); t.putBoolean("TV", sendVelocity);
        t.putBoolean("TH", sendHeading); t.putBoolean("TM", sendMission); t.putInt("Slot", activeSlot);
        t.putLong("Seq", sequence); t.put("KeyA", slots[0].save()); t.put("KeyB", slots[1].save()); return t;
    }
    public void load(CompoundTag t) {
        try { master = Master.valueOf(t.getString("Master")); } catch (Exception ignored) { master = Master.STBY; }
        mode1 = decimal(t.getString("M1"), 2); mode2 = decimal(t.getString("M2"), 4); mode3a = octal(t.getString("M3A"), 4);
        mode1Enabled = t.getBoolean("E1"); mode2Enabled = t.getBoolean("E2"); mode3aEnabled = t.getBoolean("E3");
        mode4Enabled = t.getBoolean("E4"); mode5Enabled = t.getBoolean("E5"); sendPosition = t.getBoolean("TP");
        sendVelocity = t.getBoolean("TV"); sendHeading = t.getBoolean("TH"); sendMission = t.getBoolean("TM");
        activeSlot = Math.max(0, Math.min(1, t.getInt("Slot"))); sequence = Math.max(0, t.getLong("Seq"));
        if (t.contains("KeyA")) slots[0] = KeySlot.load(t.getCompound("KeyA"));
        if (t.contains("KeyB")) slots[1] = KeySlot.load(t.getCompound("KeyB"));
    }
    private static String decimal(String s, int n) { String v = s == null ? "" : s.replaceAll("[^0-9]", ""); return pad(v, n); }
    private static String octal(String s, int n) { String v = s == null ? "" : s.replaceAll("[^0-7]", ""); return pad(v, n); }
    private static String pad(String s, int n) { if (s.length() > n) s = s.substring(0, n); return "0".repeat(Math.max(0, n - s.length())) + s; }

    public static final class KeySlot {
        private String id = "EMPTY"; private byte[] secret = new byte[0]; private long expiresAt;
        KeySlot() { }
        KeySlot(String id, byte[] secret, long expiresAt) { this.id = id == null ? "KEY" : id.substring(0, Math.min(16, id.length())); this.secret = secret; this.expiresAt = expiresAt; }
        public String id() { return id; } public byte[] secret() { return secret; }
        public long expiresAt() { return expiresAt; }
        public boolean valid(long now) { return secret.length >= 16 && expiresAt > now; }
        void zeroize() { Arrays.fill(secret, (byte) 0); secret = new byte[0]; id = "ZEROIZED"; expiresAt = 0; }
        CompoundTag save() { CompoundTag t = new CompoundTag(); t.putString("Id", id); t.putByteArray("Secret", secret); t.putLong("Expires", expiresAt); return t; }
        static KeySlot load(CompoundTag t) { return new KeySlot(t.getString("Id"), t.getByteArray("Secret"), t.getLong("Expires")); }
    }
}
