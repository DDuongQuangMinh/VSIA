package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkRoute;
import com.k1ngtle.vsia.phone.network.packet.C2SPhoneBrowserRequestPacket;
import com.k1ngtle.vsia.phone.network.packet.S2CPhoneBrowserResponsePacket;

public final class PhoneBrowser {
    private static final PhoneBrowser INSTANCE = new PhoneBrowser();

    private final BrowserHistory history = new BrowserHistory();

    private BrowserResponse response;
    private String currentUrl = "";
    private boolean loading;

    private int nextRequestId = 1;
    private int activeRequestId;

    private PhoneBrowser() {
    }

    public static PhoneBrowser get() {
        return INSTANCE;
    }

    public void navigate(String rawUrl) {
        navigateInternal(rawUrl, true);
    }

    public void reload() {
        if (currentUrl.isBlank()) {
            return;
        }

        navigateInternal(currentUrl, false);
    }

    public void back() {
        if (!history.canGoBack()) {
            return;
        }

        navigateInternal(history.back(), false);
    }

    public void forward() {
        if (!history.canGoForward()) {
            return;
        }

        navigateInternal(history.forward(), false);
    }

    private void navigateInternal(String rawUrl, boolean addHistory) {
        BrowserRequest request = new BrowserRequest(rawUrl);
        int requestId = nextRequestId++;
        activeRequestId = requestId;

        if (request.host().isBlank()) {
            loading = false;
            response = BrowserResponse.networkError(
                    request.url(),
                    "Invalid address.",
                    "Enter a VS:IA website hostname such as example.com.",
                    false
            );
            return;
        }

        PhoneNetworkRoute route = PhoneNetworkController.get().selectBrowserRoute();
        currentUrl = request.url();

        if (addHistory) {
            history.visit(currentUrl);
        }

        if (route == null) {
            loading = false;
            response = PhoneNetworkController.get().browserUnavailable(currentUrl);
            return;
        }

        loading = true;
        response = null;

        VsiaNetwork.sendToServer(
                new C2SPhoneBrowserRequestPacket(
                        requestId,
                        currentUrl,
                        route.transport().name()
                )
        );
    }

    public void acceptServerResponse(S2CPhoneBrowserResponsePacket packet) {
        if (packet.requestId() != activeRequestId) {
            return;
        }

        loading = false;
        currentUrl = packet.url();

        response = BrowserResponse.http(
                packet.url(),
                packet.statusCode(),
                packet.reason(),
                packet.contentType(),
                packet.body(),
                packet.styleSheet(),
                packet.transport(),
                packet.routeSummary()
        );
    }

    public BrowserHistory history() {
        return history;
    }

    public BrowserResponse response() {
        return response;
    }

    public String currentUrl() {
        return currentUrl;
    }

    public boolean loading() {
        return loading;
    }

    public boolean canGoBack() {
        return history.canGoBack();
    }

    public boolean canGoForward() {
        return history.canGoForward();
    }
}
