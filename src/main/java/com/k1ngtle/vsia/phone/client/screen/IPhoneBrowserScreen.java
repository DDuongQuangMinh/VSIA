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

import java.util.ArrayList;
import java.util.List;

public class IPhoneBrowserScreen extends IPhoneScreen {
    private static final int COLOR_PHONE_SHELL = 0xFF0B0B0F;
    private static final int COLOR_PAGE = 0xFFF8F8FA;
    private static final int COLOR_CARD = 0xFFFFFFFF;
    private static final int COLOR_CARD_ALT = 0xFFF1F1F5;
    private static final int COLOR_TEXT_PRIMARY = 0xFF111111;
    private static final int COLOR_TEXT_SECONDARY = 0xFF4B4C51;
    private static final int COLOR_TEXT_TERTIARY = 0xFF8C8D93;
    private static final int COLOR_ACCENT = 0xFF2D7CFF;
    private static final int COLOR_BORDER = 0xFFE6E6EB;
    private static final int COLOR_RED = 0xFFFF6157;
    private static final int COLOR_SHADOW = 0x18000000;
    private static final int COLOR_CHROME = 0xF3FFFFFF;

    private final PhoneBrowser browser = PhoneBrowser.get();

    private PhoneAddressField addressField;
    private BrowserResponse lastResponse;
    private PhoneHtmlDocument document;
    private PhoneHtmlRenderer.RenderResult lastRender = PhoneHtmlRenderer.RenderResult.empty();

    private BrowserOverlay overlay = BrowserOverlay.NONE;

    private int overlayScroll;
    private int scrollY;
    private int startPageTotalHeight;

    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    private int browserPanelY;
    private int addressY;
    private int toolbarY;

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

        browserPanelY = phoneY + PHONE_HEIGHT - 122;
        addressY = browserPanelY + 12;
        toolbarY = browserPanelY + 53;

        contentHeight = browserPanelY - contentY - 8;

        addressField = new PhoneAddressField(
                font,
                phoneX + 52,
                addressY + 8,
                PHONE_WIDTH - 104,
                20
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

        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                18,
                browser.response() == null ? COLOR_PAGE : COLOR_CARD
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

        renderBottomChrome(graphics);
        renderHomeIndicator(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (overlay != BrowserOverlay.NONE) {
            renderOverlay(graphics);
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

        roundedRect(
                graphics,
                contentX,
                contentY,
                contentWidth,
                contentHeight,
                18,
                0xFFFFFFFF
        );

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
                    contentX + 10,
                    contentY + 10,
                    contentWidth - 20,
                    28,
                    10,
                    COLOR_RED
            );

            PhoneText.drawCentered(
                    graphics,
                    font,
                    response.statusCode() + " " + response.reason(),
                    phoneX + PHONE_WIDTH / 2,
                    contentY + 19,
                    0xFFFFFFFF
            );
        }
    }

    private void renderLoading(
            GuiGraphics graphics
    ) {
        roundedRect(graphics, contentX, contentY, contentWidth, contentHeight, 18, COLOR_PAGE);

        int centerX = phoneX + PHONE_WIDTH / 2;
        int centerY = contentY + contentHeight / 2 - 10;

        drawCompassIcon(graphics, centerX - 20, centerY - 24, 40);

        PhoneText.drawCentered(graphics, font, "Loading Website", centerX, centerY + 28, COLOR_TEXT_PRIMARY);
        PhoneText.drawCentered(graphics, font, "Please wait...", centerX, centerY + 45, COLOR_TEXT_TERTIARY);

        int barX = phoneX + 40;
        int barY = centerY + 64;
        int barWidth = PHONE_WIDTH - 80;

        roundedRect(graphics, barX, barY, barWidth, 5, 3, 0xFFE4E4EA);

        int anim = (int) (System.currentTimeMillis() / 15L % (barWidth + 30)) - 30;
        int fillStart = Math.max(0, anim);
        int fillEnd = Math.min(barWidth, anim + 32);

        if (fillEnd > fillStart) {
            roundedRect(graphics, barX + fillStart, barY, fillEnd - fillStart, 5, 3, COLOR_ACCENT);
        }
    }

    private void renderStartPage(
            GuiGraphics graphics
    ) {
        roundedRect(graphics, contentX, contentY, contentWidth, contentHeight, 18, COLOR_PAGE);

        graphics.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        int innerX = contentX + 12;
        int innerWidth = contentWidth - 24;
        int y = contentY + 16 - scrollY;

        PhoneText.draw(graphics, font, "Favorites", innerX, y, COLOR_TEXT_PRIMARY);
        y += 20;
        y = drawFavoritesGrid(graphics, innerX, innerWidth, y);

        y += 10;
        PhoneText.draw(graphics, font, "Frequently Visited", innerX, y, COLOR_TEXT_PRIMARY);
        y += 20;
        y = drawFrequentlyVisitedGrid(graphics, innerX, innerWidth, y);

        y += 10;
        PhoneText.draw(graphics, font, "Shared with You", innerX, y, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, "Show All", contentX + contentWidth - 64, y, COLOR_ACCENT);
        PhoneText.draw(graphics, font, ">", contentX + contentWidth - 18, y, COLOR_ACCENT);
        y += 20;
        y = drawSharedWithYouCards(graphics, innerX, innerWidth, y);

        startPageTotalHeight = Math.max(0, (y + scrollY + 16) - contentY);

        graphics.disableScissor();
    }

