package com.k1ngtle.vsia.cockpit.network;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.*;
import com.k1ngtle.vsia.cockpit.detection.*;
import com.k1ngtle.vsia.cockpit.iff.F35IffService;
import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.signality.radar.iff.IffResult;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkApi;
import com.k1ngtle.vsia.signality.radar.network.RadarNetworkTrack;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class F35RadarSyncEvents {
    private static final String DISPLAY_NETWORK="default";
    private static final Map<UUID,F35RadarContactProjection.Memory> MEMORIES=new HashMap<>();
    private static final Map<UUID,Long> LAST_USED=new HashMap<>();
    private F35RadarSyncEvents() { }
    @SubscribeEvent public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%5!=0)return;
        F35CockpitSeatBlockEntity cockpit=F35SeatController.cockpitFor(player);
        if(cockpit==null)return;
        long tick=player.serverLevel().getGameTime(),now=System.currentTimeMillis();
        LAST_USED.entrySet().removeIf(e->{if(now-e.getValue()>30_000L){MEMORIES.remove(e.getKey());return true;}return false;});
        LAST_USED.put(cockpit.cockpitId(),now);
        var own=F35VsShipHelper.shipSnapshot(cockpit);
        F35LiveContactCatalog catalog=own.detected()?F35LiveContactCatalog.capture(player.serverLevel(),player):new F35LiveContactCatalog(Map.of());
        F35DetectionFilter filter=cockpit.displaySettings().detectionFilter();
        Map<UUID,IffResult> proofs=new HashMap<>();
        java.util.function.Function<UUID,IffResult> authenticate=id->proofs.computeIfAbsent(id,key->F35IffService.interrogate(cockpit,key));
        List<RadarNetworkTrack> projected=own.detected()?F35RadarContactProjection.project(
                RadarNetworkApi.tracks(player.serverLevel(),DISPLAY_NETWORK),catalog,filter,own.shipId(),own.worldCenter(),tick):List.of();
        List<RadarNetworkTrack> tracks=projected.stream().map(t->t.withIff(authenticate.apply(t.sourceTargetId()))).toList();
        var scan=F35DetectionScanner.scan(cockpit,filter,catalog);
        List<F35DetectionContact> contacts=scan.contacts().stream().map(c->c.withIff(authenticate.apply(c.contactId()))).toList();
        Map<UUID,UUID> sources=new HashMap<>();tracks.forEach(t->sources.put(t.trackId(),t.sourceTargetId()));
        var removals=MEMORIES.computeIfAbsent(cockpit.cockpitId(),id->new F35RadarContactProjection.Memory()).update(sources,contacts,catalog,own.shipId(),tick);
        VsiaNetwork.sendToPlayer(player,new S2CF35RadarSnapshotPacket(cockpit.cockpitId(),DISPLAY_NETWORK,tick,tracks,removals));
        VsiaNetwork.sendToPlayer(player,new S2CF35DetectionSnapshotPacket(cockpit.cockpitId(),tick,contacts,scan.silhouette()));
        if(player.tickCount%20==0)F35IffPackets.sendSnapshot(player,cockpit,java.time.Instant.now().getEpochSecond());
    }
    @SubscribeEvent public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event){F35DetectionPreferences.clear(event.getEntity().getUUID());}
    @SubscribeEvent public static void onServerStopped(ServerStoppedEvent event){MEMORIES.clear();LAST_USED.clear();}
    @SubscribeEvent public static void onCommands(net.minecraftforge.event.RegisterCommandsEvent event){
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("f35hull")
                .then(net.minecraft.commands.Commands.literal("inspect").executes(ctx->{
                    ServerPlayer player=ctx.getSource().getPlayerOrException();
                    var hit=player.pick(16.0,0.0F,false);
                    if(!(hit instanceof net.minecraft.world.phys.BlockHitResult block)){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Look at a hull block within 16 blocks."));return 0;}
                    String info=F35BlockHealthEvents.inspect(player.serverLevel(),block.getBlockPos());
                    ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(info),false);return 1;
                }))
                .then(net.minecraft.commands.Commands.literal("baseline")
                .then(net.minecraft.commands.Commands.literal("confirm").executes(ctx->{
                    ServerPlayer player=ctx.getSource().getPlayerOrException();
                    var cockpit=F35SeatController.cockpitFor(player);
                    if(cockpit==null){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Sit in the cockpit whose reference hull you want to record."));return 0;}
                    var plan=F35ShipSilhouetteScanner.scan(cockpit,true);
                    if(!plan.damage().known()){ctx.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Reference not changed: "+plan.damage().status()));return 0;}
                    ctx.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("This cockpit's reference hull recorded: "+plan.damage().baselineBlocks()+" blocks. Existing losses are now the reference; this does not repair the ship."),false);
                    return 1;
                }))));
    }
}
