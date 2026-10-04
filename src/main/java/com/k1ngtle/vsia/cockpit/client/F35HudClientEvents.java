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
        if(event.phase!=TickEvent.Phase.END||frame==null)return;
        Minecraft mc=Minecraft.getInstance();
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
        if(!eligible(mc,cockpit)||f==null||f.level()!=mc.level||f.connection()!=mc.getConnection()||!f.cockpit().equals(cockpit.cockpitId())||System.nanoTime()-f.atNanos()>250_000_000L)return;
        F35DisplayClientConfig.bind(cockpit);F35TargetLockClient.bind(cockpit.cockpitId());
        var contacts=F35HudContacts.select(f.state(),cockpit.displaySettings(),F35TargetLockClient.kind(),F35TargetLockClient.targetId());
        int color=F35DisplayClientConfig.applyBrightness(F35HologramPainter.GREEN);
        for(var contact:contacts){
            var point=F35HudProjection.project(contact.position(),f.camera(),f.view(),f.projection(),width,height);
            if(point==null)continue;
            // Dark edge first, then an opaque two-GUI-pixel green stroke. This keeps
            // the symbol readable over bright skies without an opaque target panel.
            float radius=contact.locked()?14:10;
            F35ContactSymbols.draw((a,b,c,d,tint)->stroke(graphics,a,b,c,d,5,0xe0001800,width,height),point.x(),point.y(),radius,contact.friendly(),contact.locked(),color,color);
            F35ContactSymbols.draw((a,b,c,d,tint)->stroke(graphics,a,b,c,d,2,tint,width,height),point.x(),point.y(),radius,contact.friendly(),contact.locked(),color,color);
            if(F35DisplayClientConfig.trackLabels()||contact.locked()){
                String label=(contact.locked()?"LOCK ":"")+contact.label()+" "+(contact.friendly()?"FRIEND":"UNK")+" "+Math.round(contact.position().distanceTo(f.state().ownship().position()))+"M";
                int x=Math.max(0,Math.min(width-mc.font.width(label),(int)point.x()+26)),y=Math.max(0,Math.min(height-9,(int)point.y()-4));
                graphics.drawString(mc.font,label,x,y,color,true);
            }
        }
    }
    private static void stroke(GuiGraphics g,float a,float b,float c,float d,int thickness,int color,int width,int height){
        int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(c-a),Math.abs(d-b))));
        for(int i=0;i<=steps;i++){
            float t=i/(float)steps;int x=Math.round(a+(c-a)*t)-thickness/2,y=Math.round(b+(d-b)*t)-thickness/2;
            int left=Math.max(0,x),top=Math.max(0,y),right=Math.min(width,x+thickness),bottom=Math.min(height,y+thickness);
            if(right>left&&bottom>top)g.fill(left,top,right,bottom,color);
        }
    }
    @Mod.EventBusSubscriber(modid=Vsia.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterGuiOverlaysEvent event){event.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(),"f35_sensor_cues",(gui,graphics,partial,width,height)->render(graphics,width,height));}
    }
}