    private int drawFavoritesGrid(
            GuiGraphics graphics,
            int startX,
            int availableWidth,
            int startY
    ) {
        List<QuickSite> favorites = new ArrayList<>();
        favorites.add(new QuickSite("Saved\nTabs", 0xFFEFF2F8, COLOR_ACCENT, QuickIcon.TABS, QuickAction.SAVED_TABS));
        favorites.add(new QuickSite("Apple", 0xFFF6F6F8, 0xFF8E8E93, QuickIcon.CIRCLE, QuickAction.NONE));
        favorites.add(new QuickSite("Bing", 0xFFF6F6F8, 0xFF2E9BFF, QuickIcon.B, QuickAction.NONE));
        favorites.add(new QuickSite("Google", 0xFFF6F6F8, 0xFF4285F4, QuickIcon.G, QuickAction.NONE));
        favorites.add(new QuickSite("Yahoo", 0xFF4C1FB8, 0xFFFFFFFF, QuickIcon.TEXT, QuickAction.NONE));
        favorites.add(new QuickSite("VSIA", 0xFF2D7CFF, 0xFFFFFFFF, QuickIcon.OMEGA, QuickAction.NONE));

        GridMetrics grid = createGridMetrics(availableWidth, 4, 40, 10);
        int cardHeight = 40;
        int rowGap = 22;
        int textOffset = 6;

        for (int i = 0; i < favorites.size(); i++) {
            int row = i / 4;
            int col = i % 4;

            int x = startX + col * (grid.cellWidth + grid.gap);
            int y = startY + row * (cardHeight + rowGap + 20);

            drawQuickSiteSquare(
                    graphics,
                    x,
                    y,
                    grid.cellWidth,
                    cardHeight,
                    favorites.get(i)
            );

            drawMultiLineCentered(
                    graphics,
                    favorites.get(i).title(),
                    x + grid.cellWidth / 2,
                    y + cardHeight + textOffset,
                    COLOR_TEXT_PRIMARY
            );
        }

        int rows = (favorites.size() + 3) / 4;
        return startY + rows * cardHeight + (rows - 1) * (rowGap + 20) + 20;
    }

    private int drawFrequentlyVisitedGrid(
            GuiGraphics graphics,
            int startX,
            int availableWidth,
            int startY
    ) {
        List<QuickSite> frequent = new ArrayList<>();
        frequent.add(new QuickSite("Lab", 0xFFC03A2B, 0xFFFFFFFF, QuickIcon.TOWER, QuickAction.QUICK_DOMAIN_LAB));
        frequent.add(new QuickSite("Server", 0xFFE0552E, 0xFFFFFFFF, QuickIcon.V, QuickAction.QUICK_DOMAIN_SERVER));
        frequent.add(new QuickSite("Rack", 0xFFF0F0F1, 0xFF5B5B60, QuickIcon.RACK, QuickAction.QUICK_DOMAIN_RACK));
        frequent.add(new QuickSite("Video", 0xFFF04C3E, 0xFFFFFFFF, QuickIcon.PLAY, QuickAction.NONE));

        GridMetrics grid = createGridMetrics(availableWidth, 4, 40, 10);
        int cardHeight = 40;
        int textOffset = 6;

        for (int i = 0; i < frequent.size(); i++) {
            int x = startX + i * (grid.cellWidth + grid.gap);

            drawQuickSiteSquare(
                    graphics,
                    x,
                    startY,
                    grid.cellWidth,
                    cardHeight,
                    frequent.get(i)
            );

            drawMultiLineCentered(
                    graphics,
                    frequent.get(i).title(),
                    x + grid.cellWidth / 2,
                    startY + cardHeight + textOffset,
                    COLOR_TEXT_PRIMARY
            );
        }

        return startY + cardHeight + 28;
    }

    private int drawSharedWithYouCards(
            GuiGraphics graphics,
            int startX,
            int availableWidth,
            int startY
    ) {
        int gap = 10;
        int cardWidth = (availableWidth - gap) / 2;
        int cardHeight = 84;

        drawSharedCard(
                graphics,
                startX,
                startY,
                cardWidth,
                cardHeight,
                0xFFE6EEF7,
                "Network Setup",
                "vsia.net",
                "From Admin"
        );

        drawSharedCard(
                graphics,
                startX + cardWidth + gap,
                startY,
                cardWidth,
                cardHeight,
                0xFFE8F0E7,
                "Server Tour",
                "rack.vsia",
                "From Player"
        );

        return startY + cardHeight + 10;
    }

