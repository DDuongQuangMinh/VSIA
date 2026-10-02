package com.k1ngtle.vsia.cockpit.iff;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Server-only persistent key vault. IDs are public; authorization controls provisioning. */
public final class F35IffMissions extends SavedData {
    public static final long KEY_LIFETIME = 21600, ROTATION_GRACE = 120;
    private final Map<UUID, Mission> missions = new HashMap<>();
    public static F35IffMissions get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(F35IffMissions::load, F35IffMissions::new, "vsia_f35_iff_missions");
    }
    public Mission create(UUID owner, String name, long now) {
        if (missions.size() >= 512 || missions.values().stream().filter(m -> m.owner.equals(owner)).count() >= 16)
            throw new IllegalArgumentException("Mission limit reached");
        Mission m = new Mission(UUID.randomUUID(), owner, clean(name)); m.members.add(owner);
        m.current = new Generation(1, randomKey(), now + KEY_LIFETIME); missions.put(m.id, m); setDirty(); return m;
    }
    public Mission authorized(UUID mission, UUID player) {
        Mission m = missions.get(mission);
        if (m == null || !m.members.contains(player)) throw new IllegalArgumentException("No invitation/access for this mission");
        return m;
    }
    public Mission owned(UUID mission, UUID player) {
        Mission m = authorized(mission, player);
        if (!m.owner.equals(player)) throw new IllegalArgumentException("Only the mission owner may invite, rotate or revoke");
        return m;
    }
    public void invite(UUID mission, UUID owner, UUID recipient) { owned(mission, owner).members.add(recipient); setDirty(); }
    public void revoke(UUID mission, UUID owner, UUID recipient) {
        Mission m = owned(mission, owner); if (m.owner.equals(recipient)) throw new IllegalArgumentException("Cannot revoke mission owner");
        m.members.remove(recipient); setDirty();
    }
    public void rotate(UUID mission, UUID owner, long now) {
        Mission m = owned(mission, owner); if(m.current.number >= 999999) throw new IllegalArgumentException("Create a new mission");
        if(m.previous != null) m.previous.wipe();
        m.previous = m.current; m.previous.expires = Math.min(m.previous.expires, now + ROTATION_GRACE);
        m.current = new Generation(m.previous.number + 1, randomKey(), now + KEY_LIFETIME); setDirty();
    }
    public void retire(UUID mission, UUID owner) { Mission m=owned(mission,owner);m.current.wipe();if(m.previous!=null)m.previous.wipe();missions.remove(mission);setDirty(); }
    public void provision(UUID mission, UUID player, F35IffConfig config, int slot, long now) {
        Mission m = authorized(mission, player);
        if(m.current.expires <= now)throw new IllegalArgumentException("Mission expired; owner must rotate");
        config.loadMissionKey(slot, keyId(m.id, m.current.number), m.current.secret, m.current.expires, m.id, m.current.number, player);
    }
    public boolean usable(F35IffConfig.KeySlot k, long now) {
        Mission m = missions.get(k.mission());
        if (!k.valid(now) || m == null || !m.members.contains(k.recipient())) return false;
        Generation g = m.current.number == k.generation() ? m.current : m.previous;
        return g != null && g.number == k.generation() && g.expires > now && keyId(m.id,g.number).equals(k.id());
    }
    public long effectiveExpiry(F35IffConfig.KeySlot k) {
        Mission m = missions.get(k.mission()); if(m == null)return 0;
        Generation g = m.current.number == k.generation() ? m.current : m.previous;
        return g != null && g.number == k.generation() ? Math.min(k.expiresAt(),g.expires) : 0;
    }
    public java.util.List<Mission> list(UUID player) { return missions.values().stream().filter(m -> m.members.contains(player)).toList(); }
    public static String keyId(UUID mission, int generation) { return mission.toString().substring(0,8).toUpperCase() + "-" + generation; }
    private static byte[] randomKey() { byte[] secret = new byte[32]; new SecureRandom().nextBytes(secret); return secret; }
    private static String clean(String s) { String v = s.replaceAll("[^A-Za-z0-9_-]", ""); return v.isBlank()?"MISSION":v.substring(0,Math.min(16,v.length())); }
    public static final class Mission {
        public final UUID id, owner; public final String name; private final Set<UUID> members = new HashSet<>();
        private Generation current, previous;
        Mission(UUID id, UUID owner, String name) { this.id=id; this.owner=owner; this.name=name; }
    }
    private static final class Generation {
        final int number; final byte[] secret; long expires;
        Generation(int n, byte[] secret, long expires) { this.number=n; this.secret=secret; this.expires=expires; }
        void wipe() { Arrays.fill(secret,(byte)0); expires=0; }
        CompoundTag save() { CompoundTag t=new CompoundTag();t.putInt("Number",number);t.putByteArray("Secret",secret);t.putLong("Expires",expires);return t; }
        static Generation load(CompoundTag t) { return new Generation(t.getInt("Number"),t.getByteArray("Secret"),t.getLong("Expires")); }
    }
    @Override public CompoundTag save(CompoundTag root) {
        ListTag list=new ListTag(); for(Mission m:missions.values()) {
            CompoundTag t=new CompoundTag();t.putUUID("Id",m.id);t.putUUID("Owner",m.owner);t.putString("Name",m.name);
            ListTag members=new ListTag(); for(UUID id:m.members){CompoundTag v=new CompoundTag();v.putUUID("Id",id);members.add(v);} t.put("Members",members);
            t.put("Current",m.current.save());if(m.previous!=null)t.put("Previous",m.previous.save());list.add(t);
        } root.put("Missions",list);return root;
    }
    public static F35IffMissions load(CompoundTag root) {
        F35IffMissions vault=new F35IffMissions();ListTag list=root.getList("Missions",10);
        for(int i=0;i<Math.min(512,list.size());i++){CompoundTag t=list.getCompound(i);if(!t.hasUUID("Id")||!t.hasUUID("Owner"))continue;
            Mission m=new Mission(t.getUUID("Id"),t.getUUID("Owner"),clean(t.getString("Name")));ListTag members=t.getList("Members",10);
            for(int j=0;j<members.size();j++)if(members.getCompound(j).hasUUID("Id"))m.members.add(members.getCompound(j).getUUID("Id"));
            m.members.add(m.owner);m.current=Generation.load(t.getCompound("Current"));if(t.contains("Previous"))m.previous=Generation.load(t.getCompound("Previous"));vault.missions.put(m.id,m);
        }return vault;
    }
}
