package com.k1ngtle.vsia.cockpit.detection;

import java.util.Arrays;

public record F35ShipSilhouette(
        int width,
        int height,
        int anchorX,
        int anchorY,
        int sourceWidthBlocks,
        int sourceLengthBlocks,
        boolean[] occupied
) {
    public F35ShipSilhouette {
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
