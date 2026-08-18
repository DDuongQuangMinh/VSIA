package com.k1ngtle.vsia.signality.engineering.wifi.live;

public record WifiLivePhyDecision(
        WifiLivePhyMode mode,
        WifiLivePhyPath path,
        boolean evaluated,
        boolean delivered,
        int codewords,
        int decoderIterations,
        double snrDb,
        String detail
) {
    public static WifiLivePhyDecision bypass(
            WifiLivePhyMode mode,
            double snrDb,
            String detail
    ) {
        return new WifiLivePhyDecision(
                mode,
                WifiLivePhyPath.BYPASS,
                false,
                true,
                0,
                0,
                snrDb,
                detail
        );
    }

    public static WifiLivePhyDecision fallback(
            WifiLivePhyMode mode,
            double snrDb,
            String detail
    ) {
        return new WifiLivePhyDecision(
                mode,
                WifiLivePhyPath.UNSUPPORTED_FALLBACK,
                false,
                true,
                0,
                0,
                snrDb,
                detail
        );
    }
}
