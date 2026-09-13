package com.k1ngtle.vsia.signality.internet.satellite.internet;

import com.k1ngtle.vsia.phone.browser.PhoneBrowserServerService;
import com.k1ngtle.vsia.signality.internet.routing.LongHaulRoutePolicy;
import net.minecraft.server.level.ServerPlayer;

public final class SatelliteInternetService {
    public static final double SATELLITE_REQUIRED_DISTANCE_BLOCKS =
            LongHaulRoutePolicy.SATELLITE_REQUIRED_DISTANCE_BLOCKS;

    private SatelliteInternetService() {
    }

    public static PhoneBrowserServerService.ServerPage fetchWebsite(
            ServerPlayer player,
            String rawUrl
    ) {
        return LongHaulBrowserService.fetch(
                player,
                rawUrl,
                "WIFI"
        );
    }

    public static String policySummary() {
        return "Satellite is mandatory when the physical endpoint distance is 5 km / 5000 blocks or more. Below 5 km, VS:IA uses the normal terrestrial route.";
    }
}
