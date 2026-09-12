package com.k1ngtle.vsia.phone.network;

import java.util.List;

public final class PhoneCellularInterface {
    public PhoneNetworkRoute browserRoute() {
        if (!PhoneNetworkState.get().isCellularUsable()) {
            return null;
        }

        return new PhoneNetworkRoute(
                PhoneNetworkRoute.Transport.CELLULAR,
                List.of(
                        "Browser",
                        "Cellular",
                        "UE",
                        "gNB",
                        "5G Core",
                        "UPF",
                        "DNS",
                        "HTTP"
                )
        );
    }
}
