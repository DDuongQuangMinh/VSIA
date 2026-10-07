package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.Vsia;
import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.item.DisplayHardDriveItem;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Camera-projected radar cues, only for the seated first-person pilot and current rendered frame. */
@Mod.EventBusSubscriber(modid=Vsia.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class F35HudClientEvents {
    private record Frame(UUID cockpit,Object level,Object connection,Matrix4f view,Matrix4f projection,Vec3 camera,F35DisplayState state,long atNanos) { }
    private static Frame frame;
    private F35HudClientEvents() { }
    public static boolean installed(F35CockpitSeatBlockEntity cockpit){
        if(cockpit==null||!cockpit.hasDisplayDrive())return false;
        String program=DisplayHardDriveItem.programId(cockpit.displayDrive());
        return DisplayHardDriveItem.PROGRAM_F35_CREATE.equals(program)||DisplayHardDriveItem.PROGRAM_CUSTOM.equals(program);
    }
    private static boolean eligible(Minecraft mc,F35CockpitSeatBlockEntity cockpit){return mc.player!=null&&mc.level!=null&&mc.getConnection()!=null&&F35HudContacts.eligible(cockpit!=null,mc.options.getCameraType().isFirstPerson(),mc.screen!=null,mc.options.hideGui,installed(cockpit));}
    @SubscribeEvent public static void lifecycle(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        Minecraft mc=Minecraft.getInstance();
        if(!F35HelmetHudClient.visible(mc)||!mc.isWindowActive()||!eligible(mc,F35CockpitClientContext.seated()))F35GazeLockClient.reset();
        if(frame==null)return;
        if(frame.level()!=mc.level||frame.connection()!=mc.getConnection()||!eligible(mc,F35CockpitClientContext.seated()))frame=null;
    }
    @SubscribeEvent public static void capture(RenderLevelStageEvent event){
        // Forge 1.20.1 AFTER_LEVEL supplies GameRenderer's projection-only PoseStack,
        // NOT the LevelRenderer camera view. Multiplying that by projection again
        // puts valid forward contacts behind the camera. AFTER_ENTITIES carries the
        // actual world view, including VS mounted-camera rotation and camera effects.
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        Minecraft mc=Minecraft.getInstance();F35CockpitSeatBlockEntity cockpit=F35CockpitClientContext.seated();
        frame=null;
        if(!eligible(mc,cockpit)||cockpit.cockpitId()==null)return;
        F35DisplayState state=F35DisplayStateFactory.capture(cockpit,event.getPartialTick());
        frame=new Frame(cockpit.cockpitId(),mc.level,mc.getConnection(),new Matrix4f(event.getPoseStack().last().pose()),new Matrix4f(event.getProjectionMatrix()),event.getCamera().getPosition(),state,System.nanoTime());
    }
    private static void render(GuiGraphics graphics,int width,int height){
        Minecraft mc=Minecraft.getInstance();F35CockpitSeatBlockEntity cockpit=F35CockpitClientContext.seated();Frame f=frame;
        boolean helmet=F35HelmetHudClient.visible(mc);
        if(!helmet||!mc.isWindowActive()||!eligible(mc,cockpit)||f==null||f.level()!=mc.level||f.connection()!=mc.getConnection()||!f.cockpit().equals(cockpit.cockpitId())||System.nanoTime()-f.atNanos()>250_000_000L)F35GazeLockClient.reset();
        if(helmet&&cockpit==null){F35HelmetHudClient.status(graphics,width,height,"HELMET / NO COCKPIT LINK");return;}
        if(helmet&&!installed(cockpit)){F35HelmetHudClient.status(graphics,width,height,"HELMET / NO DISPLAY DRIVE");return;}
        if(helmet&&(f==null||f.level()!=mc.level||f.connection()!=mc.getConnection()||!f.cockpit().equals(cockpit.cockpitId())||System.nanoTime()-f.atNanos()>250_000_000L)){F35HelmetHudClient.status(graphics,width,height,"HELMET / WAITING FOR COCKPIT");return;}
        if(!eligible(mc,cockpit)||f==null||f.level()!=mc.level||f.connection()!=mc.getConnection()||!f.cockpit().equals(cockpit.cockpitId())||System.nanoTime()-f.atNanos()>250_000_000L)return;
        F35DisplayClientConfig.bind(cockpit);F35TargetLockClient.bind(cockpit.cockpitId());
        var contacts=F35HudContacts.select(f.state(),cockpit.displaySettings(),F35TargetLockClient.kind(),F35TargetLockClient.targetId());
        if(helmet&&mc.isWindowActive()&&F35GazeLockClient.update(cockpit.cockpitId(),mc.level,mc.getConnection(),contacts,f.camera(),f.view(),f.projection(),width,height,System.nanoTime()))contacts=F35HudContacts.select(f.state(),cockpit.displaySettings(),F35TargetLockClient.kind(),F35TargetLockClient.targetId());
        if(helmet)F35HelmetHudClient.flight(graphics,width,height,f.state(),contacts,F35TargetLockClient.hasLock(),F35HudContacts.hasLiveLock(f.state(),F35TargetLockClient.kind(),F35TargetLockClient.targetId()));
        int color=F35DisplayClientConfig.applyBrightness(F35HologramPainter.GREEN);
        // Undo GUI magnification only for these sensor cues. Projection stays in
        // GUI coordinates; converting both the point and pose preserves alignment.
        float pixelScale=F35HudStroke.guiPixelScale(mc.getWindow().getGuiScale()),pixelsPerGui=1/pixelScale;
        int pixelWidth=Math.max(1,Math.round(width*pixelsPerGui)),pixelHeight=Math.max(1,Math.round(height*pixelsPerGui));
        graphics.pose().pushPose();
        try{
            graphics.pose().scale(pixelScale,pixelScale,1);
            try(var batch=new F35HudBatch(graphics,mc.font,pixelWidth,pixelHeight)){
            if(helmet){
                var velocityWorld=F35HelmetFlightPainter.velocityPoint(f.state().ownship(),f.camera());
                var velocityCue=F35HudProjection.project(velocityWorld,f.camera(),f.view(),f.projection(),width,height);
                if(velocityCue!=null)F35HelmetFlightPainter.flightPath((a,b,c,d,tint)->batch.line(a,b,c,d,1,tint),velocityCue.x()*pixelsPerGui,velocityCue.y()*pixelsPerGui,color);
            }
            for(var contact:contacts){
                var point=F35HudProjection.project(contact.position(),f.camera(),f.view(),f.projection(),width,height);
                if(point==null)continue;
                float x=point.x()*pixelsPerGui,y=point.y()*pixelsPerGui,radius=contact.locked()?14:10;
                // One actual screen-pixel green stroke with a narrow contrast edge,
                // not two/five GUI pixels enlarged by the user's GUI scale.
                F35ContactSymbols.draw((a,b,c,d,tint)->batch.line(a,b,c,d,3,0xc0001800),x,y,radius,contact.friendly(),contact.locked(),color,color);
                F35ContactSymbols.draw((a,b,c,d,tint)->batch.line(a,b,c,d,1,tint),x,y,radius,contact.friendly(),contact.locked(),color,color);
                if(F35DisplayClientConfig.trackLabels()||contact.locked()){
                    String label=(contact.locked()?"LOCK ":"")+contact.label()+" "+(contact.friendly()?"FRIEND":contact.hostile()?"ENEMY":"UNK")+" "+Math.round(contact.position().distanceTo(f.state().ownship().position()))+"M";
                    // Visor cue is short and split over two rows, like the reference.
                    String range=helmet?F35HelmetFlightPainter.rangeLabel(contact.position().distanceTo(f.state().ownship().position())):"";
                    if(helmet)label=(contact.locked()?"L ":"")+contact.label()+" "+(contact.friendly()?"FRIEND":contact.hostile()?"ENEMY":"UNK");
                    float fontSize=helmet?1.25f:1.5f;
                    int labelWidth=(int)Math.ceil(mc.font.width(label)*fontSize);
                    if(helmet)labelWidth=Math.max(labelWidth,(int)Math.ceil(mc.font.width(range)*fontSize));
                    int labelX=Math.max(0,Math.min(pixelWidth-labelWidth,(int)x+(helmet?23:28))),labelY=Math.max(0,Math.min(pixelHeight-(helmet?26:14),(int)y-7));
                    batch.text(label,labelX,labelY,fontSize,0,color,!helmet);
                    if(helmet)batch.text(range,labelX,labelY+10*fontSize,fontSize,0,color,false);
                }
            }
            }
            if(helmet&&mc.isWindowActive())F35GazeLockClient.progress(graphics,pixelWidth,pixelHeight,color,System.nanoTime());
        }finally{graphics.pose().popPose();}
    }
    @Mod.EventBusSubscriber(modid=Vsia.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event){event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(),"f35_sensor_cues",(gui,graphics,partial,width,height)->render(graphics,width,height));}
    }
}
