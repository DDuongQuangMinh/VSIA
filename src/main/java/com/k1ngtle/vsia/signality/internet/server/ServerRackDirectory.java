package com.k1ngtle.vsia.signality.internet.server;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ServerRackDirectory {
    private static final Map<ServerLevel, Map<BlockPos, ServerRackBlockEntity>> RACKS = new WeakHashMap<>();
    private ServerRackDirectory() {}

    public static synchronized void register(ServerRackBlockEntity rack) {
        if (rack.getLevel() instanceof ServerLevel level)
            RACKS.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>()).put(rack.getBlockPos(), rack);
    }
    public static synchronized void unregister(ServerRackBlockEntity rack) {
        if (rack.getLevel() instanceof ServerLevel level && RACKS.get(level) != null)
            RACKS.get(level).remove(rack.getBlockPos());
    }
    public static synchronized ServerRackBlockEntity byIp(ServerLevel level, String ip) {
        Map<BlockPos, ServerRackBlockEntity> racks=RACKS.get(level); if(racks==null)return null;
        return racks.values().stream().filter(r->r.ipAddress().equals(ip)).findFirst().orElse(null);
    }
    public static synchronized String resolve(ServerLevel level,String domain) {
        Map<BlockPos, ServerRackBlockEntity> racks=RACKS.get(level); if(racks==null)return null;
        for(ServerRackBlockEntity rack:racks.values()){String ip=rack.resolveDomain(domain);if(ip!=null)return ip;}return null;
    }
    public static synchronized ServerRackBlockEntity nearestPtpGrandmaster(ServerRackBlockEntity client) {
        if (!(client.getLevel() instanceof ServerLevel level)) return null;
        Map<BlockPos, ServerRackBlockEntity> racks=RACKS.get(level); if(racks==null)return null;
        ServerRackBlockEntity nearest=null; double best=Double.MAX_VALUE;
        for(ServerRackBlockEntity rack:racks.values()){
            if(rack==client||rack.ptpMode()!=ServerRackPtpMode.GRANDMASTER||rack.ptpProfile()!=client.ptpProfile())continue;
            double distance=client.getBlockPos().distSqr(rack.getBlockPos());
            double allowed=Math.min(client.effectiveMaximumRangeBlocks(),rack.effectiveMaximumRangeBlocks());
            if(distance<=allowed*allowed&&distance<best){nearest=rack;best=distance;}
        }
        return nearest;
    }
    public static synchronized ServerRackBlockEntity ntpSource(ServerRackBlockEntity client,String preferredIp){if(!(client.getLevel() instanceof ServerLevel level))return null;if(preferredIp!=null&&!preferredIp.isBlank()){ServerRackBlockEntity preferred=byIp(level,preferredIp);if(preferred!=null&&preferred.ntpServerEnabled())return preferred;}Map<BlockPos,ServerRackBlockEntity> racks=RACKS.get(level);if(racks==null)return null;return racks.values().stream().filter(r->r!=client&&r.ntpServerEnabled()).min(java.util.Comparator.comparingInt(ServerRackBlockEntity::ntpStratum).thenComparingDouble(r->r.getBlockPos().distSqr(client.getBlockPos()))).orElse(null);}
}
