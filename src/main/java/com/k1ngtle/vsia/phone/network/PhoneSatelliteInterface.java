package com.k1ngtle.vsia.phone.network;

import java.util.List;

public final class PhoneSatelliteInterface {
    public PhoneNetworkRoute browserRoute() {
        return new PhoneNetworkRoute(
                PhoneNetworkRoute.Transport.SATELLITE,
                List.of(
                        "Satellite User Terminal",
                        "LEO SATCOM",
                        "Internet Gateway",
                        "ISP1 / Data Center"
                )
        );
    }
}
