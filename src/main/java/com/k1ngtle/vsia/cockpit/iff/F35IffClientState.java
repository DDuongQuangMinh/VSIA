package com.k1ngtle.vsia.cockpit.iff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class F35IffClientState {
    private static final Map<UUID, Snapshot> VALUES = new HashMap<>();
    private F35IffClientState() { }
    public static Snapshot get(UUID cockpitId) { return VALUES.getOrDefault(cockpitId, Snapshot.empty()); }
    public static void accept(UUID cockpitId, Snapshot snapshot) { VALUES.put(cockpitId, snapshot); }
    public static void clearAll() { VALUES.clear(); }
    public record Snapshot(String master, String mode1, String mode2, String mode3a, int enabledMask,
                           int activeSlot, String keyId, long expiresAt, int telemetryMask, String status, String missionId) {
        public Snapshot(String master,String mode1,String mode2,String mode3a,int enabledMask,int activeSlot,String keyId,long expiresAt,int telemetryMask,String status){this(master,mode1,mode2,mode3a,enabledMask,activeSlot,keyId,expiresAt,telemetryMask,status,"");}
        public static Snapshot empty() { return new Snapshot("STBY", "00", "0000", "1200", 31, 0, "EMPTY", 0, 15, "WAIT"); }
        public boolean enabled(int mode) { return (enabledMask & (1 << (mode - 1))) != 0; }
        public boolean telemetry(int field) { return (telemetryMask & (1 << field)) != 0; }
    }
}
