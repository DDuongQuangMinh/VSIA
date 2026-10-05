package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.display.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Helmet-only, local pilot selection. Does not authenticate IFF, send fire commands or enumerate entities. */
public final class F35GazeLockClient {
    private record Context(UUID cockpit,Object level,Object connection){ }
    private static Context context;
    private static final F35GazeLockTracker dwell=new F35GazeLockTracker();
    private static F35TargetLockClient.LockKind priorKind;
    private static UUID priorId;
    private F35GazeLockClient(){ }
    public static void reset(){context=null;dwell.reset();priorKind=null;priorId=null;}
    public static boolean update(UUID cockpit,Object level,Object connection,List<F35HudContacts.Contact> contacts,Vec3 camera,Matrix4f view,Matrix4f projection,int width,int height,long now){
        if(cockpit==null||level==null||connection==null){reset();return false;}
        Context next=new Context(cockpit,level,connection);
        if(!next.equals(context)){reset();context=next;}
        var kind=F35TargetLockClient.kind();UUID id=F35TargetLockClient.targetId();
        if(kind!=priorKind||!Objects.equals(id,priorId))dwell.reset();
        priorKind=kind;priorId=id;
        Minecraft mc=Minecraft.getInstance();float factor=1/F35HudStroke.guiPixelScale(mc.getWindow().getGuiScale());
        int pixelsWide=Math.round(width*factor),pixelsHigh=Math.round(height*factor);
        List<F35GazeLockTracker.Candidate> candidates=new ArrayList<>();
        for(var c:contacts){var p=F35HudProjection.project(c.position(),camera,view,projection,width,height);if(p!=null)candidates.add(new F35GazeLockTracker.Candidate(c.id(),c.radar(),p.x()*factor,p.y()*factor,c.position().distanceTo(camera)));}
        var target=F35GazeLockTracker.pick(candidates,pixelsWide,pixelsHigh);
        if(target!=null&&target.id().equals(id)&&kind==(target.radar()?F35TargetLockClient.LockKind.RADAR:F35TargetLockClient.LockKind.DETECTION)){dwell.reset();return false;}
        if(!dwell.update(target,now))return false;
        if(target.radar())F35TargetLockClient.lockRadar(target.id());else F35TargetLockClient.lockDetection(target.id());
        priorKind=F35TargetLockClient.kind();priorId=F35TargetLockClient.targetId();return true;
    }
    /** Called in the sensor overlay's existing inverse-GUI-scale pose. */
    public static void progress(GuiGraphics graphics,int pixelWidth,int pixelHeight,int color,long now){
        if(dwell.target()==null)return;
        Minecraft mc=Minecraft.getInstance();String text=String.format(Locale.ROOT,"GAZE %.1f / 4.0S",dwell.progress(now)*4);
        graphics.pose().pushPose();
        try{graphics.pose().translate(pixelWidth/2f-mc.font.width(text)*.75f,pixelHeight/2f+30,0);graphics.pose().scale(1.5f,1.5f,1);graphics.drawString(mc.font,text,0,0,color,true);}
        finally{graphics.pose().popPose();}
    }
}
