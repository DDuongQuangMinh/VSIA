package com.k1ngtle.vsia.cockpit.display;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Project current world contacts using the actual frame camera/view/projection, not shipyard coordinates. */
public final class F35HudProjection {
    public record Point(float x,float y) { }
    private F35HudProjection() { }
    public static boolean finite(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    public static Point project(Vec3 world,Vec3 camera,Matrix4f view,Matrix4f projection,int width,int height) {
        if(!finite(world)||!finite(camera)||view==null||projection==null||width<=0||height<=0)return null;
        Vec3 relative=world.subtract(camera);
        Vector4f clip=new Vector4f((float)relative.x,(float)relative.y,(float)relative.z,1);
        view.transform(clip);projection.transform(clip);
        if(!Float.isFinite(clip.x)||!Float.isFinite(clip.y)||!Float.isFinite(clip.z)||!Float.isFinite(clip.w)||clip.w<=0.0001f)return null;
        float x=clip.x/clip.w,y=clip.y/clip.w,z=clip.z/clip.w;
        if(Math.abs(x)>1||Math.abs(y)>1||z < -1 || z > 1)return null;
        return new Point((x+1)*width*.5f,(1-y)*height*.5f);
    }
}
