package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.display.*;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;

/** The in-world transparent combiner above the physical monitor, not a screen-sized black panel. */
public final class F35HologramRenderer {
    private F35HologramRenderer() { }
    public static void render(PoseStack pose,MultiBufferSource buffers,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock,float width,float height) {
        float scale=Math.min(width/F35HologramPainter.WIDTH,height/F35HologramPainter.HEIGHT);
        pose.pushPose();
        try{
            pose.translate(-F35HologramPainter.WIDTH*scale/2,F35HologramPainter.HEIGHT*scale/2,0);
            pose.scale(scale,-scale,scale);
            F35DisplayCanvas canvas=new F35DisplayCanvas(pose,buffers);
            canvas.setClip(0,0,F35HologramPainter.WIDTH,F35HologramPainter.HEIGHT);
            try{F35HologramPainter.paint(new F35HologramPainter.Draw(){
                public void line(float x1,float y1,float x2,float y2,int color){canvas.line(x1,y1,x2,y2,color);}
                public void text(String text,float x,float y,float size,int color){canvas.text(text,x,y,size,color);}
            },state,contacts,hasLock,liveLock);}finally{canvas.clearClip();}
        }finally{pose.popPose();}
    }
}
