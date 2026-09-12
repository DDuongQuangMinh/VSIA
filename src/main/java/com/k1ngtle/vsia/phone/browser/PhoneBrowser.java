package com.k1ngtle.vsia.phone.browser;

import com.k1ngtle.vsia.network.VsiaNetwork;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import com.k1ngtle.vsia.phone.network.PhoneNetworkRoute;
import com.k1ngtle.vsia.phone.network.packet.C2SPhoneBrowserRequestPacket;
import com.k1ngtle.vsia.phone.network.packet.S2CPhoneBrowserResponsePacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PhoneBrowser {
    private static final PhoneBrowser INSTANCE =
            new PhoneBrowser();

    private static final int MAX_TABS = 12;

    private final List<TabState> tabs =
            new ArrayList<>();

    private final Set<String> bookmarks =
            new LinkedHashSet<>();

    private int activeTabIndex;
    private int nextTabId = 1;
    private int nextRequestId = 1;

    private PhoneBrowser() {
        tabs.add(
                new TabState(
                        nextTabId++
                )
        );
    }

    public static PhoneBrowser get() {
        return INSTANCE;
    }

    public void navigate(
            String rawUrl
    ) {
        navigateInternal(
                activeTab(),
                rawUrl,
                true
        );
    }

    public void reload() {
        TabState tab =
                activeTab();

        if (tab.currentUrl
                .isBlank()) {
            return;
        }

        navigateInternal(
                tab,
                tab.currentUrl,
                false
        );
    }

    public void back() {
        TabState tab =
                activeTab();

        if (!tab.history
                .canGoBack()) {
            return;
        }

        navigateInternal(
                tab,
                tab.history.back(),
                false
        );
    }

    public void forward() {
        TabState tab =
                activeTab();

        if (!tab.history
                .canGoForward()) {
            return;
        }

        navigateInternal(
                tab,
                tab.history.forward(),
                false
        );
    }

    private void navigateInternal(
            TabState tab,
            String rawUrl,
            boolean addHistory
    ) {
        BrowserRequest request =
                new BrowserRequest(
                        rawUrl
                );

        int requestId =
                nextRequestId++;

        tab.activeRequestId =
                requestId;

        if (request.host()
                .isBlank()) {
            tab.loading = false;

            tab.response =
                    BrowserResponse
                            .networkError(
                                    request.url(),
                                    "Invalid address.",
                                    "Enter a VS:IA website hostname such as example.com.",
                                    false
                            );

            return;
        }

        PhoneNetworkRoute route =
                PhoneNetworkController
                        .get()
                        .selectBrowserRoute();

        tab.currentUrl =
                request.url();

        if (addHistory) {
            tab.history.visit(
                    tab.currentUrl
            );
        }

        if (route == null) {
            tab.loading = false;

            tab.response =
                    PhoneNetworkController
                            .get()
                            .browserUnavailable(
                                    tab.currentUrl
                            );

            return;
        }

        tab.loading = true;
        tab.response = null;

        VsiaNetwork.sendToServer(
                new C2SPhoneBrowserRequestPacket(
                        requestId,
                        tab.currentUrl,
                        route.transport()
                                .name()
                )
        );
    }

    public void acceptServerResponse(
            S2CPhoneBrowserResponsePacket packet
    ) {
        TabState tab =
                findTabForRequest(
                        packet.requestId()
                );

        if (tab == null) {
            return;
        }

        tab.loading = false;
        tab.currentUrl =
                packet.url();

        tab.history.replaceCurrent(
                packet.url()
        );

        tab.response =
                BrowserResponse.http(
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

    private TabState findTabForRequest(
            int requestId
    ) {
        for (TabState tab
                : tabs) {
            if (tab.activeRequestId
                    == requestId) {
                return tab;
            }
        }

        return null;
    }

    public boolean newTab() {
        return newTab("");
    }

    public boolean newTab(
            String initialUrl
    ) {
        if (tabs.size()
                >= MAX_TABS) {
            return false;
        }

        TabState tab =
                new TabState(
                        nextTabId++
                );

        tabs.add(tab);

        activeTabIndex =
                tabs.size() - 1;

        if (initialUrl != null
                && !initialUrl.isBlank()) {
            navigateInternal(
                    tab,
                    initialUrl,
                    true
            );
        }

        return true;
    }

    public boolean closeTab(
            int index
    ) {
        if (index < 0
                || index >= tabs.size()) {
            return false;
        }

        tabs.remove(index);

        if (tabs.isEmpty()) {
            tabs.add(
                    new TabState(
                            nextTabId++
                    )
            );

            activeTabIndex = 0;
            return true;
        }

        if (activeTabIndex > index) {
            activeTabIndex--;
        } else if (activeTabIndex == index) {
            activeTabIndex =
                    Math.min(
                            index,
                            tabs.size() - 1
                    );
        }

        return true;
    }

    public boolean selectTab(
            int index
    ) {
        if (index < 0
                || index >= tabs.size()) {
            return false;
        }

        activeTabIndex = index;
        return true;
    }

    public int currentTabIndex() {
        return activeTabIndex;
    }

    public int tabCount() {
        return tabs.size();
    }

    public List<TabSnapshot> tabs() {
        List<TabSnapshot> snapshots =
                new ArrayList<>();

        for (int i = 0;
             i < tabs.size();
             i++) {
            TabState tab =
                    tabs.get(i);

            snapshots.add(
                    new TabSnapshot(
                            i,
                            tab.id,
                            tab.currentUrl,
                            titleFor(tab),
                            tab.loading,
                            i == activeTabIndex
                    )
            );
        }

        return Collections
                .unmodifiableList(
                        snapshots
                );
    }

    private String titleFor(
            TabState tab
    ) {
        if (tab.currentUrl
                .isBlank()) {
            return "Start Page";
        }

        BrowserRequest request =
                new BrowserRequest(
                        tab.currentUrl
                );

        String host =
                request.host();

        if (!host.isBlank()) {
            return host;
        }

        return tab.currentUrl;
    }

    public void addBookmark(
            String url
    ) {
        if (url == null
                || url.isBlank()) {
            return;
        }

        bookmarks.add(
                new BrowserRequest(
                        url
                ).displayUrl()
        );
    }

    public void removeBookmark(
            String url
    ) {
        if (url == null
                || url.isBlank()) {
            return;
        }

        bookmarks.remove(
                new BrowserRequest(
                        url
                ).displayUrl()
        );
    }

    public boolean toggleBookmark(
            String url
    ) {
        if (url == null
                || url.isBlank()) {
            return false;
        }

        String normalized =
                new BrowserRequest(
                        url
                ).displayUrl();

        if (bookmarks.contains(
                normalized
        )) {
            bookmarks.remove(
                    normalized
            );

            return false;
        }

        bookmarks.add(
                normalized
        );

        return true;
    }

    public boolean isBookmarked(
            String url
    ) {
        if (url == null
                || url.isBlank()) {
            return false;
        }

        return bookmarks.contains(
                new BrowserRequest(
                        url
                ).displayUrl()
        );
    }

    public List<String> bookmarks() {
        return List.copyOf(
                bookmarks
        );
    }

    public BrowserHistory history() {
        return activeTab()
                .history;
    }

    public BrowserResponse response() {
        return activeTab()
                .response;
    }

    public String currentUrl() {
        return activeTab()
                .currentUrl;
    }

    public boolean loading() {
        return activeTab()
                .loading;
    }

    public boolean canGoBack() {
        return activeTab()
                .history
                .canGoBack();
    }

    public boolean canGoForward() {
        return activeTab()
                .history
                .canGoForward();
    }

    private TabState activeTab() {
        if (tabs.isEmpty()) {
            tabs.add(
                    new TabState(
                            nextTabId++
                    )
            );

            activeTabIndex = 0;
        }

        if (activeTabIndex < 0
                || activeTabIndex
                >= tabs.size()) {
            activeTabIndex = 0;
        }

        return tabs.get(
                activeTabIndex
        );
    }

    public record TabSnapshot(
            int index,
            int id,
            String url,
            String title,
            boolean loading,
            boolean active
    ) {
    }

    private static final class TabState {
        private final int id;
        private final BrowserHistory history =
                new BrowserHistory();

        private BrowserResponse response;
        private String currentUrl = "";
        private boolean loading;
        private int activeRequestId;

        private TabState(
                int id
        ) {
            this.id = id;
        }
    }
}