    private void drawSharedCard(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int thumbColor,
            String title,
            String site,
            String source
    ) {
        roundedRect(graphics, x, y, width, height, 12, COLOR_CARD);
        drawBorder(graphics, x, y, width, height, COLOR_BORDER);

        roundedRect(graphics, x, y, width, 42, 12, thumbColor);
        roundedRect(graphics, x + width / 2 - 14, y + 8, 28, 22, 10, 0x66000000);
        drawPlayTriangle(graphics, x + width / 2 - 3, y + 13, 0xFFFFFFFF);

        PhoneText.draw(graphics, font, fitText(title, width - 12), x + 6, y + 48, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, fitText(site, width - 12), x + 6, y + 61, COLOR_TEXT_TERTIARY);

        roundedRect(graphics, x + 6, y + 72, Math.min(width - 12, 54), 10, 5, 0xFFEFEFF3);
        PhoneText.draw(graphics, font, fitText(source, Math.min(width - 18, 48)), x + 9, y + 74, COLOR_TEXT_SECONDARY);
    }

    private void renderNetworkError(
            GuiGraphics graphics,
            BrowserResponse response
    ) {
        roundedRect(graphics, contentX, contentY, contentWidth, contentHeight, 18, COLOR_PAGE);

        roundedRect(graphics, contentX + 14, contentY + 16, contentWidth - 28, 28, 10, COLOR_RED);
        PhoneText.drawCentered(graphics, font, response.statusCode() + " " + response.reason(),
                phoneX + PHONE_WIDTH / 2, contentY + 25, 0xFFFFFFFF);

        PhoneText.drawCentered(graphics, font, "Safari cannot open the page",
                phoneX + PHONE_WIDTH / 2, contentY + 64, COLOR_TEXT_PRIMARY);

        int y = contentY + 88;

        for (String line : response.reason().split("\n")) {
            PhoneText.drawCentered(graphics, font, line, phoneX + PHONE_WIDTH / 2, y, COLOR_TEXT_SECONDARY);
            y += 15;
        }

        if (!response.body().isBlank()) {
            roundedRect(graphics, phoneX + 28, y + 6, PHONE_WIDTH - 56, 74, 14, COLOR_CARD_ALT);
            y += 18;

            for (String line : response.body().split("\n")) {
                PhoneText.drawCentered(graphics, font, line, phoneX + PHONE_WIDTH / 2, y, COLOR_TEXT_SECONDARY);
                y += 15;
            }
        }

        if (response.openWifiSettingsSuggested()) {
            int buttonX = phoneX + 46;
            int buttonY = contentY + contentHeight - 58;
            roundedRect(graphics, buttonX, buttonY, PHONE_WIDTH - 92, 34, 12, COLOR_ACCENT);
            PhoneText.drawCentered(graphics, font, "Open Wi-Fi Settings",
                    phoneX + PHONE_WIDTH / 2, buttonY + 12, 0xFFFFFFFF);
        }
    }

    private void renderBottomChrome(
            GuiGraphics graphics
    ) {
        drawSoftShadow(graphics, phoneX + 10, browserPanelY, PHONE_WIDTH - 20, 90, 18);

        roundedRect(
                graphics,
                phoneX + 10,
                browserPanelY,
                PHONE_WIDTH - 20,
                90,
                18,
                COLOR_CHROME
        );

        roundedRect(
                graphics,
                phoneX + 18,
                addressY,
                PHONE_WIDTH - 36,
                34,
                13,
                0xFFFDFDFE
        );

        drawBorder(
                graphics,
                phoneX + 18,
                addressY,
                PHONE_WIDTH - 36,
                34,
                0xFFE7E7EC
        );

        drawSearchLens(graphics, phoneX + 29, addressY + 11, 0xFF9A9AA1);
        drawMicIcon(graphics, phoneX + PHONE_WIDTH - 38, addressY + 10, 0xFF9A9AA1);

        int iconColorEnabled = COLOR_ACCENT;
        int iconColorDisabled = 0xFFB8B9BF;

        drawBackChevron(graphics, phoneX + 30, toolbarY + 10, browser.canGoBack() ? iconColorEnabled : iconColorDisabled);
        drawForwardChevron(graphics, phoneX + 70, toolbarY + 10, browser.canGoForward() ? iconColorEnabled : iconColorDisabled);
        drawShareOutline(graphics, phoneX + 115, toolbarY + 9, COLOR_ACCENT);
        drawBookmarksOutline(graphics, phoneX + 160, toolbarY + 9, COLOR_ACCENT);
        drawTabsOutline(graphics, phoneX + 203, toolbarY + 9, COLOR_ACCENT, browser.tabCount());

        graphics.fill(phoneX + 18, browserPanelY + 44, phoneX + PHONE_WIDTH - 18, browserPanelY + 45, 0x22B4B4BA);
    }

