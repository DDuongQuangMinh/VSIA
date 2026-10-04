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
    private static final class Session {
        final UUID token=UUID.randomUUID(),drive;final InteractionHand hand;final ItemStack physical;final DisplaySourceUpload upload=new DisplaySourceUpload();final long expires=System.nanoTime()+TimeUnit.MINUTES.toNanos(5);long lastAction,lastUpload;
        final DisplayLaptopBlockEntity laptop;
        final UUID laptopId;
        Session(UUID drive,InteractionHand hand,ItemStack physical){this(drive,hand,physical,null);}
        Session(UUID drive,InteractionHand hand,ItemStack physical,DisplayLaptopBlockEntity laptop){this.drive=drive;this.hand=hand;this.physical=physical;this.laptop=laptop;this.laptopId=laptop==null?null:laptop.laptopId();}
    }
    private DisplayProgrammingServer(){}
    public static boolean matches(UUID expectedDrive,long expectedRevision,UUID actualDrive,long actualRevision){return expectedDrive!=null&&expectedDrive.equals(actualDrive)&&expectedRevision==actualRevision;}
    private static ItemStack held(ServerPlayer player,Session s){
        if(s.laptop!=null){
            // Never resolve a session to a replacement block or another laptop's inventory.
            if(System.nanoTime()>=s.expires||!s.laptopId.equals(s.laptop.laptopId())||!s.laptop.canUse(player))return ItemStack.EMPTY;
            ItemStack installed=s.laptop.displayDrive();
            return installed==s.physical&&s.drive.equals(DisplayHardDriveItem.driveId(installed))&&installed.getCount()==1&&DisplayHardDriveItem.isDisplayDrive(installed)?installed:ItemStack.EMPTY;
        }
        ItemStack stack=player.getItemInHand(s.hand);InteractionHand other=s.hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND;
        return stack==s.physical&&!player.isSpectator()&&System.nanoTime()<s.expires&&player.getItemInHand(other).getItem() instanceof DisplayLaptopItem&&s.drive.equals(DisplayHardDriveItem.driveId(stack))&&stack.getCount()==1&&DisplayHardDriveItem.isDisplayDrive(stack)?stack:ItemStack.EMPTY;
    }
    public static void open(ServerPlayer player,InteractionHand hand){
        ItemStack drive=player.getItemInHand(hand);if(!DisplayHardDriveItem.isDisplayDrive(drive)||drive.getCount()!=1||player.isSpectator())return;
        SESSIONS.entrySet().removeIf(e->System.nanoTime()>=e.getValue().expires);
        Session s=new Session(DisplayHardDriveItem.ensureDriveId(drive),hand,drive);SESSIONS.put(player.getUUID(),s);player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();snapshot(player,s,true,"Ready. Compile + write runs locally on your client; server needs no compiler service.");
    }
    public static void openPlaced(ServerPlayer player,DisplayLaptopBlockEntity laptop){
        if(!laptop.canUse(player)||!laptop.hasDrive())return;
        ItemStack drive=laptop.displayDrive();if(drive.getCount()!=1)return;
        SESSIONS.entrySet().removeIf(e->System.nanoTime()>=e.getValue().expires);
        Session s=new Session(DisplayHardDriveItem.ensureDriveId(drive),InteractionHand.MAIN_HAND,drive,laptop);
        SESSIONS.put(player.getUUID(),s);laptop.sync();
        snapshot(player,s,true,"Ready. Editing THIS placed laptop's inserted drive. Compile + write runs locally on your client.");
    }
    private static String limit(String s,int max){return s==null?"":s.substring(0,Math.min(max,s.length()));}
    private static void snapshot(ServerPlayer player,Session s,boolean open,String status){
        ItemStack drive=held(player,s);if(drive.isEmpty())return;String lang=DisplayHardDriveItem.language(drive);try{DisplayCodeLanguage.valueOf(lang);}catch(Exception e){lang="PYTHON";}
        VsiaNetwork.sendToPlayer(player,new S2CDisplayProgramPacket(s.token,s.drive,DisplayHardDriveItem.revision(drive),s.hand,open,!DisplayHardDriveItem.writable(drive),limit(DisplayHardDriveItem.programName(drive),48),lang,limit(DisplayHardDriveItem.layout(drive),24576),limit(DisplayHardDriveItem.source(drive),DisplayProgramLimits.SOURCE_CHARS),limit(status,2048),s.laptop==null?null:s.laptop.getBlockPos(),s.laptopId));
    }
    public static void chunk(ServerPlayer player,C2SDisplaySourceChunkPacket p){
        Session s=SESSIONS.get(player.getUUID());if(s==null||!s.token.equals(p.session())||!s.drive.equals(p.driveId()))return;
        ItemStack drive=held(player,s);if(drive.isEmpty()||!DisplayHardDriveItem.writable(drive)||DisplayHardDriveItem.revision(drive)!=p.revision())return;
        long now=System.currentTimeMillis();if(p.index()==0){if(now-s.lastUpload<400)return;s.lastUpload=now;}
        try{s.upload.append(p.upload(),p.index(),p.count(),p.part(),now);}catch(IllegalArgumentException e){s.upload.clear();}
    }
    public static void action(ServerPlayer player,C2SDisplayProgramPacket p){
        Session s=SESSIONS.get(player.getUUID());if(s==null||!s.token.equals(p.session())||!s.drive.equals(p.driveId()))return;
        ItemStack drive=held(player,s);if(drive.isEmpty())return;
        if(!matches(p.driveId(),p.revision(),DisplayHardDriveItem.driveId(drive),DisplayHardDriveItem.revision(drive))){snapshot(player,s,false,"STALE: drive changed; close and reopen before writing.");return;}
        if(!DisplayHardDriveItem.writable(drive)){snapshot(player,s,false,"Factory drive is read-only. Use a writable Display Hard Drive.");return;}
        long now=System.nanoTime();if(now-s.lastAction<TimeUnit.MILLISECONDS.toNanos(400)){snapshot(player,s,false,"Wait briefly before another write");return;}s.lastAction=now;
        try {
            DisplayCodeLanguage.valueOf(p.language());if(p.name().chars().anyMatch(c->c<32||c==167))throw new IllegalArgumentException("Invalid program name");
            String source=p.upload()==null?p.source():s.upload.take(p.upload(),System.currentTimeMillis());
            if(p.upload()!=null&&!p.source().isEmpty())throw new IllegalArgumentException("Ambiguous source upload");
            DisplayProgramLimits.validate(source);
            if(p.action()==1){
                throw new IllegalArgumentException("Server-side compilation retired. Update the client; Compile + write now runs locally.");
            }
            switch(p.action()) {
                case DisplayProgramSubmission.DESIGNER,DisplayProgramSubmission.CLIENT_COMPILED -> {var submission=DisplayProgramSubmission.validate(p.action(),p.name(),p.language(),p.layout(),source);DisplayHardDriveItem.writeDesign(drive,submission.name(),submission.design().json(),submission.language().name(),submission.source());}
                case 2 -> {DisplayHardDriveItem.clearProgram(drive);DisplayHardDriveItem.setProgramId(drive,DisplayHardDriveItem.PROGRAM_F35_CREATE);}
                case 3 -> DisplayHardDriveItem.clearProgram(drive);
                default -> throw new IllegalArgumentException("Unknown editor action");
            }
            if(s.laptop!=null)s.laptop.sync();else{player.getInventory().setChanged();player.inventoryMenu.broadcastChanges();}
            snapshot(player,s,false,p.action()==DisplayProgramSubmission.CLIENT_COMPILED?"OK: client-produced layout validated and saved to this drive":"OK: drive updated");
        }catch(Exception e){snapshot(player,s,false,"ERROR: "+limit(e.getMessage(),1800));}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SESSIONS.clear();}
}
