package com.k1ngtle.vsia.cockpit.display;

/** Reusable, viewport-clipped screen-pixel strokes. No work proportional to line length. */
public final class F35HudLineMesh {
    @FunctionalInterface public interface Quads {
        void quad(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy,int color);
    }
    // A clipped convex quadrilateral has at most eight vertices. Scratch is instance-owned.
    private final float[] first=new float[24],second=new float[24];
    private final Quads output;
    public F35HudLineMesh(Quads output){this.output=output;}
    public void line(float a,float b,float c,float d,float thickness,int color,float width,float height){
        if(!Float.isFinite(a)||!Float.isFinite(b)||!Float.isFinite(c)||!Float.isFinite(d)
            ||!Float.isFinite(thickness)||!Float.isFinite(width)||!Float.isFinite(height)
            ||thickness<=0||width<=0||height<=0)return;
        // Pixel-center offset preserves thin strokes under the inverse GUI-scale pose.
        double dx=(double)c-a,dy=(double)d-b,len=Math.hypot(dx,dy);
        float half=thickness/2;a+=.5f;b+=.5f;c+=.5f;d+=.5f;
        float x,y;
        if(len<.0001){
            first[0]=a-half;first[1]=b+half;first[2]=a+half;first[3]=b+half;
            first[4]=a+half;first[5]=b-half;first[6]=a-half;first[7]=b-half;
        }else{
            x=(float)(-dy/len*half);y=(float)(dx/len*half);
            first[0]=a+x;first[1]=b+y;first[2]=c+x;first[3]=d+y;
            first[4]=c-x;first[5]=d-y;first[6]=a-x;first[7]=b-y;
        }
        float[] input=first,clipped=second;int count=4;
        for(int edge=0;edge<4&&count>0;edge++){
            float limit=edge==1?width:edge==3?height:0;int next=0;
            float px=input[(count-1)*2],py=input[(count-1)*2+1];
            boolean before=inside(px,py,edge,limit);
            for(int i=0;i<count;i++){
                float qx=input[i*2],qy=input[i*2+1];boolean after=inside(qx,qy,edge,limit);
                if(before!=after){
                    double old=edge<2?px:py,value=edge<2?qx:qy,t=(limit-old)/(value-old);
                    clipped[next*2]=edge<2?limit:(float)(px+t*((double)qx-px));
                    clipped[next*2+1]=edge<2?(float)(py+t*((double)qy-py)):limit;next++;
                }
                if(after){clipped[next*2]=qx;clipped[next*2+1]=qy;next++;}
                px=qx;py=qy;before=after;
            }
            count=next;float[] swap=input;input=clipped;clipped=swap;
        }
        if(count==4){output.quad(input[0],input[1],input[2],input[3],input[4],input[5],input[6],input[7],color);}
        else for(int i=1;i<count-1;i++){
            // A degenerate fourth vertex lets clipped triangle fans share the GUI QUADS buffer.
            output.quad(input[0],input[1],input[i*2],input[i*2+1],input[(i+1)*2],input[(i+1)*2+1],input[(i+1)*2],input[(i+1)*2+1],color);
        }
    }
    private static boolean inside(float x,float y,int edge,float limit){float v=edge<2?x:y;return edge%2==0?v>=limit:v<=limit;}
}
