package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

import net.minecraft.core.BlockPos;

public record WifiEngineeringTestLinkResult(
        boolean success,
        BlockPos peerPos,
        double distanceBlocks,
        int frameBytes,
        String detail
) {
    public WifiEngineeringTestLinkResult {
        peerPos =
                peerPos == null
                        ? BlockPos.ZERO
                        : peerPos.immutable();

        detail =
                detail == null
                        ? ""
                        : detail;
    }
}
