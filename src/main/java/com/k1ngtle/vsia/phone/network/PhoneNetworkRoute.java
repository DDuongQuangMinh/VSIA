package com.k1ngtle.vsia.phone.network;

import java.util.List;

public record PhoneNetworkRoute(
        Transport transport,
        List<String> hops
) {
    public enum Transport {
        WIFI,
        CELLULAR
    }

    public String summary() {
        return String.join(" → ", hops);
    }
}
