package com.k1ngtle.vsia.cockpit.display;

import java.util.List;

public final class F35ClientRadarCache {
    private static final java.util.Map<java.util.UUID,Snapshot> VALUES=new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID,Long> RECEIVED=new java.util.HashMap<>();
    public static Snapshot snapshot(java.util.UUID cockpitId){return System.currentTimeMillis()-RECEIVED.getOrDefault(cockpitId,0L)>1500?new Snapshot("default",0,List.of()):VALUES.getOrDefault(cockpitId,new Snapshot("default",0,List.of()));}
    public static void clearAll(){VALUES.clear();RECEIVED.clear();snapshot=new Snapshot("default",0,List.of());}
    public static void accept(java.util.UUID cockpitId,String networkId,long serverTick,List<F35RadarTrackView> tracks){acceptSnapshot(cockpitId,networkId,serverTick,tracks);}
    public static boolean acceptSnapshot(java.util.UUID cockpitId,String networkId,long serverTick,List<F35RadarTrackView> tracks){
        if(cockpitId==null||cockpitId.equals(new java.util.UUID(0L,0L)))return false;
        Snapshot old=VALUES.get(cockpitId);if(old!=null&&serverTick<old.serverTick())return false;
        VALUES.put(cockpitId,new Snapshot(networkId,serverTick,tracks));RECEIVED.put(cockpitId,System.currentTimeMillis());return true;
    }
    private static volatile Snapshot snapshot =
            new Snapshot(
                    "default",
                    0L,
                    List.of()
            );

    private F35ClientRadarCache() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void accept(
            String networkId,
            long serverTick,
            List<F35RadarTrackView> tracks
    ) {
        snapshot =
                new Snapshot(
                        networkId,
                        serverTick,
                        List.copyOf(
                                tracks
                        )
                );
    }

    public record Snapshot(
            String networkId,
            long serverTick,
            List<F35RadarTrackView> tracks
    ) {
        public Snapshot {
            tracks =
                    List.copyOf(
                            tracks
                    );
        }
    }
}
