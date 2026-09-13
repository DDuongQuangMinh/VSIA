package com.k1ngtle.vsia.phone.network;

import java.util.List;

public final class PhoneCellularInterface {
    public PhoneNetworkRoute browserRoute() {
        PhoneNetworkState state =
                PhoneNetworkState.get();

        if (!state.isCellularUsable()) {
            return null;
        }

        PhoneNetworkState.CellularStatus cellular =
                state.getCellular();

        return new PhoneNetworkRoute(
                        PhoneNetworkRoute.Transport.CELLULAR,
                        List.of(
                                "Browser",
                                cellular.radioLabel(),
                                cellular.carrier(),
                                "Cell "
                                        + cellular.cellId(),
                                "RAN",
                                "Core",
                                "UPF"
                        )
                );
    }
}
