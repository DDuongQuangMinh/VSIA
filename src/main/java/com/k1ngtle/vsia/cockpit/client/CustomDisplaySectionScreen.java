package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.F35CockpitSeatBlockEntity;
import com.k1ngtle.vsia.cockpit.display.*;
import com.k1ngtle.vsia.cockpit.program.*;
import com.k1ngtle.vsia.item.DisplayHardDriveItem;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A live pop-out of the installed custom section, not a stock F-35 substitute. */
public final class CustomDisplaySectionScreen extends Screen {
    private final UUID cockpit,drive;private final long revision;private final DisplayDesign design;private int section;
    private float left,top,scale;private CustomDisplaySections.Bounds bounds;private DisplayDesign selected;private F35DisplayState state;
    public CustomDisplaySectionScreen(F35CockpitSeatBlockEntity cockpit,int section){super(Component.literal("CUSTOM COCKPIT DISPLAY"));this.cockpit=cockpit.cockpitId();drive=DisplayHardDriveItem.driveId(cockpit.displayDrive());revision=DisplayHardDriveItem.revision(cockpit.displayDrive());DisplayDesign parsed;try{parsed=DisplayDesign.parse(DisplayHardDriveItem.layout(cockpit.displayDrive()));}catch(Exception e){parsed=new DisplayDesign("CYAN",List.of());}design=parsed;select(section);}
    private boolean valid(){var current=F35CockpitClientContext.seated();return F35CockpitClientContext.bindSeat(cockpit)&&current!=null&&Objects.equals(drive,DisplayHardDriveItem.driveId(current.displayDrive()))&&revision==DisplayHardDriveItem.revision(current.displayDrive())&&CustomDisplayScreenRouter.customInstalled();}
    private void select(int value){section=Math.max(1,Math.min(6,value));selected=CustomDisplaySections.select(design,section);bounds=CustomDisplaySections.bounds(selected);CustomDisplayFocus.select(cockpit,drive,revision,section);}
    @Override public void tick(){if(!valid())onClose();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0xf4050a0a);if(!valid()){g.drawString(font,"Custom drive changed / seat unavailable. Reopen this cockpit.",12,12,0xffffca53);return;}
        state=F35DisplayStateFactory.capture(F35CockpitClientContext.seated(),partial);
        g.drawString(font,"CUSTOM "+F35CockpitClientContext.label(cockpit)+" / SECTION "+section+" / "+CustomDisplaySections.title(design,section),12,10,0xff7fdddd);
        int tab=Math.max(1,(width-24)/6);for(int i=1;i<=6;i++){int x=12+(i-1)*tab;g.fill(x,28,x+tab-3,48,i==section?0xff254c4c:0xff152020);String label=i+" "+CustomDisplaySections.title(design,i);g.drawString(font,font.plainSubstrByWidth(label,Math.max(1,tab-8)),x+4,34,0xffdddddd);}
        scale=Math.max(.01f,Math.min((width-32f)/bounds.w(),(height-92f)/bounds.h()));left=(width-bounds.w()*scale)/2;top=58+(height-92-bounds.h()*scale)/2;
        if(selected.widgets().isEmpty())g.drawString(font,"No widgets assigned to this section. Use the laptop's Section control.",12,70,0xffffca53);
        else try(DisplayGuiClipScope clips=new DisplayGuiClipScope(new DisplayGuiClipScope.Backend(){public void push(int l,int t,int r,int b){g.enableScissor(l,t,r,b);}public void pop(){g.disableScissor();}})){
            DisplayDesignPainter.paint(new DisplayDesignPainter.Draw(){
                public void line(float a,float b,float c,float d,int color){int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(c-a),Math.abs(d-b))*scale));for(int i=0;i<=steps;i++){float f=i/(float)steps;int x=Math.round(left+(a+(c-a)*f-bounds.x())*scale),y=Math.round(top+(b+(d-b)*f-bounds.y())*scale);g.fill(x,y,x+1,y+1,color);}}
                public void text(String s,float x,float y,float size,int color){g.pose().pushPose();try{g.pose().translate(left+(x-bounds.x())*scale,top+(y-bounds.y())*scale,0);g.pose().scale(size*scale,size*scale,1);g.drawString(font,s,0,0,color,false);}finally{g.pose().popPose();}}
                public void clip(float x,float y,float w,float h){clips.replace((int)(left+(x-bounds.x())*scale),(int)(top+(y-bounds.y())*scale),(int)(left+(x+w-bounds.x())*scale),(int)(top+(y+h-bounds.y())*scale));}
            },selected,state);
        }
        g.drawString(font,"[1-6] CUSTOM SECTIONS  / CLICK TARGET = LOCK  / RIGHT CLICK or DEL = UNLOCK",12,height-28,0xffaaaaaa);
        g.drawString(font,"Aircraft systems / IFF",12,height-14,0xff7fdddd);g.drawString(font,"ESC: close   \\: cockpit config",Math.max(12,width-190),height-14,0xff888888);
    }
    @Override public boolean keyPressed(int key,int scan,int mods){int requested=CustomDisplaySections.key(key);if(requested>0){select(requested);return true;}if(key==261||key==259){F35TargetLockClient.clear();return true;}if(F35DisplayKeyMappings.CONFIGURE_DISPLAY.matches(key,scan)){minecraft.setScreen(new F35DisplayConfigScreen());return true;}return super.keyPressed(key,scan,mods);}
    @Override public boolean mouseClicked(double x,double y,int button){if(!valid())return false;if(button==1){F35TargetLockClient.clear();return true;}if(button==0&&y>=28&&y<=48&&x>=12&&x<width-12){select(Math.min(6,1+(int)(x-12)/Math.max(1,(width-24)/6)));return true;}if(button==0&&y>=height-16&&x<190){minecraft.setScreen(new F35DisplaySectionScreen(1));return true;}if(button==0&&state!=null){var target=DisplayDesignPainter.pick(selected,state,(float)(bounds.x()+(x-left)/scale),(float)(bounds.y()+(y-top)/scale),10/scale);if(target!=null){if(target.radar())F35TargetLockClient.toggleRadar(target.id());else F35TargetLockClient.toggleDetection(target.id());return true;}}return super.mouseClicked(x,y,button);}
}
