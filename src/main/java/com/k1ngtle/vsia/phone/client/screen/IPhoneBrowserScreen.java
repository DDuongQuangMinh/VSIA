package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.browser.BrowserRequest;
import com.k1ngtle.vsia.phone.browser.BrowserResponse;
import com.k1ngtle.vsia.phone.browser.PhoneBrowser;
import com.k1ngtle.vsia.phone.browser.PhoneHtmlDocument;
import com.k1ngtle.vsia.phone.browser.PhoneHtmlRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class IPhoneBrowserScreen extends IPhoneScreen {
    private final PhoneBrowser browser =
            PhoneBrowser.get();

    private EditBox addressBox;
    private BrowserResponse lastResponse;
    private PhoneHtmlDocument document;

    private PhoneHtmlRenderer.RenderResult lastRender =
            PhoneHtmlRenderer.RenderResult.empty();

    private int scrollY;

    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;

    private int browserPanelY;
    private int controlsY;
    private int addressY;

    public IPhoneBrowserScreen() {
        super(Component.literal("Browser"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 8;
        contentY = phoneY + 38;
        contentWidth = PHONE_WIDTH - 16;

        browserPanelY =
                phoneY + PHONE_HEIGHT - 120;

        controlsY =
                browserPanelY + 8;

        addressY =
                browserPanelY + 38;

        contentHeight =
                browserPanelY
                        - contentY;

        addressBox =
                new EditBox(
                        font,
                        phoneX + 46,
                        addressY + 9,
                        PHONE_WIDTH - 86,
                        16,
                        Component.literal("Website address")
                );

        addressBox.setBordered(false);
        addressBox.setCanLoseFocus(true);
        addressBox.setMaxLength(512);
        addressBox.setTextColor(0xFF111111);
        addressBox.setTextColorUneditable(0xFF111111);
        addressBox.setValue(displayAddress(browser.currentUrl()));
        addressBox.setFocused(false);
        addressBox.setVisible(false);

        addRenderableWidget(addressBox);
    }

    @Override
    public void tick() {
        super.tick();

        if (addressBox == null) {
            return;
        }

        addressBox.tick();

        if (!addressBox.isFocused()) {
            addressBox.setVisible(false);

            String display =
                    displayAddress(
                            browser.currentUrl()
                    );

            if (!addressBox
                    .getValue()
                    .equals(display)) {
                addressBox.setValue(display);
            }
        } else {
            addressBox.setVisible(true);
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, 0xFF0E0F14);
        renderStatusBar(graphics);
        refreshDocument();

        graphics.fill(
                contentX,
                contentY,
                contentX + contentWidth,
                contentY + contentHeight,
                0xFFFFFFFF
        );

        if (browser.loading()) {
            renderLoading(graphics);
        } else if (browser.response() == null) {
            renderStartPage(graphics);
        } else {
            BrowserResponse response =
                    browser.response();

            if (response.statusCode() == 0) {
                renderNetworkError(graphics, response);
            } else {
                renderWebsite(graphics, response);
            }
        }

        renderBrowserChrome(graphics);
        renderHomeIndicator(graphics);

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void refreshDocument() {
        BrowserResponse response =
                browser.response();

        if (response == lastResponse) {
            return;
        }

        lastResponse = response;
        scrollY = 0;

        if (response == null
                || response.statusCode() == 0) {
            document = null;
            lastRender =
                    PhoneHtmlRenderer.RenderResult.empty();
            return;
        }

        document =
                PhoneHtmlDocument.from(response);
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
                    contentX + contentWidth,
                    contentY + 18,
                    0xE6FF6961
            );

            graphics.drawCenteredString(
                    font,
                    status,
                    phoneX + PHONE_WIDTH / 2,
                    contentY + 5,
                    0xFFFFFFFF
            );
        }
    }

    private void renderLoading(GuiGraphics graphics) {
        graphics.fill(
                contentX,
                contentY,
                contentX + contentWidth,
                contentY + contentHeight,
                0xFFF2F2F7
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int centerY = contentY + contentHeight / 2;

        drawCompass(
                graphics,
                centerX,
                centerY - 28,
                34
        );

        graphics.drawCenteredString(
                font,
                "Loading Website",
                centerX,
                centerY + 12,
                0xFF1C1C1E
        );

        int progressWidth = PHONE_WIDTH - 30;
        int phase = (int) (
                System.currentTimeMillis() / 80L % progressWidth
        );

        graphics.fill(
                phoneX + 15,
                contentY,
                phoneX + 15 + phase,
                contentY + 2,
                0xFF0A84FF
        );
    }

    private void renderStartPage(GuiGraphics graphics) {
        graphics.fill(
                contentX,
                contentY,
                contentX + contentWidth,
                contentY + contentHeight,
                0xFFF2F2F7
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int centerY = contentY + contentHeight / 2 - 26;

        drawCompass(
                graphics,
                centerX,
                centerY - 32,
                42
        );

        graphics.drawCenteredString(
                font,
                "Safari",
                centerX,
                centerY + 12,
                0xFF1C1C1E
        );

        graphics.drawCenteredString(
                font,
                "Open any published W1.28 website",
                centerX,
                centerY + 32,
                0xFF636366
        );

        graphics.drawCenteredString(
                font,
                "by domain or direct Server Rack address.",
                centerX,
                centerY + 46,
                0xFF8E8E93
        );

        graphics.drawCenteredString(
                font,
                "Examples:",
                centerX,
                centerY + 72,
                0xFF636366
        );

        graphics.drawCenteredString(
                font,
                "phone-test.com",
                centerX,
                centerY + 86,
                0xFF0A84FF
        );

        graphics.drawCenteredString(
                font,
                "phone-test.com@10.0.1.20",
                centerX,
                centerY + 100,
                0xFF0A84FF
        );
    }

    private void renderNetworkError(
            GuiGraphics graphics,
            BrowserResponse response
    ) {
        graphics.fill(
                contentX,
                contentY,
                contentX + contentWidth,
                contentY + contentHeight,
                0xFFF2F2F7
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int y = contentY + 50;

        graphics.drawCenteredString(
                font,
                "Safari cannot open the page",
                centerX,
                y,
                0xFF1C1C1E
        );

        y += 28;

        for (String line : response.reason().split("\n")) {
            graphics.drawCenteredString(
                    font,
                    line,
                    centerX,
                    y,
                    0xFF636366
            );

            y += 14;
        }

        if (!response.body().isBlank()) {
            y += 8;

            for (String line : response.body().split("\n")) {
                graphics.drawCenteredString(
                        font,
                        line,
                        centerX,
                        y,
                        0xFF8E8E93
                );

                y += 13;
            }
        }

        if (response.openWifiSettingsSuggested()) {
            int buttonX = phoneX + 45;
            int buttonY = contentY + contentHeight - 55;

            roundedRect(
                    graphics,
                    buttonX,
                    buttonY,
                    PHONE_WIDTH - 90,
                    32,
                    10,
                    0xFF0A84FF
            );

            graphics.drawCenteredString(
                    font,
                    "Open Wi-Fi Settings",
                    phoneX + PHONE_WIDTH / 2,
                    buttonY + 12,
                    0xFFFFFFFF
            );
        }
    }

    private void renderBrowserChrome(GuiGraphics graphics) {
        roundedRect(
                graphics,
                phoneX + 7,
                browserPanelY,
                PHONE_WIDTH - 14,
                86,
                15,
                0xF7F2F2F7
        );

        int enabled = 0xFF0A84FF;
        int disabled = 0xFFB2B2B7;

        drawBackArrow(
                graphics,
                phoneX + 39,
                controlsY + 9,
                browser.canGoBack() ? enabled : disabled
        );

        drawForwardArrow(
                graphics,
                phoneX + 83,
                controlsY + 9,
                browser.canGoForward() ? enabled : disabled
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
                enabled
        );

        roundedRect(
                graphics,
                phoneX + 15,
                addressY,
                PHONE_WIDTH - 30,
                34,
                12,
                0xFFE5E5EA
        );

        drawLock(
                graphics,
                phoneX + 26,
                addressY + 10,
                browser.response() != null && browser.response().success()
                        ? 0xFF5F6368
                        : 0xFF8E8E93
        );

        if (addressBox != null) {
            addressBox.setVisible(addressBox.isFocused());
        }

        if (addressBox == null || !addressBox.isFocused()) {
            String shown =
                    fitAddress(
                            displayAddress(
                                    addressBox == null
                                            ? browser.currentUrl()
                                            : addressBox.getValue()
                            ),
                            PHONE_WIDTH - 106
                    );

            int color =
                    shown.isBlank()
                            ? 0xFF8E8E93
                            : 0xFF1C1C1E;

            graphics.drawString(
                    font,
                    shown.isBlank()
                            ? "Search or enter website name"
                            : shown,
                    phoneX + 46,
                    addressY + 12,
                    color,
                    false
            );
        }
    }

    private String displayAddress(String raw) {
        BrowserRequest request =
                new BrowserRequest(raw);

        return request.displayUrl();
    }

    private String fitAddress(String value, int maxWidth) {
        String text =
                value == null
                        ? ""
                        : value;

        if (font.width(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";

        while (!text.isEmpty()
                && font.width(text + suffix) > maxWidth) {
            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }

        return text + suffix;
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
                0xFF0A84FF
        );

        int inner = Math.max(6, radius - 4);

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
        graphics.fill(x - 6, y + 1, x + 6, y + 8, color);
        graphics.fill(x - 4, y + 3, x + 4, y + 7, 0xFFF2F2F7);
        graphics.vLine(x, y - 7, y + 3, color);
        graphics.fill(x - 3, y - 5, x, y - 3, color);
        graphics.fill(x + 1, y - 5, x + 4, y - 3, color);
    }

    private void drawBookmarks(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 6, y - 7, x + 6, y + 7, color);
        graphics.fill(x - 4, y - 5, x + 4, y + 3, 0xFFF2F2F7);
        graphics.fill(x - 2, y + 3, x + 2, y + 7, color);
    }

    private void drawTabs(
            GuiGraphics graphics,
            int x,
            int y,
            int color
    ) {
        graphics.fill(x - 7, y - 6, x + 4, y + 5, color);
        graphics.fill(x - 5, y - 4, x + 2, y + 3, 0xFFF2F2F7);
        graphics.fill(x - 3, y - 8, x + 8, y + 3, color);
        graphics.fill(x - 1, y - 6, x + 6, y + 1, 0xFFF2F2F7);
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

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            BrowserResponse response = browser.response();

            if (response != null && response.openWifiSettingsSuggested()) {
                int buttonX = phoneX + 45;
                int buttonY = contentY + contentHeight - 55;

                if (inside(
                        mouseX,
                        mouseY,
                        buttonX,
                        buttonY,
                        PHONE_WIDTH - 90,
                        32
                )) {
                    minecraft.setScreen(new IPhoneWifiScreen());
                    return true;
                }
            }

            for (PhoneHtmlRenderer.LinkRegion link : lastRender.links()) {
                if (!link.contains(mouseX, mouseY)) {
                    continue;
                }

                String target =
                        BrowserRequest.resolve(
                                browser.currentUrl(),
                                link.href()
                        );

                browser.navigate(target);

                if (addressBox != null) {
                    addressBox.setValue(displayAddress(target));
                    addressBox.setFocused(false);
                    addressBox.setVisible(false);
                }

                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 23, controlsY - 2, 31, 24)) {
                browser.back();
                syncAddressFromBrowser();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 68, controlsY - 2, 31, 24)) {
                browser.forward();
                syncAddressFromBrowser();
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 15, addressY, PHONE_WIDTH - 30, 34)) {
                if (addressBox != null) {
                    addressBox.setVisible(true);
                    addressBox.setFocused(true);
                    addressBox.setValue(displayAddress(browser.currentUrl()));
                    addressBox.setCursorPosition(addressBox.getValue().length());
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void syncAddressFromBrowser() {
        if (addressBox != null) {
            addressBox.setValue(
                    displayAddress(
                            browser.currentUrl()
                    )
            );
            addressBox.setFocused(false);
            addressBox.setVisible(false);
        }
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && addressBox != null
                && addressBox.isFocused()) {
            navigateAddressBar();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                && addressBox != null
                && addressBox.isFocused()) {
            addressBox.setFocused(false);
            addressBox.setVisible(false);
            syncAddressFromBrowser();
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
                            lastRender.totalHeight() - contentHeight
                    );

            scrollY =
                    Math.max(
                            0,
                            Math.min(
                                    maximum,
                                    scrollY - (int) Math.round(delta * 20.0D)
                            )
                    );

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void navigateAddressBar() {
        if (addressBox == null) {
            return;
        }

        String value = addressBox.getValue();

        browser.navigate(value);
        addressBox.setFocused(false);
        addressBox.setVisible(false);
        scrollY = 0;
    }
}