    private void renderOverlay(
            GuiGraphics graphics
    ) {
        graphics.fill(
                phoneX + 5,
                phoneY + 36,
                phoneX + PHONE_WIDTH - 5,
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
        int x = phoneX + 16;
        int y = phoneY + PHONE_HEIGHT - 264;
        int width = PHONE_WIDTH - 32;

        drawSoftShadow(graphics, x, y, width, 226, 20);
        roundedRect(graphics, x, y, width, 226, 20, 0xFFF9F9FB);

        String host = browser.currentUrl().isBlank()
                ? "Start Page"
                : new BrowserRequest(browser.currentUrl()).host();

        PhoneText.drawCentered(graphics, font, host, phoneX + PHONE_WIDTH / 2, y + 16, COLOR_TEXT_PRIMARY);
        PhoneText.drawCentered(graphics, font, fitText(displayAddress(browser.currentUrl()), width - 44),
                phoneX + PHONE_WIDTH / 2, y + 34, COLOR_TEXT_TERTIARY);

        int rowY = y + 58;
        drawSheetRow(graphics, x + 12, rowY, width - 24, "Copy Link", "Copy website address");
        rowY += 46;
        drawSheetRow(graphics, x + 12, rowY, width - 24,
                browser.isBookmarked(browser.currentUrl()) ? "Remove Bookmark" : "Add Bookmark",
                "Save this page");
        rowY += 46;
        drawSheetRow(graphics, x + 12, rowY, width - 24, "Open in New Tab", "Keep this page open");

        roundedRect(graphics, x + 12, y + 194, width - 24, 24, 9, COLOR_CARD_ALT);
        PhoneText.drawCentered(graphics, font, "Cancel", phoneX + PHONE_WIDTH / 2, y + 202, COLOR_ACCENT);
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
        drawBorder(graphics, x, y, width, 40, COLOR_BORDER);
        PhoneText.draw(graphics, font, title, x + 13, y + 8, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, subtitle, x + 13, y + 22, COLOR_TEXT_TERTIARY);
    }

    private void renderBookmarksPanel(
            GuiGraphics graphics
    ) {
        int x = phoneX + 16;
        int y = phoneY + 56;
        int width = PHONE_WIDTH - 32;
        int height = PHONE_HEIGHT - 112;

        drawSoftShadow(graphics, x, y, width, height, 20);
        roundedRect(graphics, x, y, width, height, 20, 0xFFF9F9FB);

        PhoneText.draw(graphics, font, "Done", x + 16, y + 16, COLOR_ACCENT);
        PhoneText.drawCentered(graphics, font, "Bookmarks", phoneX + PHONE_WIDTH / 2, y + 16, COLOR_TEXT_PRIMARY);

        String current = browser.currentUrl();

        if (!current.isBlank()) {
            PhoneText.draw(graphics, font,
                    browser.isBookmarked(current) ? "Remove Current" : "Add Current",
                    x + width - 86, y + 16, COLOR_ACCENT);
        }

        int listTop = y + 44;
        int listBottom = y + height - 12;

        graphics.enableScissor(x + 8, listTop, x + width - 8, listBottom);

        List<String> bookmarks = browser.bookmarks();

        if (bookmarks.isEmpty()) {
            PhoneText.drawCentered(graphics, font, "No Bookmarks Yet",
                    phoneX + PHONE_WIDTH / 2, listTop + 46, COLOR_TEXT_TERTIARY);
            PhoneText.drawCentered(graphics, font, "Save a page from the share menu.",
                    phoneX + PHONE_WIDTH / 2, listTop + 61, COLOR_TEXT_TERTIARY);
        }

        for (int i = 0; i < bookmarks.size(); i++) {
            int rowY = listTop + i * 50 - overlayScroll;

            if (rowY + 44 < listTop || rowY > listBottom) {
                continue;
            }

            String url = bookmarks.get(i);

            roundedRect(graphics, x + 10, rowY, width - 20, 44, 12, COLOR_CARD);
            drawBorder(graphics, x + 10, rowY, width - 20, 44, COLOR_BORDER);

            PhoneText.draw(graphics, font, fitText(new BrowserRequest(url).host(), width - 86),
                    x + 22, rowY + 8, COLOR_TEXT_PRIMARY);
            PhoneText.draw(graphics, font, fitText(url, width - 86),
                    x + 22, rowY + 24, COLOR_TEXT_TERTIARY);
            PhoneText.draw(graphics, font, "Delete", x + width - 54, rowY + 16, COLOR_RED);
        }

        graphics.disableScissor();
    }

    private void renderTabsPanel(
            GuiGraphics graphics
    ) {
        int x = phoneX + 16;
        int y = phoneY + 54;
        int width = PHONE_WIDTH - 32;
        int height = PHONE_HEIGHT - 108;

        drawSoftShadow(graphics, x, y, width, height, 20);
        roundedRect(graphics, x, y, width, height, 20, 0xFFF4F4F8);

        PhoneText.draw(graphics, font, "Done", x + 16, y + 16, COLOR_ACCENT);
        PhoneText.drawCentered(graphics, font, "Tabs", phoneX + PHONE_WIDTH / 2, y + 16, COLOR_TEXT_PRIMARY);
        PhoneText.draw(graphics, font, "New", x + width - 32, y + 16, COLOR_ACCENT);

        int listTop = y + 44;
        int listBottom = y + height - 12;

        graphics.enableScissor(x + 8, listTop, x + width - 8, listBottom);

        List<PhoneBrowser.TabSnapshot> tabs = browser.tabs();

        for (int i = 0; i < tabs.size(); i++) {
            PhoneBrowser.TabSnapshot tab = tabs.get(i);
            int cardY = listTop + i * 74 - overlayScroll;

            if (cardY + 64 < listTop || cardY > listBottom) {
                continue;
            }

            roundedRect(graphics, x + 10, cardY, width - 20, 64, 14, tab.active() ? COLOR_CARD : COLOR_CARD_ALT);
            drawBorder(graphics, x + 10, cardY, width - 20, 64, COLOR_BORDER);

            String shownUrl = tab.url().isBlank() ? "Start Page" : displayAddress(tab.url());

            PhoneText.draw(graphics, font, fitText(tab.title(), width - 96), x + 22, cardY + 10, COLOR_TEXT_PRIMARY);
            PhoneText.draw(graphics, font, fitText(shownUrl, width - 96), x + 22, cardY + 27, COLOR_TEXT_TERTIARY);
            PhoneText.draw(graphics, font, tab.loading() ? "Loading..." : tab.active() ? "Current Tab" : "Tap to switch",
                    x + 22, cardY + 44, COLOR_TEXT_TERTIARY);

            PhoneText.draw(graphics, font, "Close", x + width - 46, cardY + 10, COLOR_RED);
        }

        graphics.disableScissor();
    }

    private void renderToast(
            GuiGraphics graphics
    ) {
        if (toast.isBlank() || System.currentTimeMillis() > toastUntil) {
            return;
        }

        int toastWidth = Math.min(PHONE_WIDTH - 58, PhoneText.width(font, toast) + 28);
        int toastX = phoneX + (PHONE_WIDTH - toastWidth) / 2;
        int toastY = browserPanelY - 36;

        roundedRect(graphics, toastX, toastY, toastWidth, 24, 12, 0xEA1D1D20);
        PhoneText.drawCentered(graphics, font, toast, phoneX + PHONE_WIDTH / 2, toastY + 8, 0xFFFFFFFF);
    }

    private void showToast(String text) {
        toast = text == null ? "" : text;
        toastUntil = System.currentTimeMillis() + 1500L;
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

            if (browser.response() == null && handleQuickSiteClick(mouseX, mouseY)) {
                return true;
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

            if (inside(mouseX, mouseY, phoneX + 22, toolbarY + 1, 20, 20)) {
                browser.back();
                forcePageRefresh();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 63, toolbarY + 1, 20, 20)) {
                browser.forward();
                forcePageRefresh();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 106, toolbarY - 1, 24, 24)) {
                openOverlay(BrowserOverlay.SHARE);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 151, toolbarY - 1, 24, 24)) {
                openOverlay(BrowserOverlay.BOOKMARKS);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 194, toolbarY - 1, 26, 24)) {
                openOverlay(BrowserOverlay.TABS);
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 18, addressY, PHONE_WIDTH - 36, 34)) {
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

    private boolean handleQuickSiteClick(
            double mouseX,
            double mouseY
    ) {
        int innerX = contentX + 12;
        int innerWidth = contentWidth - 24;
        int baseY = contentY + 16 - scrollY;

        int favoritesTitleY = baseY;
        int favoritesGridY = favoritesTitleY + 20;

        List<QuickSite> favorites = new ArrayList<>();
        favorites.add(new QuickSite("Saved\nTabs", 0xFFEFF2F8, COLOR_ACCENT, QuickIcon.TABS, QuickAction.SAVED_TABS));
        favorites.add(new QuickSite("Apple", 0xFFF6F6F8, 0xFF8E8E93, QuickIcon.CIRCLE, QuickAction.NONE));
        favorites.add(new QuickSite("Bing", 0xFFF6F6F8, 0xFF2E9BFF, QuickIcon.B, QuickAction.NONE));
        favorites.add(new QuickSite("Google", 0xFFF6F6F8, 0xFF4285F4, QuickIcon.G, QuickAction.NONE));
        favorites.add(new QuickSite("Yahoo", 0xFF4C1FB8, 0xFFFFFFFF, QuickIcon.TEXT, QuickAction.NONE));
        favorites.add(new QuickSite("VSIA", 0xFF2D7CFF, 0xFFFFFFFF, QuickIcon.OMEGA, QuickAction.NONE));

        GridMetrics grid = createGridMetrics(innerWidth, 4, 40, 10);
        int cardHeight = 40;
        int rowGap = 22;

        for (int i = 0; i < favorites.size(); i++) {
            int row = i / 4;
            int col = i % 4;
            int x = innerX + col * (grid.cellWidth + grid.gap);
            int y = favoritesGridY + row * (cardHeight + rowGap + 20);

            if (inside(mouseX, mouseY, x, y, grid.cellWidth, cardHeight + 24)) {
                if (favorites.get(i).action() == QuickAction.SAVED_TABS) {
                    openOverlay(BrowserOverlay.TABS);
                    return true;
                }

                navigateQuickAction(favorites.get(i).action());
                return true;
            }
        }

        int favoritesRows = (favorites.size() + 3) / 4;
        int afterFavoritesY = favoritesGridY + favoritesRows * cardHeight + (favoritesRows - 1) * (rowGap + 20) + 20;
        int frequentTitleY = afterFavoritesY + 10;
        int frequentGridY = frequentTitleY + 20;

        List<QuickSite> frequent = new ArrayList<>();
        frequent.add(new QuickSite("Lab", 0xFFC03A2B, 0xFFFFFFFF, QuickIcon.TOWER, QuickAction.QUICK_DOMAIN_LAB));
        frequent.add(new QuickSite("Server", 0xFFE0552E, 0xFFFFFFFF, QuickIcon.V, QuickAction.QUICK_DOMAIN_SERVER));
        frequent.add(new QuickSite("Rack", 0xFFF0F0F1, 0xFF5B5B60, QuickIcon.RACK, QuickAction.QUICK_DOMAIN_RACK));
        frequent.add(new QuickSite("Video", 0xFFF04C3E, 0xFFFFFFFF, QuickIcon.PLAY, QuickAction.NONE));

        for (int i = 0; i < frequent.size(); i++) {
            int x = innerX + i * (grid.cellWidth + grid.gap);

            if (inside(mouseX, mouseY, x, frequentGridY, grid.cellWidth, cardHeight + 24)) {
                navigateQuickAction(frequent.get(i).action());
                return true;
            }
        }

        int sharedHeaderY = frequentGridY + cardHeight + 28 + 10;
        int sharedCardsY = sharedHeaderY + 20;

        int gap = 10;
        int sharedWidth = (innerWidth - gap) / 2;
        int sharedHeight = 84;

        if (inside(mouseX, mouseY, innerX, sharedCardsY, sharedWidth, sharedHeight)) {
            browser.navigate("a.w128lab.com");
            syncAddressFromBrowser();
            return true;
        }

        if (inside(mouseX, mouseY, innerX + sharedWidth + gap, sharedCardsY, sharedWidth, sharedHeight)) {
            browser.navigate("demo.w128lab.com");
            syncAddressFromBrowser();
            return true;
        }

        if (inside(mouseX, mouseY, contentX + contentWidth - 70, sharedHeaderY, 60, 18)) {
            openOverlay(BrowserOverlay.BOOKMARKS);
            return true;
        }

        return false;
    }

    private void navigateQuickAction(QuickAction action) {
        switch (action) {
            case QUICK_DOMAIN_LAB -> browser.navigate("a.w128lab.com");
            case QUICK_DOMAIN_SERVER -> browser.navigate("b.w128lab.com");
            case QUICK_DOMAIN_RACK -> browser.navigate("demo.w128lab.com");
            case SAVED_TABS -> openOverlay(BrowserOverlay.TABS);
            case NONE -> {
                showToast("No linked site");
                return;
            }
        }

        syncAddressFromBrowser();
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
        int x = phoneX + 16;
        int y = phoneY + PHONE_HEIGHT - 264;
        int width = PHONE_WIDTH - 32;

        int rowY = y + 58;

        if (inside(mouseX, mouseY, x + 12, rowY, width - 24, 40)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(displayAddress(current));
                showToast("Address Copied");
            }

            closeOverlay();
            return true;
        }

        rowY += 46;

        if (inside(mouseX, mouseY, x + 12, rowY, width - 24, 40)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                boolean added = browser.toggleBookmark(current);
                showToast(added ? "Bookmark Added" : "Bookmark Removed");
            }

            closeOverlay();
            return true;
        }

        rowY += 46;

        if (inside(mouseX, mouseY, x + 12, rowY, width - 24, 40)) {
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

        if (inside(mouseX, mouseY, x + 12, y + 194, width - 24, 24)) {
            closeOverlay();
            return true;
        }

        return true;
    }

    private boolean handleBookmarksClick(
            double mouseX,
            double mouseY
    ) {
        int x = phoneX + 16;
        int y = phoneY + 56;
        int width = PHONE_WIDTH - 32;

        if (inside(mouseX, mouseY, x + 10, y + 8, 48, 24)) {
            closeOverlay();
            return true;
        }

        if (inside(mouseX, mouseY, x + width - 102, y + 8, 92, 24)) {
            String current = browser.currentUrl();

            if (!current.isBlank()) {
                boolean added = browser.toggleBookmark(current);
                showToast(added ? "Bookmark Added" : "Bookmark Removed");
            }

            return true;
        }

        List<String> bookmarks = browser.bookmarks();
        int listTop = y + 44;

        for (int i = 0; i < bookmarks.size(); i++) {
            int rowY = listTop + i * 50 - overlayScroll;

            if (!inside(mouseX, mouseY, x + 10, rowY, width - 20, 44)) {
                continue;
            }

            String url = bookmarks.get(i);

            if (mouseX >= x + width - 66) {
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
        int x = phoneX + 16;
        int y = phoneY + 54;
        int width = PHONE_WIDTH - 32;

        if (inside(mouseX, mouseY, x + 10, y + 8, 48, 24)) {
            closeOverlay();
            return true;
        }

        if (inside(mouseX, mouseY, x + width - 44, y + 8, 34, 24)) {
            boolean opened = browser.newTab();

            if (opened) {
                closeOverlay();
                forcePageRefresh();
            } else {
                showToast("Maximum Tabs Reached");
            }

            return true;
        }

        int listTop = y + 44;
        List<PhoneBrowser.TabSnapshot> tabs = browser.tabs();

        for (int i = 0; i < tabs.size(); i++) {
            int cardY = listTop + i * 74 - overlayScroll;

            if (!inside(mouseX, mouseY, x + 10, cardY, width - 20, 64)) {
                continue;
            }

            if (mouseX >= x + width - 62 && mouseY <= cardY + 28) {
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
            int maximum;

            if (browser.response() == null) {
                maximum = Math.max(0, startPageTotalHeight - contentHeight);
            } else {
                maximum = Math.max(0, lastRender.totalHeight() - contentHeight);
            }

            scrollY = clampScroll(scrollY - (int) Math.round(delta * 20.0D), maximum);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
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

    private void syncAddressFromBrowser() {
        if (addressField == null) {
            return;
        }

        addressField.setValue(displayAddress(browser.currentUrl()));
        addressField.setFocused(false);
        setFocused(null);
    }

    private int clampScroll(
            int value,
            int maximum
    ) {
        return Math.max(0, Math.min(maximum, value));
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

    private String displayAddress(String raw) {
        BrowserRequest request = new BrowserRequest(raw);
        return request.displayUrl();
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

    private void drawMultiLineCentered(
            GuiGraphics graphics,
            String text,
            int centerX,
            int startY,
            int color
    ) {
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            PhoneText.drawCentered(graphics, font, lines[i], centerX, startY + i * 11, color);
        }
    }

    private GridMetrics createGridMetrics(
            int availableWidth,
            int columns,
            int preferredCellWidth,
            int minGap
    ) {
        int cellWidth = Math.min(preferredCellWidth, (availableWidth - minGap * (columns - 1)) / columns);
        cellWidth = Math.max(34, cellWidth);
        int gap = columns > 1 ? Math.max(6, (availableWidth - cellWidth * columns) / (columns - 1)) : 0;
        return new GridMetrics(cellWidth, gap);
    }

    private void drawQuickSiteSquare(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            QuickSite site
    ) {
        roundedRect(graphics, x, y, width, height, 10, site.bgColor());
        drawBorder(graphics, x, y, width, height, 0x12A0A0AA);
        drawQuickIcon(graphics, x + width / 2, y + height / 2, site.iconColor(), site.icon());
    }

    private void drawQuickIcon(
            GuiGraphics graphics,
            int centerX,
            int centerY,
            int color,
            QuickIcon icon
    ) {
        switch (icon) {
            case TABS -> {
                roundedRect(graphics, centerX - 10, centerY - 6, 9, 12, 4, color);
                roundedRect(graphics, centerX + 1, centerY - 6, 9, 12, 4, color);
                roundedRect(graphics, centerX - 8, centerY - 4, 5, 8, 3, 0xFFFFFFFF);
                roundedRect(graphics, centerX + 3, centerY - 4, 5, 8, 3, 0xFFFFFFFF);
            }
            case CIRCLE -> roundedRect(graphics, centerX - 9, centerY - 9, 18, 18, 9, color);
            case B -> PhoneText.drawCentered(graphics, font, "b", centerX, centerY - 4, color);
            case G -> PhoneText.drawCentered(graphics, font, "G", centerX, centerY - 4, color);
            case TEXT -> PhoneText.drawCentered(graphics, font, "Y!", centerX, centerY - 4, color);
            case OMEGA -> PhoneText.drawCentered(graphics, font, "O", centerX, centerY - 4, color);
            case TOWER -> {
                graphics.fill(centerX - 3, centerY - 10, centerX + 3, centerY + 8, color);
                graphics.fill(centerX - 6, centerY - 7, centerX - 3, centerY - 4, color);
                graphics.fill(centerX + 3, centerY - 7, centerX + 6, centerY - 4, color);
                graphics.fill(centerX - 7, centerY + 8, centerX + 7, centerY + 10, color);
            }
            case V -> PhoneText.drawCentered(graphics, font, "V", centerX, centerY - 4, color);
            case RACK -> {
                roundedRect(graphics, centerX - 8, centerY - 10, 16, 20, 4, color);
                graphics.fill(centerX - 5, centerY - 6, centerX + 5, centerY - 4, 0xFFFFFFFF);
                graphics.fill(centerX - 5, centerY - 1, centerX + 5, centerY + 1, 0xFFFFFFFF);
                graphics.fill(centerX - 5, centerY + 4, centerX + 5, centerY + 6, 0xFFFFFFFF);
            }
            case PLAY -> drawPlayTriangle(graphics, centerX - 4, centerY - 6, color);
        }
    }

    private void drawPlayTriangle(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x, y, x + 2, y + 12, color);
        graphics.fill(x + 2, y + 2, x + 4, y + 10, color);
        graphics.fill(x + 4, y + 4, x + 6, y + 8, color);
    }

    private void drawCompassIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int size
    ) {
        roundedRect(graphics, x, y, size, size, size / 2, COLOR_ACCENT);
        roundedRect(graphics, x + 5, y + 5, size - 10, size - 10, (size - 10) / 2, 0xFFFFFFFF);
        graphics.fill(x + size / 2 - 1, y + 8, x + size / 2 + 1, y + size / 2 + 1, 0xFFFF5A55);
        graphics.fill(x + size / 2, y + size / 2, x + size / 2 + 2, y + size - 9, COLOR_ACCENT);
    }

    private void drawBackChevron(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x + 7, y - 7, x + 9, y - 5, color);
        graphics.fill(x + 5, y - 5, x + 7, y - 3, color);
        graphics.fill(x + 3, y - 3, x + 5, y - 1, color);
        graphics.fill(x + 5, y - 1, x + 7, y + 1, color);
        graphics.fill(x + 7, y + 1, x + 9, y + 3, color);
    }

    private void drawForwardChevron(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x + 3, y - 7, x + 5, y - 5, color);
        graphics.fill(x + 5, y - 5, x + 7, y - 3, color);
        graphics.fill(x + 7, y - 3, x + 9, y - 1, color);
        graphics.fill(x + 5, y - 1, x + 7, y + 1, color);
        graphics.fill(x + 3, y + 1, x + 5, y + 3, color);
    }

    private void drawShareOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 5, y + 4, x + 5, y + 5, color);
        graphics.fill(x - 5, y + 4, x - 4, y + 12, color);
        graphics.fill(x + 4, y + 4, x + 5, y + 12, color);
        graphics.fill(x - 5, y + 12, x + 5, y + 13, color);
        graphics.fill(x, y - 6, x + 1, y + 5, color);
        graphics.fill(x - 3, y - 3, x, y, color);
        graphics.fill(x + 1, y - 3, x + 4, y, color);
    }

    private void drawBookmarksOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 6, y - 7, x - 5, y + 7, color);
        graphics.fill(x + 5, y - 7, x + 6, y + 7, color);
        graphics.fill(x - 6, y - 7, x + 6, y - 6, color);
        graphics.fill(x - 6, y + 7, x + 6, y + 8, color);
        graphics.fill(x - 2, y + 7, x + 2, y + 11, color);
    }

    private void drawTabsOutline(
            GuiGraphics graphics,
            int x,
            int y,
            int color,
            int count
    ) {
        graphics.fill(x - 7, y - 6, x + 2, y + 5, color);
        graphics.fill(x - 6, y - 5, x + 1, y + 4, 0xFFFFFFFF);
        graphics.fill(x - 2, y - 9, x + 7, y + 2, color);
        graphics.fill(x - 1, y - 8, x + 6, y + 1, 0xFFFFFFFF);

        if (count > 1) {
            PhoneText.drawCentered(graphics, font, Integer.toString(count), x + 2, y - 2, color);
        }
    }

    private void drawSearchLens(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        roundedRect(graphics, x, y, 9, 9, 5, color);
        roundedRect(graphics, x + 2, y + 2, 5, 5, 3, 0xFFFDFDFE);
        graphics.fill(x + 7, y + 7, x + 10, y + 10, color);
    }

    private void drawMicIcon(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        roundedRect(graphics, x, y, 6, 10, 3, color);
        graphics.fill(x + 1, y + 1, x + 5, y + 7, 0xFFFDFDFE);
        graphics.fill(x + 2, y + 10, x + 4, y + 13, color);
        graphics.fill(x - 1, y + 12, x + 7, y + 13, color);
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

    private void drawBorder(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private enum BrowserOverlay {
        NONE,
        SHARE,
        BOOKMARKS,
        TABS
    }

    private enum QuickIcon {
        TABS,
        CIRCLE,
        B,
        G,
        TEXT,
        OMEGA,
        TOWER,
        V,
        RACK,
        PLAY
    }

    private enum QuickAction {
        NONE,
        SAVED_TABS,
        QUICK_DOMAIN_LAB,
        QUICK_DOMAIN_SERVER,
        QUICK_DOMAIN_RACK
    }

    private record QuickSite(
            String title,
            int bgColor,
            int iconColor,
            QuickIcon icon,
            QuickAction action
    ) {
    }

    private record GridMetrics(
            int cellWidth,
            int gap
    ) {
    }
}
