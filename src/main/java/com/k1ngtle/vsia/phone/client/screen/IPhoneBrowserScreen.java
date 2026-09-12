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
    private static final int COLOR_PHONE_SHELL = 0xFF0D0D10;
    private static final int COLOR_SURFACE = 0xFFF2F2F7;
    private static final int COLOR_SURFACE_2 = 0xFFE9E9EF;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_TEXT_PRIMARY = 0xFF111111;
    private static final int COLOR_TEXT_SECONDARY = 0xFF55565B;
    private static final int COLOR_TEXT_TERTIARY = 0xFF8E8E93;
    private static final int COLOR_ACCENT = 0xFF007AFF;
    private static final int COLOR_ACCENT_SOFT = 0x22007AFF;
    private static final int COLOR_RED = 0xFFFF5D57;
    private static final int COLOR_DIVIDER = 0xFFD6D6DC;
    private static final int COLOR_SHADOW = 0x22000000;

    private final PhoneBrowser browser = PhoneBrowser.get();

    private PhoneAddressField addressField;
    private BrowserResponse lastResponse;
    private PhoneHtmlDocument document;
    private PhoneHtmlRenderer.RenderResult lastRender = PhoneHtmlRenderer.RenderResult.empty();

    private BrowserOverlay overlay = BrowserOverlay.NONE;

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
        super(Component.literal("Browser"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 10;
        contentY = phoneY + 40;
        contentWidth = PHONE_WIDTH - 20;

        browserPanelY = phoneY + PHONE_HEIGHT - 120;
        controlsY = browserPanelY + 12;
        addressY = browserPanelY + 42;

        contentHeight = browserPanelY - contentY - 6;

        addressField = new PhoneAddressField(
                font,
                phoneX + 43,
                addressY + 6,
                PHONE_WIDTH - 82,
                22
        );

        addressField.setMaxLength(512);
        addressField.setPlaceholder("Search or enter website");
        addressField.setValue(displayAddress(browser.currentUrl()));

        addRenderableWidget(addressField);
    }

    @Override
    public void tick() {
        super.tick();

        if (addressField == null) {
            return;
        }

        if (overlay != BrowserOverlay.NONE) {
            addressField.setFocused(false);
            return;
        }

        if (!addressField.isFocused()) {
            String current = displayAddress(browser.currentUrl());
            if (!addressField.getValue().equals(current)) {
                addressField.setValue(current);
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
        renderPhoneShell(graphics, COLOR_PHONE_SHELL);
        renderStatusBar(graphics);

        refreshDocument();

        drawSoftShadow(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight + 2,
                18
        );

        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                16,
                COLOR_CARD
        );

        if (browser.loading()) {
            renderLoading(graphics);
        } else if (browser.response() == null) {
            renderStartPage(graphics);
        } else {
            BrowserResponse response = browser.response();

            if (response.statusCode() == 0) {
                renderNetworkError(graphics, response);
            } else {
                renderWebsite(graphics, response);
            }
        }

        renderBrowserChrome(graphics);
        renderHomeIndicator(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (overlay != BrowserOverlay.NONE) {
            renderOverlay(graphics, mouseX, mouseY);
        }

        renderToast(graphics);
    }

    private void refreshDocument() {
        BrowserResponse response = browser.response();

        if (response == lastResponse) {
            return;
        }

        lastResponse = response;
        scrollY = 0;

        if (response == null || response.statusCode() == 0) {
            document = null;
            lastRender = PhoneHtmlRenderer.RenderResult.empty();
            return;
        }

        document = PhoneHtmlDocument.from(response);
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

        lastRender = PhoneHtmlRenderer.render(
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
            roundedRect(
                    graphics,
                    contentX + 8,
                    contentY + 8,
                    contentWidth - 16,
                    28,
                    10,
                    COLOR_RED
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    response.statusCode() + " " + response.reason(),
                    phoneX + PHONE_WIDTH / 2,
                    contentY + 17,
                    0xFFFFFFFF
            );
        }
    }

    private void renderLoading(
            GuiGraphics graphics
    ) {
        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                16,
                0xFFFAFAFC
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int centerY = contentY + contentHeight / 2 - 22;

        drawCompass(graphics, centerX, centerY - 28, 42);

        PhoneText.drawCentered(
                graphics,
                font,
                "Loading Website",
                centerX,
                centerY + 20,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Please wait…",
                centerX,
                centerY + 38,
                COLOR_TEXT_TERTIARY
        );

        int barX = phoneX + 38;
        int barY = centerY + 60;
        int barWidth = PHONE_WIDTH - 76;

        roundedRect(
                graphics,
                barX,
                barY,
                barWidth,
                5,
                3,
                0xFFE1E1E7
        );

        int animated = (int) (System.currentTimeMillis() / 12L % (barWidth + 24)) - 24;
        int fillStart = Math.max(0, animated);
        int fillEnd = Math.min(barWidth, animated + 24);

        if (fillEnd > fillStart) {
            roundedRect(
                    graphics,
                    barX + fillStart,
                    barY,
                    fillEnd - fillStart,
                    5,
                    3,
                    COLOR_ACCENT
            );
        }
    }

    private void renderStartPage(
            GuiGraphics graphics
    ) {
        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                16,
                0xFFFBFBFD
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int topY = contentY + 42;

        drawCompass(graphics, centerX, topY + 14, 46);

        PhoneText.drawCentered(
                graphics,
                font,
                "Safari",
                centerX,
                topY + 54,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Clean browser for VS:IA websites",
                centerX,
                topY + 76,
                COLOR_TEXT_SECONDARY
        );

        roundedRect(
                graphics,
                phoneX + 34,
                topY + 104,
                PHONE_WIDTH - 68,
                74,
                16,
                COLOR_SURFACE
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Quick Start",
                centerX,
                topY + 116,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "• Type a website in the address bar",
                centerX,
                topY + 136,
                COLOR_TEXT_SECONDARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "• Use host@rack-ip for direct rack access",
                centerX,
                topY + 151,
                COLOR_TEXT_SECONDARY
        );

        roundedRect(
                graphics,
                phoneX + 34,
                topY + 192,
                PHONE_WIDTH - 68,
                82,
                16,
                COLOR_CARD
        );
        drawSoftShadow(
                graphics,
                phoneX + 34,
                topY + 192,
                PHONE_WIDTH - 68,
                82,
                16
        );

        PhoneText.draw(
                graphics,
                font,
                "Examples",
                phoneX + 48,
                topY + 204,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.draw(
                graphics,
                font,
                "a.w128lab.com",
                phoneX + 48,
                topY + 223,
                COLOR_ACCENT
        );

        PhoneText.draw(
                graphics,
                font,
                "a.w128lab.com@192.168.1.2",
                phoneX + 48,
                topY + 239,
                COLOR_ACCENT
        );

        PhoneText.draw(
                graphics,
                font,
                "192.168.1.2/a.w128lab.com",
                phoneX + 48,
                topY + 255,
                COLOR_ACCENT
        );
    }

    private void renderNetworkError(
            GuiGraphics graphics,
            BrowserResponse response
    ) {
        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                16,
                0xFFFBFBFD
        );

        roundedRect(
                graphics,
                contentX + 12,
                contentY + 16,
                contentWidth - 24,
                28,
                10,
                COLOR_RED
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Safari cannot open the page",
                phoneX + PHONE_WIDTH / 2,
                contentY + 56,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                response.statusCode() + " " + response.reason(),
                phoneX + PHONE_WIDTH / 2,
                contentY + 22,
                0xFFFFFFFF
        );

        int y = contentY + 82;

        for (String line : response.reason().split("\n")) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    line,
                    phoneX + PHONE_WIDTH / 2,
                    y,
                    COLOR_TEXT_SECONDARY
            );
            y += 15;
        }

        if (!response.body().isBlank()) {
            y += 10;

            roundedRect(
                    graphics,
                    phoneX + 30,
                    y - 6,
                    PHONE_WIDTH - 60,
                    72,
                    14,
                    COLOR_SURFACE
            );

            for (String line : response.body().split("\n")) {
                PhoneText.drawCentered(
                        graphics,
                        font,
                        line,
                        phoneX + PHONE_WIDTH / 2,
                        y,
                        COLOR_TEXT_SECONDARY
                );
                y += 15;
            }
        }

        if (response.openWifiSettingsSuggested()) {
            int buttonX = phoneX + 46;
            int buttonY = contentY + contentHeight - 58;

            roundedRect(
                    graphics,
                    buttonX,
                    buttonY,
                    PHONE_WIDTH - 92,
                    34,
                    12,
                    COLOR_ACCENT
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    "Open Wi-Fi Settings",
                    phoneX + PHONE_WIDTH / 2,
                    buttonY + 12,
                    0xFFFFFFFF
            );
        }
    }

    private void renderBrowserChrome(
            GuiGraphics graphics
    ) {
        drawSoftShadow(
                graphics,
                phoneX + 14,
                browserPanelY + 4,
                PHONE_WIDTH - 28,
                88,
                18
        );

        roundedRect(
                graphics,
                phoneX + 14,
                browserPanelY + 4,
                PHONE_WIDTH - 28,
                88,
                18,
                0xF7FFFFFF
        );

        roundedRect(
                graphics,
                phoneX + 24,
                controlsY + 2,
                34,
                28,
                11,
                browser.canGoBack() ? COLOR_ACCENT_SOFT : 0x00000000
        );

        roundedRect(
                graphics,
                phoneX + 68,
                controlsY + 2,
                34,
                28,
                11,
                browser.canGoForward() ? COLOR_ACCENT_SOFT : 0x00000000
        );

        drawBackArrow(
                graphics,
                phoneX + 41,
                controlsY + 16,
                browser.canGoBack() ? COLOR_ACCENT : 0xFFBCBCC4
        );

        drawForwardArrow(
                graphics,
                phoneX + 85,
                controlsY + 16,
                browser.canGoForward() ? COLOR_ACCENT : 0xFFBCBCC4
        );

        drawShare(graphics, phoneX + 128, controlsY + 16, COLOR_ACCENT);
        drawBookmarks(graphics, phoneX + 172, controlsY + 16, COLOR_ACCENT);
        drawTabs(graphics, phoneX + 214, controlsY + 16, COLOR_ACCENT, browser.tabCount());

        roundedRect(
                graphics,
                phoneX + 22,
                addressY + 1,
                PHONE_WIDTH - 44,
                34,
                15,
                COLOR_SURFACE_2
        );

        graphics.vLine(phoneX + 43, addressY + 8, addressY + 26, COLOR_DIVIDER);
        drawLock(graphics, phoneX + 28, addressY + 11, 0xFF6B6C72);
    }

    private void renderOverlay(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.fill(
                phoneX + 6,
                phoneY + 36,
                phoneX + PHONE_WIDTH - 6,
                phoneY + PHONE_HEIGHT - 22,
                0x66000000
        );

        switch (overlay) {
            case SHARE -> renderShareSheet(graphics);
            case BOOKMARKS -> renderBookmarksPanel(graphics);
            case TABS -> renderTabsPanel(graphics);
            default -> {
            }
        }
    }

    private void renderShareSheet(
            GuiGraphics graphics
    ) {
        int sheetX = phoneX + 16;
        int sheetY = phoneY + PHONE_HEIGHT - 264;
        int sheetWidth = PHONE_WIDTH - 32;

        drawSoftShadow(graphics, sheetX, sheetY, sheetWidth, 226, 20);
        roundedRect(graphics, sheetX, sheetY, sheetWidth, 226, 20, 0xFFF8F8FB);

        String host = browser.currentUrl().isBlank()
                ? "Start Page"
                : new BrowserRequest(browser.currentUrl()).host();

        PhoneText.drawCentered(
                graphics,
                font,
                host,
                phoneX + PHONE_WIDTH / 2,
                sheetY + 16,
                COLOR_TEXT_PRIMARY
        );

        PhoneText.drawCentered(
                graphics,
                font,
                fitText(displayAddress(browser.currentUrl()), sheetWidth - 42),
                phoneX + PHONE_WIDTH / 2,
                sheetY + 34,
                COLOR_TEXT_TERTIARY
        );

        int rowY = sheetY + 58;

        drawSheetRow(graphics, sheetX + 12, rowY, sheetWidth - 24, "Copy Link", "Copy website address");
        rowY += 46;
        drawSheetRow(graphics, sheetX + 12, rowY, sheetWidth - 24,
                browser.isBookmarked(browser.currentUrl()) ? "Remove Bookmark" : "Add Bookmark",
                "Save this page");
        rowY += 46;
        drawSheetRow(graphics, sheetX + 12, rowY, sheetWidth - 24, "Open in New Tab", "Keep this page open");

        int cancelY = sheetY + 194;

        roundedRect(
                graphics,
                sheetX + 12,
                cancelY,
                sheetWidth - 24,
                24,
                9,
                COLOR_SURFACE_2
        );

        PhoneText.drawCentered(
                graphics,
                font,
                "Cancel",
                phoneX + PHONE_WIDTH / 2,
                cancelY + 8,
                COLOR_ACCENT
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
        roundedRect(graphics, x, y, width, 40, 12, COLOR_CARD);
        PhoneText.draw(graphics, font, title, x + 13, y + 8, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, subtitle, x + 13, y + 22, COLOR_TEXT_TERTIARY);
    }

    private void renderBookmarksPanel(
            GuiGraphics graphics
    ) {
        int panelX = phoneX + 16;
        int panelY = phoneY + 58;
        int panelWidth = PHONE_WIDTH - 32;
        int panelHeight = PHONE_HEIGHT - 116;

        drawSoftShadow(graphics, panelX, panelY, panelWidth, panelHeight, 20);
        roundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 20, 0xFFF9F9FB);

        PhoneText.draw(graphics, font, "Done", panelX + 16, panelY + 16, COLOR_ACCENT);
        PhoneText.drawCentered(graphics, font, "Bookmarks", phoneX + PHONE_WIDTH / 2, panelY + 16, COLOR_TEXT_PRIMARY);

        String current = browser.currentUrl();

        if (!current.isBlank()) {
            PhoneText.draw(
                    graphics,
                    font,
                    browser.isBookmarked(current) ? "Remove Current" : "Add Current",
                    panelX + panelWidth - 86,
                    panelY + 16,
                    COLOR_ACCENT
            );
        }

        int listTop = panelY + 44;
        int listBottom = panelY + panelHeight - 12;

        graphics.enableScissor(panelX + 8, listTop, panelX + panelWidth - 8, listBottom);

        List<String> bookmarks = browser.bookmarks();

        if (bookmarks.isEmpty()) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    "No Bookmarks Yet",
                    phoneX + PHONE_WIDTH / 2,
                    listTop + 44,
                    COLOR_TEXT_TERTIARY
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    "Save a page from the share menu.",
                    phoneX + PHONE_WIDTH / 2,
                    listTop + 60,
                    COLOR_TEXT_TERTIARY
            );
        }

        for (int i = 0; i < bookmarks.size(); i++) {
            int rowY = listTop + i * 50 - overlayScroll;

            if (rowY + 44 < listTop || rowY > listBottom) {
                continue;
            }

            String url = bookmarks.get(i);

            roundedRect(
                    graphics,
                    panelX + 10,
                    rowY,
                    panelWidth - 20,
                    44,
                    12,
                    COLOR_CARD
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(new BrowserRequest(url).host(), panelWidth - 80),
                    panelX + 22,
                    rowY + 8,
                    COLOR_TEXT_PRIMARY
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(url, panelWidth - 80),
                    panelX + 22,
                    rowY + 24,
                    COLOR_TEXT_TERTIARY
            );

            PhoneText.draw(
                    graphics,
                    font,
                    "Delete",
                    panelX + panelWidth - 54,
                    rowY + 16,
                    COLOR_RED
            );
        }

        graphics.disableScissor();
    }

    private void renderTabsPanel(
            GuiGraphics graphics
    ) {
        int panelX = phoneX + 16;
        int panelY = phoneY + 54;
        int panelWidth = PHONE_WIDTH - 32;
        int panelHeight = PHONE_HEIGHT - 108;

        drawSoftShadow(graphics, panelX, panelY, panelWidth, panelHeight, 20);
        roundedRect(graphics, panelX, panelY, panelWidth, panelHeight, 20, 0xFFF4F4F8);

        PhoneText.draw(graphics, font, "Done", panelX + 16, panelY + 16, COLOR_ACCENT);
        PhoneText.drawCentered(graphics, font, "Tabs", phoneX + PHONE_WIDTH / 2, panelY + 16, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, "New", panelX + panelWidth - 32, panelY + 16, COLOR_ACCENT);

        int listTop = panelY + 44;
        int listBottom = panelY + panelHeight - 12;

        graphics.enableScissor(panelX + 8, listTop, panelX + panelWidth - 8, listBottom);

        List<PhoneBrowser.TabSnapshot> tabs = browser.tabs();

        for (int i = 0; i < tabs.size(); i++) {
            PhoneBrowser.TabSnapshot tab = tabs.get(i);

            int cardY = listTop + i * 74 - overlayScroll;

            if (cardY + 64 < listTop || cardY > listBottom) {
                continue;
            }

            int cardColor = tab.active() ? COLOR_CARD : 0xFFEAEAF0;

            roundedRect(
                    graphics,
                    panelX + 10,
                    cardY,
                    panelWidth - 20,
                    64,
                    14,
                    cardColor
            );

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(tab.title(), panelWidth - 92),
                    panelX + 22,
                    cardY + 10,
                    COLOR_TEXT_PRIMARY
            );

            String shownUrl = tab.url().isBlank() ? "Start Page" : displayAddress(tab.url());

            PhoneText.draw(
                    graphics,
                    font,
                    fitText(shownUrl, panelWidth - 92),
                    panelX + 22,
                    cardY + 27,
                    COLOR_TEXT_TERTIARY
            );

            String state = tab.loading() ? "Loading…" : (tab.active() ? "Current Tab" : "Tap to switch");

            PhoneText.draw(
                    graphics,
                    font,
                    state,
                    panelX + 22,
                    cardY + 44,
                    COLOR_TEXT_TERTIARY
            );

            PhoneText.draw(
                    graphics,
                    font,
                    "Close",
                    panelX + panelWidth - 46,
                    cardY + 10,
                    COLOR_RED
            );
        }

        graphics.disableScissor();
    }

    private void renderToast(
            GuiGraphics graphics
    ) {
        if (toast.isBlank() || System.currentTimeMillis() > toastUntil) {
            return;
        }

        int toastWidth = Math.min(PHONE_WIDTH - 58, PhoneText.width(font, toast) + 30);
        int toastX = phoneX + (PHONE_WIDTH - toastWidth) / 2;
        int toastY = browserPanelY - 36;

        roundedRect(
                graphics,
                toastX,
                toastY,
                toastWidth,
                25,
                12,
                0xE61D1D20
        );

        PhoneText.drawCentered(
                graphics,
                font,
                toast,
                phoneX + PHONE_WIDTH / 2,
                toastY + 8,
                0xFFFFFFFF
        );
    }

    private void showToast(String text) {
        toast = text == null ? "" : text;
        toastUntil = System.currentTimeMillis() + 1600L;
    }

    private String fitText(
            String value,
            int maxWidth
    ) {
        String text = value == null ? "" : value;

        if (PhoneText.width(font, text) <= maxWidth) {
            return text;
        }

        String suffix = "...";

        while (!text.isEmpty() && PhoneText.width(font, text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text + suffix;
    }

    private String displayAddress(String raw) {
        BrowserRequest request = new BrowserRequest(raw);
        return request.displayUrl();
    }

    private void openOverlay(BrowserOverlay next) {
        overlay = next;
        overlayScroll = 0;

        if (addressField != null) {
            addressField.setFocused(false);
        }

        setFocused(null);
    }

    private void closeOverlay() {
        overlay = BrowserOverlay.NONE;
        overlayScroll = 0;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0 && overlay != BrowserOverlay.NONE) {
            return handleOverlayClick(mouseX, mouseY);
        }

        if (button == 0) {
            BrowserResponse response = browser.response();

            if (response != null && response.openWifiSettingsSuggested()) {
                int buttonX = phoneX + 46;
                int buttonY = contentY + contentHeight - 58;

                if (inside(mouseX, mouseY, buttonX, buttonY, PHONE_WIDTH - 92, 34)) {
                    minecraft.setScreen(new IPhoneWifiScreen());
                    return true;
                }
            }

            for (PhoneHtmlRenderer.LinkRegion link : lastRender.links()) {
                if (!link.contains(mouseX, mouseY)) {
                    continue;
                }

                String target = BrowserRequest.resolve(browser.currentUrl(), link.href());
                browser.navigate(target);

                if (addressField != null) {
                    addressField.setValue(displayAddress(target));
                    addressField.setFocused(false);
                }

                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 24, controlsY + 2, 34, 28)) {
                browser.back();
                forcePageRefresh();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 68, controlsY + 2, 34, 28)) {
                browser.forward();
                forcePageRefresh();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 112, controlsY + 2, 32, 28)) {
                openOverlay(BrowserOverlay.SHARE);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 157, controlsY + 2, 32, 28)) {
                openOverlay(BrowserOverlay.BOOKMARKS);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 198, controlsY + 2, 34, 28)) {
                openOverlay(BrowserOverlay.TABS);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 22, addressY + 1, PHONE_WIDTH - 44, 34)) {
                setFocused(addressField);
                addressField.setFocused(true);

                if (addressField.getValue().isBlank()) {
                    addressField.setValue(displayAddress(browser.currentUrl()));
                }

                addressField.moveCursorToEnd();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleOverlayClick(
            double mouseX,
            double mouseY
    ) {
        return switch (overlay) {
            case SHARE -> handleShareClick(mouseX, mouseY);
            case BOOKMARKS -> handleBookmarksClick(mouseX, mouseY);
            case TABS -> handleTabsClick(mouseX, mouseY);
            default -> false;
        };
    }

    private boolean handleShareClick(
            double mouseX,
            double mouseY
    ) {
        int sheetX = phoneX + 16;
        int sheetY = phoneY + PHONE_HEIGHT - 264;
        int sheetWidth = PHONE_WIDTH - 32;

        int rowY = sheetY + 58;

        if (inside(mouseX, mouseY, sheetX + 12, rowY, sheetWidth - 24, 40)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(displayAddress(current));
                showToast("Address Copied");
            }

            closeOverlay();
            return true;
        }

        rowY += 46;

        if (inside(mouseX, mouseY, sheetX + 12, rowY, sheetWidth - 24, 40)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                boolean added = browser.toggleBookmark(current);
                showToast(added ? "Bookmark Added" : "Bookmark Removed");
            }

            closeOverlay();
            return true;
        }

        rowY += 46;

        if (inside(mouseX, mouseY, sheetX + 12, rowY, sheetWidth - 24, 40)) {
            String current = browser.currentUrl();
            boolean opened = browser.newTab(current);

            if (opened) {
                closeOverlay();
                forcePageRefresh();
                showToast("Opened New Tab");
            } else {
                showToast("Maximum Tabs Reached");
            }

            return true;
        }

        int cancelY = sheetY + 194;

        if (inside(mouseX, mouseY, sheetX + 12, cancelY, sheetWidth - 24, 24)) {
            closeOverlay();
            return true;
        }

        return true;
    }

    private boolean handleBookmarksClick(
            double mouseX,
            double mouseY
    ) {
        int panelX = phoneX + 16;
        int panelY = phoneY + 58;
        int panelWidth = PHONE_WIDTH - 32;

        if (inside(mouseX, mouseY, panelX + 10, panelY + 8, 48, 24)) {
            closeOverlay();
            return true;
        }

        if (inside(mouseX, mouseY, panelX + panelWidth - 102, panelY + 8, 92, 24)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                boolean added = browser.toggleBookmark(current);
                showToast(added ? "Bookmark Added" : "Bookmark Removed");
            }

            return true;
        }

        List<String> bookmarks = browser.bookmarks();
        int listTop = panelY + 44;

        for (int i = 0; i < bookmarks.size(); i++) {
            int rowY = listTop + i * 50 - overlayScroll;

            if (!inside(mouseX, mouseY, panelX + 10, rowY, panelWidth - 20, 44)) {
                continue;
            }

            String url = bookmarks.get(i);

            if (mouseX >= panelX + panelWidth - 66) {
                browser.removeBookmark(url);
                showToast("Bookmark Removed");
                return true;
            }

            closeOverlay();
            browser.navigate(url);
            forcePageRefresh();
            return true;
        }

        return true;
    }

    private boolean handleTabsClick(
            double mouseX,
            double mouseY
    ) {
        int panelX = phoneX + 16;
        int panelY = phoneY + 54;
        int panelWidth = PHONE_WIDTH - 32;

        if (inside(mouseX, mouseY, panelX + 10, panelY + 8, 48, 24)) {
            closeOverlay();
            return true;
        }

        if (inside(mouseX, mouseY, panelX + panelWidth - 44, panelY + 8, 34, 24)) {
            boolean opened = browser.newTab();

            if (opened) {
                closeOverlay();
                forcePageRefresh();
            } else {
                showToast("Maximum Tabs Reached");
            }

            return true;
        }

        int listTop = panelY + 44;
        List<PhoneBrowser.TabSnapshot> tabs = browser.tabs();

        for (int i = 0; i < tabs.size(); i++) {
            int cardY = listTop + i * 74 - overlayScroll;

            if (!inside(mouseX, mouseY, panelX + 10, cardY, panelWidth - 20, 64)) {
                continue;
            }

            if (mouseX >= panelX + panelWidth - 62 && mouseY <= cardY + 28) {
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

        addressField.setValue(displayAddress(browser.currentUrl()));
        addressField.setFocused(false);
        setFocused(null);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (overlay != BrowserOverlay.NONE) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeOverlay();
                return true;
            }

            return true;
        }

        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && addressField != null
                && addressField.isFocused()) {
            navigateAddressBar();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && addressField != null
                && addressField.isFocused()) {
            addressField.setFocused(false);
            setFocused(null);
            syncAddressFromBrowser();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_F5) {
            browser.reload();
            forcePageRefresh();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (overlay == BrowserOverlay.BOOKMARKS) {
            int maximum = Math.max(0, browser.bookmarks().size() * 50 - 270);
            overlayScroll = clampScroll(overlayScroll - (int) Math.round(delta * 24.0D), maximum);
            return true;
        }

        if (overlay == BrowserOverlay.TABS) {
            int maximum = Math.max(0, browser.tabCount() * 74 - 294);
            overlayScroll = clampScroll(overlayScroll - (int) Math.round(delta * 24.0D), maximum);
            return true;
        }

        if (overlay != BrowserOverlay.NONE) {
            return true;
        }

        if (inside(mouseX, mouseY, contentX, contentY, contentWidth, contentHeight)) {
            int maximum = Math.max(0, lastRender.totalHeight() - contentHeight);
            scrollY = clampScroll(scrollY - (int) Math.round(delta * 20.0D), maximum);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int clampScroll(
            int value,
            int maximum
    ) {
        return Math.max(0, Math.min(maximum, value));
    }

    private void navigateAddressBar() {
        if (addressField == null) {
            return;
        }

        browser.navigate(addressField.getValue());

        addressField.setFocused(false);
        setFocused(null);
        scrollY = 0;
    }

    private void drawCompass(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int size
    ) {
        int radius = size / 2;

        roundedRect(
                graphics,
                centerX - radius,
                centerY - radius,
                size,
                size,
                radius,
                COLOR_ACCENT
        );

        int inner = Math.max(6, radius - 5);

        roundedRect(
                graphics,
                centerX - inner,
                centerY - inner,
                inner * 2,
                inner * 2,
                inner,
                COLOR_CARD
        );

        graphics.fill(centerX - 1, centerY - inner + 5, centerX + 1, centerY + 1, 0xFFFF5B52);
        graphics.fill(centerX, centerY, centerX + 2, centerY + inner - 4, COLOR_ACCENT);
    }

    private void drawBackArrow(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.hLine(x - 7, x + 6, y, color);
        graphics.fill(x - 8, y - 1, x - 5, y + 2, color);
        graphics.fill(x - 7, y - 4, x - 5, y - 1, color);
        graphics.fill(x - 7, y + 2, x - 5, y + 5, color);
    }

    private void drawForwardArrow(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.hLine(x - 6, x + 7, y, color);
        graphics.fill(x + 5, y - 1, x + 8, y + 2, color);
        graphics.fill(x + 4, y - 4, x + 6, y - 1, color);
        graphics.fill(x + 4, y + 2, x + 6, y + 5, color);
    }

    private void drawShare(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 6, y + 1, x + 6, y + 9, color);
        graphics.fill(x - 4, y + 3, x + 4, y + 8, COLOR_CARD);
        graphics.vLine(x, y - 8, y + 3, color);
        graphics.fill(x - 3, y - 5, x, y - 3, color);
        graphics.fill(x + 1, y - 5, x + 4, y - 3, color);
    }

    private void drawBookmarks(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 6, y - 8, x + 6, y + 7, color);
        graphics.fill(x - 4, y - 6, x + 4, y + 3, COLOR_CARD);
        graphics.fill(x - 2, y + 3, x + 2, y + 7, color);
    }

    private void drawTabs(
            GuiGraphics graphics,
            int x,
            int y,
            int color,
            int count
    ) {
        graphics.fill(x - 7, y - 6, x + 4, y + 5, color);
        graphics.fill(x - 5, y - 4, x + 2, y + 3, COLOR_CARD);
        graphics.fill(x - 3, y - 8, x + 8, y + 3, color);
        graphics.fill(x - 1, y - 6, x + 6, y + 1, COLOR_CARD);

        if (count > 1) {
            PhoneText.drawCentered(
                    graphics,
                    font,
                    Integer.toString(count),
                    x,
                    y - 1,
                    color
            );
        }
    }

    private void drawLock(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x, y + 4, x + 9, y + 12, color);
        graphics.fill(x + 2, y, x + 7, y + 2, color);
        graphics.fill(x + 1, y + 1, x + 3, y + 6, color);
        graphics.fill(x + 6, y + 1, x + 8, y + 6, color);
    }

    private void drawSoftShadow(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius
    ) {
        roundedRect(graphics, x + 1, y + 2, width, height, radius, COLOR_SHADOW);
    }

    private enum BrowserOverlay {
        NONE,
        SHARE,
        BOOKMARKS,
        TABS
    }
}
