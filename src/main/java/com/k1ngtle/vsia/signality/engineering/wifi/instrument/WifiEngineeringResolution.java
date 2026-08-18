package com.k1ngtle.vsia.signality.engineering.wifi.instrument;

public record WifiEngineeringResolution(
        WifiEngineeringResolvedTarget target,
        String failureDetail
) {
    public boolean resolved() {
        return target != null;
    }

    public static WifiEngineeringResolution success(
            WifiEngineeringResolvedTarget target
    ) {
        return new WifiEngineeringResolution(
                target,
                ""
        );
    }

    public static WifiEngineeringResolution failure(
            String detail
    ) {
        return new WifiEngineeringResolution(
                null,
                detail == null
                        ? "Unable to resolve Wi-Fi engineering target"
                        : detail
        );
    }
}
