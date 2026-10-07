package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.item.F35HelmetItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import com.mojang.math.Axis;

/** Head-following transparent flight instruments; sensors/lock remain cockpit-owned. */
public final class F35HelmetHudClient {
    private F35HelmetHudClient(){ }
    public static boolean visible(Minecraft mc){return mc.player!=null&&mc.level!=null&&F35HelmetHudRules.visible(mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof F35HelmetItem,mc.options.getCameraType().isFirstPerson(),mc.screen!=null,mc.options.hideGui);}
    public static boolean suppressPhysical(F35CockpitSeatBlockEntity cockpit){
        Minecraft mc=Minecraft.getInstance();var seated=F35CockpitClientContext.seated();
        return F35HelmetHudRules.suppressPhysical(visible(mc),cockpit!=null&&seated!=null&&cockpit.cockpitId()!=null&&cockpit.cockpitId().equals(seated.cockpitId()),F35HudClientEvents.installed(cockpit));
    }
    public static void status(GuiGraphics graphics,int width,int height,String message){
        Minecraft mc=Minecraft.getInstance();float pixelScale=F35HudStroke.guiPixelScale(mc.getWindow().getGuiScale()),pixelsPerGui=1/pixelScale;
        graphics.pose().pushPose();
        try{
            graphics.pose().scale(pixelScale,pixelScale,1);
            int x=Math.max(0,Math.round(width*pixelsPerGui/2-mc.font.width(message)*.75f)),y=Math.round(height*pixelsPerGui*.23f);
            graphics.pose().translate(x,y,0);graphics.pose().scale(1.5f,1.5f,1);
            graphics.drawString(mc.font,message,0,0,F35HologramPainter.GREEN,true);
        }finally{graphics.pose().popPose();}
    }
    public static void flight(GuiGraphics graphics,int width,int height,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock){
        Minecraft mc=Minecraft.getInstance();float pixelScale=F35HudStroke.guiPixelScale(mc.getWindow().getGuiScale()),pixelsPerGui=1/pixelScale;
        int pixelWidth=Math.max(1,Math.round(width*pixelsPerGui)),pixelHeight=Math.max(1,Math.round(height*pixelsPerGui));
        var layout=F35HelmetHudRules.layout(pixelWidth,pixelHeight);if(layout==null)return;
        graphics.pose().pushPose();
        try{
            graphics.pose().scale(pixelScale,pixelScale,1);
            F35HelmetFlightPainter.paint(new F35HelmetFlightPainter.Draw(){
                public float textWidth(String text,float size){return mc.font.width(text)*size;}
                public void line(float a,float b,float c,float d,int color){F35HudClientEvents.stroke(graphics,layout.left()+a*layout.scale(),layout.top()+b*layout.scale(),layout.left()+c*layout.scale(),layout.top()+d*layout.scale(),1,F35DisplayClientConfig.applyBrightness(color),pixelWidth,pixelHeight);}
                public void text(String text,float x,float y,float size,int color){
                    graphics.pose().pushPose();
                    try{
                        graphics.pose().translate(layout.left()+x*layout.scale(),layout.top()+y*layout.scale(),0);
                        graphics.pose().scale(size*layout.scale(),size*layout.scale(),1);
                        graphics.drawString(mc.font,text,0,0,F35DisplayClientConfig.applyBrightness(color),false);
                    }finally{graphics.pose().popPose();}
                }
                public void rotatedText(String text,float x,float y,float size,float degrees,int color){
                    graphics.pose().pushPose();
                    try{
                        graphics.pose().translate(layout.left()+x*layout.scale(),layout.top()+y*layout.scale(),0);
                        graphics.pose().mulPose(Axis.ZP.rotationDegrees(degrees));
                        graphics.pose().scale(size*layout.scale(),size*layout.scale(),1);
                        graphics.drawString(mc.font,text,0,0,F35DisplayClientConfig.applyBrightness(color),false);
                    }finally{graphics.pose().popPose();}
                }
            },state,contacts,hasLock,liveLock);
        }finally{graphics.pose().popPose();}
    }
}
