package com.k1ngtle.vsia.cockpit.display;

import java.util.ArrayList;
import java.util.List;

/** Filled HUD strokes, clipped in virtual coordinates; independent of GL line width. */
public final class F35HudStroke {
    public record Point(float x,float y) { }
    private F35HudStroke() { }
    public static List<Point> polygon(float a,float b,float c,float d,float thickness,float width,float height){
        for(float v:new float[]{a,b,c,d,thickness,width,height})if(!Float.isFinite(v))return List.of();
        if(thickness<=0||width<=0||height<=0)return List.of();
        double dx=(double)c-a,dy=(double)d-b,len=Math.hypot(dx,dy);float half=thickness/2;
        List<Point> points;
        if(len<.0001)points=List.of(new Point(a-half,b-half),new Point(a+half,b-half),new Point(a+half,b+half),new Point(a-half,b+half));
        else{
            float x=(float)(-dy/len*half),y=(float)(dx/len*half);
            points=List.of(new Point(a+x,b+y),new Point(c+x,d+y),new Point(c-x,d-y),new Point(a-x,b-y));
        }
        for(int edge=0;edge<4&&!points.isEmpty();edge++)points=clip(points,edge,edge==1?width:edge==3?height:0);
        return List.copyOf(points);
    }
    private static boolean inside(Point p,int edge,float limit){float value=edge<2?p.x():p.y();return edge%2==0?value>=limit:value<=limit;}
    private static List<Point> clip(List<Point> input,int edge,float limit){
        List<Point> output=new ArrayList<>();Point previous=input.get(input.size()-1);boolean previousInside=inside(previous,edge,limit);
        for(Point current:input){
            boolean currentInside=inside(current,edge,limit);
            if(currentInside!=previousInside){
                double before=edge<2?previous.x():previous.y(),after=edge<2?current.x():current.y();
                double t=(limit-before)/(after-before);
                output.add(edge<2?new Point(limit,(float)(previous.y()+t*((double)current.y()-previous.y()))):new Point((float)(previous.x()+t*((double)current.x()-previous.x())),limit));
            }
            if(currentInside)output.add(current);
            previous=current;previousInside=currentInside;
        }
        return output;
    }
}
