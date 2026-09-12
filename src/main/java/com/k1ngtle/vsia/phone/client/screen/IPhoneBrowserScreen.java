package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.browser.PhoneBrowser;
import com.k1ngtle.vsia.phone.browser.PhoneHtmlDocument;
import com.k1ngtle.vsia.phone.browser.PhoneHtmlRenderer;
import com.k1ngtle.vsia.phone.client.PhoneText;
import com.k1ngtle.vsia.phone.client.widget.PhoneAddressField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class IPhoneBrowserScreen extends IPhoneScreen {
    private final PhoneBrowser browser =
            PhoneBrowser.get();

    private PhoneAddressField addressField;
    private BrowserResponse lastResponse;
    private PhoneHtmlDocument document;

    private PhoneHtmlRenderer.RenderResult lastRender =
            PhoneHtmlRenderer.RenderResult.empty();

    private BrowserOverlay overlay =
            BrowserOverlay.NONE;

    private int overlayScroll;
    private int scrollY;

    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    private int browserPanelY;
    private int controlsY;
    private int addressY;

    private String toast = "";
    private long toastUntil;

    public IPhoneBrowserScreen() {
        super(
                Component.literal(
                        "Browser"
                )
        );
    }

    @Override
    protected void init() {
        super.init();

        contentX =
                phoneX + 8;

        contentY =
                phoneY + 38;

        contentWidth =
                PHONE_WIDTH - 16;

        browserPanelY =
                phoneY
                        + PHONE_HEIGHT
                        - 118;

        controlsY =
                browserPanelY + 8;

        addressY =
                browserPanelY + 39;

        contentHeight =
                browserPanelY
                        - contentY;

        addressField =
                new PhoneAddressField(
                        font,
                        phoneX + 43,
                        addressY + 7,
                        PHONE_WIDTH - 82,
                        20
                );

        addressField.setMaxLength(
                512
        );

        addressField.setPlaceholder(
                "Search or enter website"
        );

        addressField.setValue(
                displayAddress(
                        browser.currentUrl()
                )
        );

        addRenderableWidget(
                addressField
        );
    }

    @Override
    public void tick() {
        super.tick();

        if (addressField == null) {
            return;
        }

        if (overlay
                != BrowserOverlay.NONE) {
            addressField.setFocused(
                    false
            );

            return;
        }

        if (!addressField
                .isFocused()) {
            String current =
                    displayAddress(
                            browser.currentUrl()
                    );

            if (!addressField
                    .getValue()
                    .equals(current)) {
                addressField.setValue(
                        current
                );
            }
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                0xFF0D0D0F
        );

        renderStatusBar(
                graphics
        );

        refreshDocument();

        graphics.fill(
                contentX,
                contentY,
                contentX + contentWidth,
                contentY + contentHeight,
                0xFFFFFFFF
        );

        if (browser.loading()) {
            renderLoading(
                    graphics
            );
        } else if (browser.response()
                == null) {
            renderStartPage(
                    graphics
            );
        } else {
            BrowserResponse response =
                    browser.response();

            if (response.statusCode()
                    == 0) {
                renderNetworkError(
                        graphics,
                        response
                );
            } else {
                renderWebsite(
                        graphics,
                        response
                );
            }
        }

        renderBrowserChrome(
                graphics
        );

        renderHomeIndicator(
                graphics
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        if (overlay
                != BrowserOverlay.NONE) {
            renderOverlay(
                    graphics,
                    mouseX,
                    mouseY
            );
        }

        renderToast(
                graphics
        );
    }

    private void refreshDocument() {
        BrowserResponse response =
                browser.response();

        if (response
                == lastResponse) {
            return;
        }

        lastResponse =
                response;

        scrollY = 0;

        if (response == null
                || response.statusCode()
                == 0) {
            document = null;

            lastRender =
                    PhoneHtmlRenderer
                            .RenderResult
                            .empty();

            return;
        }

        document =
                PhoneHtmlDocument.from(
                        response
                );
    }

    private void forcePageRefresh() {
        lastResponse = null;
        document = null;
        scrollY = 0;
        overlayScroll = 0;

        syncAddressFromBrowser();
    }

    private void renderWebsite(
            GuiGraphics graphics,
            BrowserResponse response
    ) {
        if (document == null) {
            return;
        }

        lastRender =
                PhoneHtmlRenderer.render(
                        graphics,
                        font,
                        document,
                        contentX,
                        contentY,
                        contentWidth,
                        contentHeight,
                        scrollY
                );

        if (!response.success()) {
            String status =
                    response.statusCode()
                            + " "
                            + response.reason();

            graphics.fill(
                    contentX,
                    contentY,
                    contentX
                            + contentWidth,
                    contentY + 20,
                    0xF2FF6961
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    status,
                    phoneX
                            + PHONE_WIDTH
                            / 2,
                    contentY + 6,
                    0xFFFFFFFF
            );
        }
    }

    private void renderLoading(
            GuiGraphics graphics
    ) {
        graphics.fill(
                contentX,
                contentY,
                contentX
                        + contentWidth,
                contentY
                        + contentHeight,
                0xFFF2F2F7
        );

        int centerX =
                phoneX
                        + PHONE_WIDTH
                        / 2;

        int centerY =
                contentY
                        + contentHeight
                        / 2;

        drawCompass(
                graphics,
                centerX,
                centerY - 30,
                36
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Loading Website",
                centerX,
                centerY + 14,
                0xFF1C1C1E
        );

        int progressWidth =
                PHONE_WIDTH - 30;

        int phase =
                (int) (
                        System.currentTimeMillis()
                                / 80L
                                % progressWidth
                );

        graphics.fill(
                phoneX + 15,
                contentY,
                phoneX
                        + 15
                        + phase,
                contentY + 2,
                0xFF0A84FF
        );
    }

    private void renderStartPage(
            GuiGraphics graphics
    ) {
        graphics.fill(
                contentX,
                contentY,
                contentX
                        + contentWidth,
                contentY
                        + contentHeight,
                0xFFF7F7FA
        );

        int centerX =
                phoneX
                        + PHONE_WIDTH
                        / 2;

        int centerY =
                contentY
                        + contentHeight
                        / 2
                        - 28;

        drawCompass(
                graphics,
                centerX,
                centerY - 34,
                44
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Safari",
                centerX,
                centerY + 14,
                0xFF111111
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Browse published VS:IA websites",
                centerX,
                centerY + 37,
                0xFF48484A
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "by domain or direct Server Rack.",
                centerX,
                centerY + 52,
                0xFF6E6E73
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Tap the address field below.",
                centerX,
                centerY + 82,
                0xFF8E8E93
        );
    }

    private void renderNetworkError(
            GuiGraphics graphics,
            BrowserResponse response
    ) {
        graphics.fill(
                contentX,
                contentY,
                contentX
                        + contentWidth,
                contentY
                        + contentHeight,
                0xFFF7F7FA
        );

        int centerX =
                phoneX
                        + PHONE_WIDTH
                        / 2;

        int y =
                contentY + 48;

        PhoneText.drawCentered(
                graphics,
                font,
                "Safari cannot open the page",
                centerX,
                y,
                0xFF111111
        );

        y += 28;

        for (String line
                : response
                .reason()
                .split("\n")) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    line,
                    centerX,
                    y,
                    0xFF48484A
            );

            y += 15;
        }

        if (!response.body()
                .isBlank()) {
            y += 8;

            for (String line
                    : response
                    .body()
                    .split("\n")) {
                PhoneText.drawCentered(
                        graphics,
                        font,
                        line,
                        centerX,
                        y,
                        0xFF6E6E73
                );

                y += 14;
            }
        }

        if (response
                .openWifiSettingsSuggested()) {
            int buttonX =
                    phoneX + 45;

            int buttonY =
                    contentY
                            + contentHeight
                            - 55;

            roundedRect(
                    graphics,
                    buttonX,
                    buttonY,
                    PHONE_WIDTH - 90,
                    32,
                    10,
                    0xFF0A84FF
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    "Open Wi-Fi Settings",
                    phoneX
                            + PHONE_WIDTH
                            / 2,
                    buttonY + 11,
                    0xFFFFFFFF
            );
        }
    }

    private void renderBrowserChrome(
            GuiGraphics graphics
    ) {
        roundedRect(
                graphics,
                phoneX + 7,
                browserPanelY,
                PHONE_WIDTH - 14,
                86,
                16,
                0xF7F2F2F7
        );

        int enabled =
                0xFF007AFF;

        int disabled =
                0xFFB4B4B9;

        drawBackArrow(
                graphics,
                phoneX + 39,
                controlsY + 9,
                browser.canGoBack()
                        ? enabled
                        : disabled
        );

        drawForwardArrow(
                graphics,
                phoneX + 83,
                controlsY + 9,
                browser.canGoForward()
                        ? enabled
                        : disabled
        );

        drawShare(
                graphics,
                phoneX + 128,
                controlsY + 9,
                enabled
        );

        drawBookmarks(
                graphics,
                phoneX + 172,
                controlsY + 9,
                enabled
        );

        drawTabs(
                graphics,
                phoneX + 214,
                controlsY + 9,
                enabled,
                browser.tabCount()
        );

        roundedRect(
                graphics,
                phoneX + 15,
                addressY,
                PHONE_WIDTH - 30,
                34,
                12,
                0xFFE4E4E9
        );

        drawLock(
                graphics,
                phoneX + 26,
                addressY + 10,
                browser.response() != null
                        && browser
                        .response()
                        .success()
                        ? 0xFF5F6368
                        : 0xFF8E8E93
        );
    }

    private void renderOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                phoneX + 6,
                phoneY + 36,
                phoneX
                        + PHONE_WIDTH
                        - 6,
                phoneY
                        + PHONE_HEIGHT
                        - 26,
                0x77000000
        );

        switch (overlay) {
            case SHARE ->
                    renderShareSheet(
                            graphics
                    );

            case BOOKMARKS ->
                    renderBookmarksPanel(
                            graphics
                    );

            case TABS ->
                    renderTabsPanel(
                            graphics
                    );

            default -> {
            }
        }
    }

    private void renderShareSheet(
            GuiGraphics graphics
    ) {
        int sheetX =
                phoneX + 14;

        int sheetY =
                phoneY
                        + PHONE_HEIGHT
                        - 260;

        int sheetWidth =
                PHONE_WIDTH - 28;

        roundedRect(
                graphics,
                sheetX,
                sheetY,
                sheetWidth,
                222,
                18,
                0xFFF7F7FA
        );

        String host =
                browser.currentUrl()
                        .isBlank()
                        ? "Start Page"
                        : new BrowserRequest(
                        browser.currentUrl()
                ).host();

        PhoneText.drawCentered(
                graphics,
                font,
                host,
                phoneX
                        + PHONE_WIDTH
                        / 2,
                sheetY + 15,
                0xFF111111
        );

        String url =
                displayAddress(
                        browser.currentUrl()
                );

        PhoneText.drawCentered(
                graphics,
                font,
                fitText(
                        url,
                        sheetWidth - 32
                ),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                sheetY + 33,
                0xFF6E6E73
        );

        int rowY =
                sheetY + 58;

        drawSheetRow(
                graphics,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                "Copy",
                "Copy website address"
        );

        rowY += 44;

        drawSheetRow(
                graphics,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                browser.isBookmarked(
                        browser.currentUrl()
                )
                        ? "Remove Bookmark"
                        : "Add Bookmark",
                "Save this website"
        );

        rowY += 44;

        drawSheetRow(
                graphics,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                "Open in New Tab",
                "Keep this page open"
        );

        int cancelY =
                sheetY + 190;

        roundedRect(
                graphics,
                sheetX + 10,
                cancelY,
                sheetWidth - 20,
                24,
                8,
                0xFFE5E5EA
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Cancel",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                cancelY + 8,
                0xFF007AFF
        );
    }

    private void drawSheetRow(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            String title,
            String subtitle
    ) {
        roundedRect(
                graphics,
                x,
                y,
                width,
                38,
                10,
                0xFFFFFFFF
        );

        PhoneText.draw(
                graphics,
                font,
                title,
                x + 12,
                y + 7,
                0xFF111111
        );

        PhoneText.draw(
                graphics,
                font,
                subtitle,
                x + 12,
                y + 21,
                0xFF8E8E93
        );
    }

    private void renderBookmarksPanel(
            GuiGraphics graphics
    ) {
        int panelX =
                phoneX + 14;

        int panelY =
                phoneY + 62;

        int panelWidth =
                PHONE_WIDTH - 28;

        int panelHeight =
                PHONE_HEIGHT - 120;

        roundedRect(
                graphics,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                18,
                0xFFF7F7FA
        );

        PhoneText.draw(
                graphics,
                font,
                "Done",
                panelX + 14,
                panelY + 15,
                0xFF007AFF
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Bookmarks",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                panelY + 15,
                0xFF111111
        );

        String current =
                browser.currentUrl();

        if (!current.isBlank()) {
            PhoneText.draw(
                    graphics,
                    font,
                    browser.isBookmarked(
                            current
                    )
                            ? "Remove Current"
                            : "Add Current",
                    panelX
                            + panelWidth
                            - 86,
                    panelY + 15,
                    0xFF007AFF
            );
        }

        List<String> bookmarks =
                browser.bookmarks();

        int listTop =
                panelY + 43;

        int listBottom =
                panelY
                        + panelHeight
                        - 12;

        graphics.enableScissor(
                panelX + 8,
                listTop,
                panelX
                        + panelWidth
                        - 8,
                listBottom
        );

        if (bookmarks.isEmpty()) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "No Bookmarks",
                    phoneX
                            + PHONE_WIDTH
                            / 2,
                    listTop + 40,
                    0xFF8E8E93
            );
        }

        for (int i = 0;
             i < bookmarks.size();
             i++) {
            int rowY =
                    listTop
                            + i * 48
                            - overlayScroll;

            if (rowY + 42
                    < listTop
                    || rowY
                    > listBottom) {
                continue;
            }

            String url =
                    bookmarks.get(i);

            roundedRect(
                    graphics,
                    panelX + 10,
                    rowY,
                    panelWidth - 20,
                    42,
                    10,
                    0xFFFFFFFF
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(
                            new BrowserRequest(
                                    url
                            ).host(),
                            panelWidth - 72
                    ),
                    panelX + 22,
                    rowY + 8,
                    0xFF111111
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(
                            url,
                            panelWidth - 72
                    ),
                    panelX + 22,
                    rowY + 23,
                    0xFF8E8E93
            );

            PhoneText.draw(
                    graphics,
                    font,
                    "x",
                    panelX
                            + panelWidth
                            - 28,
                    rowY + 15,
                    0xFFFF453A
            );
        }

        graphics.disableScissor();
    }

    private void renderTabsPanel(
            GuiGraphics graphics
    ) {
        int panelX =
                phoneX + 12;

        int panelY =
                phoneY + 54;

        int panelWidth =
                PHONE_WIDTH - 24;

        int panelHeight =
                PHONE_HEIGHT - 106;

        roundedRect(
                graphics,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                18,
                0xFFF2F2F7
        );

        PhoneText.draw(
                graphics,
                font,
                "Done",
                panelX + 14,
                panelY + 15,
                0xFF007AFF
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Tabs",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                panelY + 15,
                0xFF111111
        );

        PhoneText.draw(
                graphics,
                font,
                "+",
                panelX
                        + panelWidth
                        - 23,
                panelY + 13,
                0xFF007AFF
        );

        int listTop =
                panelY + 42;

        int listBottom =
                panelY
                        + panelHeight
                        - 10;

        graphics.enableScissor(
                panelX + 6,
                listTop,
                panelX
                        + panelWidth
                        - 6,
                listBottom
        );

        List<PhoneBrowser.TabSnapshot> tabs =
                browser.tabs();

        for (int i = 0;
             i < tabs.size();
             i++) {
            PhoneBrowser.TabSnapshot tab =
                    tabs.get(i);

            int cardY =
                    listTop
                            + i * 70
                            - overlayScroll;

            if (cardY + 62
                    < listTop
                    || cardY
                    > listBottom) {
                continue;
            }

            int background =
                    tab.active()
                            ? 0xFFFFFFFF
                            : 0xFFE5E5EA;

            roundedRect(
                    graphics,
                    panelX + 10,
                    cardY,
                    panelWidth - 20,
                    62,
                    12,
                    background
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(
                            tab.title(),
                            panelWidth - 72
                    ),
                    panelX + 22,
                    cardY + 9,
                    0xFF111111
            );

            String url =
                    tab.url()
                            .isBlank()
                            ? "Start Page"
                            : displayAddress(
                            tab.url()
                    );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(
                            url,
                            panelWidth - 72
                    ),
                    panelX + 22,
                    cardY + 27,
                    0xFF6E6E73
            );

            PhoneText.draw(
                    graphics,
                    font,
                    tab.loading()
                            ? "Loading..."
                            : tab.active()
                            ? "Current Tab"
                            : "",
                    panelX + 22,
                    cardY + 43,
                    0xFF8E8E93
            );

            PhoneText.draw(
                    graphics,
                    font,
                    "x",
                    panelX
                            + panelWidth
                            - 29,
                    cardY + 9,
                    0xFFFF453A
            );
        }

        graphics.disableScissor();
    }

    private void renderToast(
            GuiGraphics graphics
    ) {
        if (toast.isBlank()
                || System.currentTimeMillis()
                > toastUntil) {
            return;
        }

        int toastWidth =
                Math.min(
                        PHONE_WIDTH - 54,
                        PhoneText.width(
                                font,
                                toast
                        )
                                + 28
                );

        int toastX =
                phoneX
                        + (
                        PHONE_WIDTH
                                - toastWidth
                )
                        / 2;

        int toastY =
                browserPanelY - 34;

        roundedRect(
                graphics,
                toastX,
                toastY,
                toastWidth,
                25,
                12,
                0xE61C1C1E
        );

        PhoneText.drawCentered(
                graphics,
                font,
                toast,
                phoneX
                        + PHONE_WIDTH
                        / 2,
                toastY + 8,
                0xFFFFFFFF
        );
    }

    private void showToast(
            String text
    ) {
        toast =
                text == null
                        ? ""
                        : text;

        toastUntil =
                System.currentTimeMillis()
                        + 1600L;
    }

    private String fitText(
            String value,
            int maxWidth
    ) {
        String text =
                value == null
                        ? ""
                        : value;

        if (PhoneText.width(
                font,
                text
        ) <= maxWidth) {
            return text;
        }

        String suffix =
                "...";

        while (!text.isEmpty()
                && PhoneText.width(
                font,
                text + suffix
        ) > maxWidth) {
            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }

        return text + suffix;
    }

    private String displayAddress(
            String raw
    ) {
        BrowserRequest request =
                new BrowserRequest(
                        raw
                );

        return request.displayUrl();
    }

    private void openOverlay(
            BrowserOverlay next
    ) {
        overlay =
                next;

        overlayScroll = 0;

        if (addressField != null) {
            addressField.setFocused(
                    false
            );
        }

        setFocused(null);
    }

    private void closeOverlay() {
        overlay =
                BrowserOverlay.NONE;

        overlayScroll = 0;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0
                && overlay
                != BrowserOverlay.NONE) {
            return handleOverlayClick(
                    mouseX,
                    mouseY
            );
        }

        if (button == 0) {
            BrowserResponse response =
                    browser.response();

            if (response != null
                    && response
                    .openWifiSettingsSuggested()) {
                int buttonX =
                        phoneX + 45;

                int buttonY =
                        contentY
                                + contentHeight
                                - 55;

                if (inside(
                        mouseX,
                        mouseY,
                        buttonX,
                        buttonY,
                        PHONE_WIDTH - 90,
                        32
                )) {
                    minecraft.setScreen(
                            new IPhoneWifiScreen()
                    );

                    return true;
                }
            }

            for (PhoneHtmlRenderer.LinkRegion link
                    : lastRender.links()) {
                if (!link.contains(
                        mouseX,
                        mouseY
                )) {
                    continue;
                }

                String target =
                        BrowserRequest.resolve(
                                browser.currentUrl(),
                                link.href()
                        );

                browser.navigate(
                        target
                );

                if (addressField != null) {
                    addressField.setValue(
                            displayAddress(
                                    target
                            )
                    );

                    addressField.setFocused(
                            false
                    );
                }

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 23,
                    controlsY - 2,
                    31,
                    24
            )) {
                browser.back();
                forcePageRefresh();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 68,
                    controlsY - 2,
                    31,
                    24
            )) {
                browser.forward();
                forcePageRefresh();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 113,
                    controlsY - 4,
                    31,
                    28
            )) {
                openOverlay(
                        BrowserOverlay.SHARE
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 157,
                    controlsY - 4,
                    31,
                    28
            )) {
                openOverlay(
                        BrowserOverlay.BOOKMARKS
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 198,
                    controlsY - 4,
                    32,
                    28
            )) {
                openOverlay(
                        BrowserOverlay.TABS
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 15,
                    addressY,
                    PHONE_WIDTH - 30,
                    34
            )) {
                setFocused(
                        addressField
                );

                addressField.setFocused(
                        true
                );

                if (addressField
                        .getValue()
                        .isBlank()) {
                    addressField.setValue(
                            displayAddress(
                                    browser.currentUrl()
                            )
                    );
                }

                addressField.moveCursorToEnd();

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private boolean handleOverlayClick(
            double mouseX,
            double mouseY
    ) {
        return switch (overlay) {
            case SHARE ->
                    handleShareClick(
                            mouseX,
                            mouseY
                    );

            case BOOKMARKS ->
                    handleBookmarksClick(
                            mouseX,
                            mouseY
                    );

            case TABS ->
                    handleTabsClick(
                            mouseX,
                            mouseY
                    );

            default -> false;
        };
    }

    private boolean handleShareClick(
            double mouseX,
            double mouseY
    ) {
        int sheetX =
                phoneX + 14;

        int sheetY =
                phoneY
                        + PHONE_HEIGHT
                        - 260;

        int sheetWidth =
                PHONE_WIDTH - 28;

        int rowY =
                sheetY + 58;

        if (inside(
                mouseX,
                mouseY,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                38
        )) {
            String current =
                    browser.currentUrl();

            if (!current.isBlank()) {
                Minecraft.getInstance()
                        .keyboardHandler
                        .setClipboard(
                                displayAddress(
                                        current
                                )
                        );

                showToast(
                        "Address Copied"
                );
            }

            closeOverlay();
            return true;
        }

        rowY += 44;

        if (inside(
                mouseX,
                mouseY,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                38
        )) {
            String current =
                    browser.currentUrl();

            if (!current.isBlank()) {
                boolean added =
                        browser.toggleBookmark(
                                current
                        );

                showToast(
                        added
                                ? "Bookmark Added"
                                : "Bookmark Removed"
                );
            }

            closeOverlay();
            return true;
        }

        rowY += 44;

        if (inside(
                mouseX,
                mouseY,
                sheetX + 10,
                rowY,
                sheetWidth - 20,
                38
        )) {
            String current =
                    browser.currentUrl();

            boolean opened =
                    browser.newTab(
                            current
                    );

            if (opened) {
                closeOverlay();
                forcePageRefresh();
                showToast(
                        "Opened New Tab"
                );
            } else {
                showToast(
                        "Maximum Tabs Reached"
                );
            }

            return true;
        }

        int cancelY =
                sheetY + 190;

        if (inside(
                mouseX,
                mouseY,
                sheetX + 10,
                cancelY,
                sheetWidth - 20,
                24
        )) {
            closeOverlay();
            return true;
        }

        return true;
    }

    private boolean handleBookmarksClick(
            double mouseX,
            double mouseY
    ) {
        int panelX =
                phoneX + 14;

        int panelY =
                phoneY + 62;

        int panelWidth =
                PHONE_WIDTH - 28;

        int panelHeight =
                PHONE_HEIGHT - 120;

        if (inside(
                mouseX,
                mouseY,
                panelX + 8,
                panelY + 7,
                48,
                28
        )) {
            closeOverlay();
            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                panelX
                        + panelWidth
                        - 94,
                panelY + 7,
                88,
                28
        )) {
            String current =
                    browser.currentUrl();

            if (!current.isBlank()) {
                boolean added =
                        browser.toggleBookmark(
                                current
                        );

                showToast(
                        added
                                ? "Bookmark Added"
                                : "Bookmark Removed"
                );
            }

            return true;
        }

        List<String> bookmarks =
                browser.bookmarks();

        int listTop =
                panelY + 43;

        for (int i = 0;
             i < bookmarks.size();
             i++) {
            int rowY =
                    listTop
                            + i * 48
                            - overlayScroll;

            if (!inside(
                    mouseX,
                    mouseY,
                    panelX + 10,
                    rowY,
                    panelWidth - 20,
                    42
            )) {
                continue;
            }

            String url =
                    bookmarks.get(i);

            if (mouseX
                    >= panelX
                    + panelWidth
                    - 45) {
                browser.removeBookmark(
                        url
                );

                showToast(
                        "Bookmark Removed"
                );

                return true;
            }

            closeOverlay();

            browser.navigate(
                    url
            );

            forcePageRefresh();

            return true;
        }

        return true;
    }

    private boolean handleTabsClick(
            double mouseX,
            double mouseY
    ) {
        int panelX =
                phoneX + 12;

        int panelY =
                phoneY + 54;

        int panelWidth =
                PHONE_WIDTH - 24;

        if (inside(
                mouseX,
                mouseY,
                panelX + 8,
                panelY + 6,
                48,
                28
        )) {
            closeOverlay();
            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                panelX
                        + panelWidth
                        - 46,
                panelY + 6,
                40,
                28
        )) {
            boolean opened =
                    browser.newTab();

            if (opened) {
                closeOverlay();
                forcePageRefresh();
            } else {
                showToast(
                        "Maximum Tabs Reached"
                );
            }

            return true;
        }

        int listTop =
                panelY + 42;

        List<PhoneBrowser.TabSnapshot> tabs =
                browser.tabs();

        for (int i = 0;
             i < tabs.size();
             i++) {
            int cardY =
                    listTop
                            + i * 70
                            - overlayScroll;

            if (!inside(
                    mouseX,
                    mouseY,
                    panelX + 10,
                    cardY,
                    panelWidth - 20,
                    62
            )) {
                continue;
            }

            if (mouseX
                    >= panelX
                    + panelWidth
                    - 46
                    && mouseY
                    <= cardY + 30) {
                browser.closeTab(i);
                forcePageRefresh();

                return true;
            }

            browser.selectTab(i);
            closeOverlay();
            forcePageRefresh();

            return true;
        }

        return true;
    }

    private void syncAddressFromBrowser() {
        if (addressField == null) {
            return;
        }

        addressField.setValue(
                displayAddress(
                        browser.currentUrl()
                )
        );

        addressField.setFocused(
                false
        );

        setFocused(null);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (overlay
                != BrowserOverlay.NONE) {
            if (keyCode
                    == GLFW.GLFW_KEY_ESCAPE) {
                closeOverlay();
                return true;
            }

            return true;
        }

        if ((
                keyCode
                        == GLFW.GLFW_KEY_ENTER
                        || keyCode
                        == GLFW.GLFW_KEY_KP_ENTER
        )
                && addressField != null
                && addressField
                .isFocused()) {
            navigateAddressBar();
            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_ESCAPE
                && addressField != null
                && addressField
                .isFocused()) {
            addressField.setFocused(
                    false
            );

            setFocused(null);
            syncAddressFromBrowser();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (overlay
                == BrowserOverlay.BOOKMARKS) {
            int maximum =
                    Math.max(
                            0,
                            browser.bookmarks()
                                    .size()
                                    * 48
                                    - 260
                    );

            overlayScroll =
                    clampScroll(
                            overlayScroll
                                    - (int) Math.round(
                                    delta * 24.0D
                            ),
                            maximum
                    );

            return true;
        }

        if (overlay
                == BrowserOverlay.TABS) {
            int maximum =
                    Math.max(
                            0,
                            browser.tabCount()
                                    * 70
                                    - 290
                    );

            overlayScroll =
                    clampScroll(
                            overlayScroll
                                    - (int) Math.round(
                                    delta * 24.0D
                            ),
                            maximum
                    );

            return true;
        }

        if (overlay
                != BrowserOverlay.NONE) {
            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                contentX,
                contentY,
                contentWidth,
                contentHeight
        )) {
            int maximum =
                    Math.max(
                            0,
                            lastRender
                                    .totalHeight()
                                    - contentHeight
                    );

            scrollY =
                    clampScroll(
                            scrollY
                                    - (int) Math.round(
                                    delta * 20.0D
                            ),
                            maximum
                    );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
    }

    private int clampScroll(
            int value,
            int maximum
    ) {
        return Math.max(
                0,
                Math.min(
                        maximum,
                        value
                )
        );
    }

    private void navigateAddressBar() {
        if (addressField == null) {
            return;
        }

        String value =
                addressField
                        .getValue();

        browser.navigate(
                value
        );

        addressField.setFocused(
                false
        );

        setFocused(null);
        scrollY = 0;
    }

    private void drawCompass(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int size
    ) {
        int radius =
                size / 2;

        roundedRect(
                graphics,
                centerX - radius,
                centerY - radius,
                size,
                size,
                radius,
                0xFF0A84FF
        );

        int inner =
                Math.max(
                        6,
                        radius - 4
                );

        roundedRect(
                graphics,
                centerX - inner,
                centerY - inner,
                inner * 2,
                inner * 2,
                inner,
                0xFFFFFFFF
        );

        graphics.fill(
                centerX - 1,
                centerY - inner + 4,
                centerX + 1,
                centerY + 1,
                0xFFFF453A
        );

        graphics.fill(
                centerX,
                centerY,
                centerX + 2,
                centerY + inner - 3,
                0xFF0A84FF
        );
    }

    private void drawBackArrow(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.hLine(
                x - 7,
                x + 6,
                y,
                color
        );

        graphics.fill(
                x - 8,
                y - 1,
                x - 5,
                y + 2,
                color
        );

        graphics.fill(
                x - 7,
                y - 4,
                x - 5,
                y - 1,
                color
        );

        graphics.fill(
                x - 7,
                y + 2,
                x - 5,
                y + 5,
                color
        );
    }

    private void drawForwardArrow(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.hLine(
                x - 6,
                x + 7,
                y,
                color
        );

        graphics.fill(
                x + 5,
                y - 1,
                x + 8,
                y + 2,
                color
        );

        graphics.fill(
                x + 4,
                y - 4,
                x + 6,
                y - 1,
                color
        );

        graphics.fill(
                x + 4,
                y + 2,
                x + 6,
                y + 5,
                color
        );
    }

    private void drawShare(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(
                x - 6,
                y + 1,
                x + 6,
                y + 8,
                color
        );

        graphics.fill(
                x - 4,
                y + 3,
                x + 4,
                y + 7,
                0xFFF2F2F7
        );

        graphics.vLine(
                x,
                y - 7,
                y + 3,
                color
        );

        graphics.fill(
                x - 3,
                y - 5,
                x,
                y - 3,
                color
        );

        graphics.fill(
                x + 1,
                y - 5,
                x + 4,
                y - 3,
                color
        );
    }

    private void drawBookmarks(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(
                x - 6,
                y - 7,
                x + 6,
                y + 7,
                color
        );

        graphics.fill(
                x - 4,
                y - 5,
                x + 4,
                y + 3,
                0xFFF2F2F7
        );

        graphics.fill(
                x - 2,
                y + 3,
                x + 2,
                y + 7,
                color
        );
    }

    private void drawTabs(
            GuiGraphics graphics,
            int x,
            int y,
            int color,
            int count
    ) {
        graphics.fill(
                x - 7,
                y - 6,
                x + 4,
                y + 5,
                color
        );

        graphics.fill(
                x - 5,
                y - 4,
                x + 2,
                y + 3,
                0xFFF2F2F7
        );

        graphics.fill(
                x - 3,
                y - 8,
                x + 8,
                y + 3,
                color
        );

        graphics.fill(
                x - 1,
                y - 6,
                x + 6,
                y + 1,
                0xFFF2F2F7
        );

        if (count > 1) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    Integer.toString(
                            count
                    ),
                    x,
                    y - 1,
                    0xFF007AFF
            );
        }
    }

    private void drawLock(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(
                x,
                y + 4,
                x + 9,
                y + 12,
                color
        );

        graphics.fill(
                x + 2,
                y,
                x + 7,
                y + 2,
                color
        );

        graphics.fill(
                x + 1,
                y + 1,
                x + 3,
                y + 6,
                color
        );

        graphics.fill(
                x + 6,
                y + 1,
                x + 8,
                y + 6,
                color
        );
    }

    private enum BrowserOverlay {
        NONE,
        SHARE,
        BOOKMARKS,
        TABS
    }
}
