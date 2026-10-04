package com.k1ngtle.vsia.cockpit.client;

import com.k1ngtle.vsia.cockpit.display.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

/** The in-world transparent combiner above the physical monitor, not a screen-sized black panel. */
public final class F35HologramRenderer {
    private F35HologramRenderer() { }
    public static void render(PoseStack pose,MultiBufferSource buffers,F35DisplayState state,List<F35HudContacts.Contact> contacts,boolean hasLock,boolean liveLock,float width,float height) {
        float scale=Math.min(width/F35HologramPainter.WIDTH,height/F35HologramPainter.HEIGHT);
        pose.pushPose();
        try{
            pose.translate(-F35HologramPainter.WIDTH*scale/2,F35HologramPainter.HEIGHT*scale/2,0);
            pose.scale(scale,-scale,scale);
            Font font=Minecraft.getInstance().font;
            F35HologramPainter.paint(new F35HologramPainter.Draw(){
                public float textWidth(String text,float size){return font.width(text)*size;}
                public void line(float x1,float y1,float x2,float y2,int color){
                    // Unlit, two-sided filled geometry instead of one-pixel GL_LINES.
                    // A single slender stroke avoids the broad dark border and
                    // coplanar halo/core overlap seen in the previous screenshot.
                    stroke(pose,buffers,x1,y1,x2,y2,F35HologramPainter.STROKE_WIDTH,F35DisplayClientConfig.applyBrightness(color));
                }
                public void text(String text,float x,float y,float size,int color){
                    if(size<=0||x<0||y<0||y+font.lineHeight*size>F35HologramPainter.HEIGHT)return;
                    String visible=font.plainSubstrByWidth(text,Math.max(0,(int)((F35HologramPainter.WIDTH-x)/size)));
                    pose.pushPose();
                    try{
                        pose.translate(x,y,.1f);pose.scale(size,size,1);
                        // Solid font-atlas glyphs are readable at oblique angles; a
                        // shadow adds contrast without an opaque hologram backplate.
                        font.drawInBatch(visible,0,0,F35DisplayClientConfig.applyBrightness(color),true,pose.last().pose(),buffers,Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);
                    }finally{pose.popPose();}
                }
            },state,contacts,hasLock,liveLock);
        }finally{pose.popPose();}
    }
    private static void stroke(PoseStack pose,MultiBufferSource buffers,float a,float b,float c,float d,float width,int color){
        var polygon=F35HudStroke.polygon(a,b,c,d,width,F35HologramPainter.WIDTH,F35HologramPainter.HEIGHT);
        if(polygon.size()<3)return;
        VertexConsumer vertices=buffers.getBuffer(RenderType.debugQuads());Matrix4f matrix=pose.last().pose();
        // A clipped stroke can have more than four corners. Degenerate quads form
        // a triangle fan, keeping every vertex within the transparent HUD bounds.
        for(int i=1;i+1<polygon.size();i++){
            vertex(vertices,matrix,polygon.get(0),color);vertex(vertices,matrix,polygon.get(i),color);
            vertex(vertices,matrix,polygon.get(i+1),color);vertex(vertices,matrix,polygon.get(i+1),color);
        }
    }
    private static void vertex(VertexConsumer vertices,Matrix4f matrix,F35HudStroke.Point p,int color){vertices.vertex(matrix,p.x(),p.y(),0).color((color>>>16)&255,(color>>>8)&255,color&255,(color>>>24)&255).endVertex();}
}
