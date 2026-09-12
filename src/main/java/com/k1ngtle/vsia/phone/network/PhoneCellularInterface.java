package com.k1ngtle.vsia.phone.network;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.browser.WebsiteRenderer;

import java.util.List;

public final class PhoneCellularInterface {
    public BrowserResponse request(BrowserRequest request) {
        PhoneNetworkState state = PhoneNetworkState.get();
        var c = state.getCellular();

        if (!c.enabled()) {
            return BrowserResponse.networkError(
                    "Cellular Data is turned off.",
                    List.of("Turn on Cellular Data in Settings."),
                    false
            );
        }

        if (!"ACTIVE".equals(c.pduState())) {
            return BrowserResponse.networkError(
                    "The server cannot be found.",
                    List.of(
                            "Network",
                            "Cellular Data",
                            "",
                            "PDU session is not active."
                    ),
                    false
            );
        }

        if (!state.isCellularUsable()) {
            return BrowserResponse.networkError(
                    "Cellular network unavailable.",
                    List.of(
                            "RRC: " + c.rrcState(),
                            "NAS: " + c.nasState(),
                            "PDU: " + c.pduState()
                    ),
                    false
            );
        }

        PhoneNetworkRoute route = new PhoneNetworkRoute(
                PhoneNetworkRoute.Transport.CELLULAR,
                List.of("Browser", "Cellular", "UE", "gNB", "5G Core", "UPF", "DNS", "HTTP")
        );

        return WebsiteRenderer.handle(request, route);
    }
}
