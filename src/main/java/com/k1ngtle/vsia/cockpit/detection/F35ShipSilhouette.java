package com.k1ngtle.vsia.cockpit.detection;

import java.util.Arrays;

public record F35ShipSilhouette(
        int width,
        int height,
        int anchorX,
        int anchorY,
        int sourceWidthBlocks,
        int sourceLengthBlocks,
        boolean[] occupied,
        F35HullDamage damage
) {
    public F35ShipSilhouette(int width,int height,int anchorX,int anchorY,int sourceWidthBlocks,int sourceLengthBlocks,boolean[] occupied){
        this(width,height,anchorX,anchorY,sourceWidthBlocks,sourceLengthBlocks,occupied,F35HullDamage.unknown("NO BASELINE"));
    }
    public F35ShipSilhouette {
        damage=damage==null?F35HullDamage.unknown("NO DATA"):damage;
        if(width<0||width>64||height<0||height>64)throw new IllegalArgumentException("Invalid hull grid dimensions");
        if(damage.known()&&damage.expected().length!=width*height)throw new IllegalArgumentException("Hull damage grid differs from plan");
        width =
                Math.max(
                        0,
                        width
                );

        height =
                Math.max(
                        0,
                        height
                );

        occupied =
                occupied == null
                        ? new boolean[0]
                        : Arrays.copyOf(
                        occupied,
                        occupied.length
                );
    }
    @Override public boolean[] occupied(){return occupied.clone();}
    public static F35ShipSilhouette empty(F35HullDamage damage){return new F35ShipSilhouette(0,0,-1,-1,0,0,new boolean[0],damage);}

    public static F35ShipSilhouette empty() {
        return new F35ShipSilhouette(
                0,
                0,
                -1,
                -1,
                0,
                0,
                new boolean[0]
        );
    }

    public boolean available() {
        return width > 0
                && height > 0
                && occupied.length
                >= width * height;
    }

    public boolean occupied(
            int x,
            int y
    ) {
        if (x < 0
                || y < 0
                || x >= width
                || y >= height) {
            return false;
        }

        int index =
                y * width + x;

        return index >= 0
                && index < occupied.length
                && occupied[index];
    }
}
