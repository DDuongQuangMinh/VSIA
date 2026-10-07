package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.display.F35HudLineMesh;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** One geometry buffer followed by grouped labels, using the same GUI shader/font as vanilla. */
final class F35HudBatch implements AutoCloseable {
    private record Label(String text,float x,float y,float size,float angle,int color,boolean shadow){ }
    private final GuiGraphics graphics;
    private final Font font;
    private final Matrix4f pose;
    private final VertexConsumer vertices;
    private final F35HudLineMesh mesh;
    private final List<Label> labels=new ArrayList<>(96);
    private final int width,height;
    private boolean closed;
    F35HudBatch(GuiGraphics graphics,Font font,int width,int height){
        this.graphics=graphics;this.font=font;this.width=width;this.height=height;
        graphics.flush(); // Never mix pending vanilla/UI geometry with our phase.
        pose=new Matrix4f(graphics.pose().last().pose());
        vertices=graphics.bufferSource().getBuffer(RenderType.gui());
        mesh=new F35HudLineMesh(this::quad);
    }
    void line(float a,float b,float c,float d,int thickness,int color){mesh.line(a,b,c,d,thickness,color,width,height);}
    void panel(float left,float top,float right,float bottom,int color){
        left=Math.max(0,left);top=Math.max(0,top);right=Math.min(width,right);bottom=Math.min(height,bottom);
        if(right>left&&bottom>top)quad(left,bottom,right,bottom,right,top,left,top,color);
    }
    private void vertex(float x,float y,int color){vertices.vertex(pose,x,y,0).color((color>>16)&255,(color>>8)&255,color&255,color>>>24).endVertex();}
    private void quad(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy,int color){
        vertex(ax,ay,color);vertex(bx,by,color);vertex(cx,cy,color);vertex(dx,dy,color);
    }
    void text(String text,float x,float y,float size,float angle,int color,boolean shadow){
        if(text!=null&&!text.isEmpty())labels.add(new Label(text,x,y,size,angle,color,shadow));
    }
    @Override public void close(){
        if(closed)return;closed=true;
        graphics.flush(); // Submit all backing/lines before the labels, preserving transparency order.
        try{
            Matrix4f transform=new Matrix4f();
            for(var label:labels){
                transform.set(pose).translate(label.x(),label.y(),0);
                if(label.angle()!=0)transform.rotateZ((float)Math.toRadians(label.angle()));
                transform.scale(label.size(),label.size(),1);
                font.drawInBatch(label.text(),0,0,label.color(),label.shadow(),transform,graphics.bufferSource(),Font.DisplayMode.NORMAL,0,15728880,font.isBidirectional());
            }
        }finally{graphics.flush();labels.clear();}
    }
}
