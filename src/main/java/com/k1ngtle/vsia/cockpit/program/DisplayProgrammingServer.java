package com.k1ngtle.vsia.cockpit.program;

import com.k1ngtle.vsia.cockpit.network.*;
import com.k1ngtle.vsia.item.*;
import com.k1ngtle.vsia.network.VsiaNetwork;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="vsia")
public final class DisplayProgrammingServer {
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Semaphore JOBS=new Semaphore(2);
    private static final ExecutorService WORKERS=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"VSIA display compiler");t.setDaemon(true);return t;});
    private static final class Session {
        final UUID token=UUID.randomUUID(),drive;final InteractionHand hand;final ItemStack physical;final long expires=System.nanoTime()+TimeUnit.MINUTES.toNanos(5);long lastAction;boolean busy;
        Session(UUID drive,InteractionHand hand,ItemStack physical){this.drive=drive;this.hand=hand;this.physical=physical;}
    }
    private DisplayProgrammingServer(){}
    public static boolean matches(UUID expectedDrive,long expectedRevision,UUID actualDrive,long actualRevision){return expectedDrive!=null&&expectedDrive.equals(actualDrive)&&expectedRevision==actualRevision;}
    private static ItemStack held(ServerPlayer player,Session s){
        ItemStack stack=player.getItemInHand(s.hand);InteractionHand other=s.hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        return stack==s.physical&&!player.isSpectator()&&System.nanoTime()<s.expires&&player.getItemInHand(other).getItem() instanceof DisplayLaptopItem&&s.drive.equals(DisplayHardDriveItem.driveId(stack))&&stack.getCount()==1&&DisplayHardDriveItem.isDisplayDrive(stack)?stack:ItemStack.EMPTY;
    }
    public static void open(ServerPlayer player,InteractionHand hand){
        ItemStack drive=player.getItemInHand(hand);if(!DisplayHardDriveItem.isDisplayDrive(drive)||drive.getCount()!=1||player.isSpectator())return;
        SESSIONS.entrySet().removeIf(e->System.nanoTime()>=e.getValue().expires);
        Session s=new Session(DisplayHardDriveItem.ensureDriveId(drive),hand,drive);SESSIONS.put(player.getUUID(),s);player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();snapshot(player,s,true,"Ready. Designer needs no service; Compile + write uses the isolated service.");
    }
    private static String limit(String s,int max){return s==null?"":s.substring(0,Math.min(max,s.length()));}
    private static void snapshot(ServerPlayer player,Session s,boolean open,String status){
        ItemStack drive=held(player,s);if(drive.isEmpty())return;String lang=DisplayHardDriveItem.language(drive);try{DisplayCodeLanguage.valueOf(lang);}catch(Exception e){lang="PYTHON";}
        VsiaNetwork.sendToPlayer(player,new S2CDisplayProgramPacket(s.token,s.drive,DisplayHardDriveItem.revision(drive),s.hand,open,!DisplayHardDriveItem.writable(drive),limit(DisplayHardDriveItem.programName(drive),48),lang,limit(DisplayHardDriveItem.layout(drive),24576),limit(DisplayHardDriveItem.source(drive),24576),limit(status,2048)));
    }
    public static void action(ServerPlayer player,C2SDisplayProgramPacket p){
        Session s=SESSIONS.get(player.getUUID());if(s==null||!s.token.equals(p.session())||!s.drive.equals(p.driveId()))return;
        ItemStack drive=held(player,s);if(drive.isEmpty())return;
        if(!matches(p.driveId(),p.revision(),DisplayHardDriveItem.driveId(drive),DisplayHardDriveItem.revision(drive))){snapshot(player,s,false,"STALE: drive changed; close and reopen before writing.");return;}
        if(!DisplayHardDriveItem.writable(drive)){snapshot(player,s,false,"Factory drive is read-only. Use a writable Display Hard Drive.");return;}
        long now=System.nanoTime();if(s.busy||now-s.lastAction<TimeUnit.MILLISECONDS.toNanos(400)){snapshot(player,s,false,s.busy?"BUSY: waiting for compiler":"Wait briefly before another write");return;}s.lastAction=now;
        try {
            DisplayCodeLanguage.valueOf(p.language());if(p.name().chars().anyMatch(c->c<32||c==167))throw new IllegalArgumentException("Invalid program name");
            if(p.action()==1){
                if(!JOBS.tryAcquire()){snapshot(player,s,false,"Compiler busy: try again shortly");return;}s.busy=true;snapshot(player,s,false,"BUSY: compiling in isolated service...");
                var server=player.getServer();UUID owner=player.getUUID();long revision=p.revision();
                WORKERS.execute(()->{
                    DisplayDesign result=null;String error=null;try{result=DisplayCompilerClient.compile(p.language(),p.source());}catch(Exception e){error=limit(e.getMessage(),1800);}finally{JOBS.release();}
                    DisplayDesign compiled=result;String failure=error;
                    if(server!=null&&server.isRunning())server.execute(()->{s.busy=false;ServerPlayer live=server.getPlayerList().getPlayer(owner);if(live==null||SESSIONS.get(owner)!=s)return;ItemStack current=held(live,s);if(current.isEmpty())return;
                        if(!matches(s.drive,revision,DisplayHardDriveItem.driveId(current),DisplayHardDriveItem.revision(current))){snapshot(live,s,false,"STALE: compile result discarded; drive changed");return;}
                        if(compiled==null){snapshot(live,s,false,"ERROR: "+failure);return;}
                        DisplayHardDriveItem.writeDesign(current,p.name(),compiled.json(),p.language(),p.source());live.getInventory().setChanged();live.inventoryMenu.broadcastChanges();snapshot(live,s,false,"OK: real "+p.language()+" execution completed; design written to this drive");
                    });
                });return;
            }
            switch(p.action()) {
                case 0 -> DisplayHardDriveItem.writeDesign(drive,p.name(),p.layout(),p.language(),p.source());
                case 2 -> {DisplayHardDriveItem.clearProgram(drive);DisplayHardDriveItem.setProgramId(drive,DisplayHardDriveItem.PROGRAM_F35_CREATE);}
                case 3 -> DisplayHardDriveItem.clearProgram(drive);
                default -> throw new IllegalArgumentException("Unknown editor action");
            }
            player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();snapshot(player,s,false,"OK: drive updated");
        }catch(Exception e){snapshot(player,s,false,"ERROR: "+limit(e.getMessage(),1800));}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SESSIONS.clear();}
}
