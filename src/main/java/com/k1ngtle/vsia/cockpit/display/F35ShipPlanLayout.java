package com.k1ngtle.vsia.cockpit.display;

import com.k1ngtle.vsia.cockpit.detection.F35ShipSilhouette;

/** Fit the visible reference hull, not empty grid margins, with true plan aspect ratio. */
public record F35ShipPlanLayout(float originX,float originY,float cellWidth,float cellHeight,
        int minX,int minY,int maxX,int maxY) {
    public static F35ShipPlanLayout fit(F35ShipSilhouette s,float areaX,float areaY,float areaWidth,float areaHeight){
        int minX=s.width(),minY=s.height(),maxX=-1,maxY=-1;
        for(int y=0;y<s.height();y++)for(int x=0;x<s.width();x++)if(s.occupied(x,y)){
            minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
        }
        if(maxX<0||areaWidth<=0||areaHeight<=0)return new F35ShipPlanLayout(areaX+areaWidth/2,areaY+areaHeight/2,0,0,0,0,-1,-1);
        float unitX=Math.max(1,s.sourceWidthBlocks())/(float)s.width(),unitY=Math.max(1,s.sourceLengthBlocks())/(float)s.height();
        float scale=Math.min(areaWidth/((maxX-minX+1)*unitX),areaHeight/((maxY-minY+1)*unitY)),cw=unitX*scale,ch=unitY*scale;
        return new F35ShipPlanLayout(areaX+areaWidth/2-(minX+maxX+1)*cw/2,areaY+areaHeight/2-(minY+maxY+1)*ch/2,cw,ch,minX,minY,maxX,maxY);
    }
    public float visibleCenterX(){return originX+(minX+maxX+1)*cellWidth/2;}
    public float visibleCenterY(){return originY+(minY+maxY+1)*cellHeight/2;}
}
