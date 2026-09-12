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
    private final PhoneBrowser browser = PhoneBrowser.get();

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

    private int toolbarY;
    private int addressBackgroundY;

    public IPhoneBrowserScreen() {
        super(Component.literal("Browser"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 9;
        contentY = phoneY + 42;
        contentWidth = PHONE_WIDTH - 18;

        toolbarY = phoneY + PHONE_HEIGHT - 102;
        addressBackgroundY = toolbarY + 25;

        contentHeight = toolbarY - contentY - 7;

        addressBox = new EditBox(
                font,
                phoneX + 42,
                addressBackgroundY + 7,
                PHONE_WIDTH - 76,
                18,
                Component.literal("Address")
        );

        addressBox.setBordered(false);
        addressBox.setMaxLength(512);
        addressBox.setTextColor(0xFF1C1C1E);
        addressBox.setValue(browser.currentUrl());

        addRenderableWidget(addressBox);
    }

    @Override
    public void tick() {
        super.tick();

        if (addressBox != null) {
            addressBox.tick();

            if (!addressBox.isFocused()
                    && !browser.currentUrl().isBlank()
                    && !addressBox.getValue().equals(browser.currentUrl())) {
                addressBox.setValue(browser.currentUrl());
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
        renderPhoneBase(graphics);
        refreshDocument();

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

        renderBrowserToolbar(graphics);
        renderHomeIndicator(graphics);

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
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
            String status = response.statusCode() + " " + response.reason();

            graphics.fill(
                    contentX,
                    contentY,
                    contentX + contentWidth,
                    contentY + 17,
                    0xD9FF453A
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
                0xFFFFFFFF
        );

        int centerX = phoneX + PHONE_WIDTH / 2;
        int centerY = contentY + contentHeight / 2;

        drawCompass(graphics, centerX, centerY - 20);

        graphics.drawCenteredString(
                font,
                "Loading...",
                centerX,
                centerY + 16,
                0xFF1C1C1E
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
        int centerY = contentY + contentHeight / 2;

        drawCompass(graphics, centerX, centerY - 36);

        graphics.drawCenteredString(
                font,
                "VS:IA Browser",
                centerX,
                centerY + 4,
                0xFF1C1C1E
        );

        graphics.pose().pushPose();
        graphics.pose().scale(0.8F, 0.8F, 1.0F);

        graphics.drawCenteredString(
                font,
                "Enter a published server website below",
                Math.round(centerX / 0.8F),
                Math.round((centerY + 24) / 0.8F),
                0xFF636366
        );

        graphics.pose().popPose();
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
        int y = contentY + 58;

        graphics.drawCenteredString(
                font,
                "Safari cannot open the page",
                centerX,
                y,
                0xFF1C1C1E
        );

        y += 30;

        for (String line : response.reason().split("\n")) {
            graphics.drawCenteredString(
                    font,
                    line,
                    centerX,
                    y,
                    0xFF636366
            );
            y += 15;
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
                y += 14;
            }
        }

        if (response.openWifiSettingsSuggested()) {
            int buttonX = phoneX + 45;
            int buttonY = contentY + contentHeight - 58;

            roundedRect(
                    graphics,
                    buttonX,
                    buttonY,
                    PHONE_WIDTH - 90,
                    32,
                    9,
                    0xFF007AFF
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

    private void renderBrowserToolbar(GuiGraphics graphics) {
        graphics.fill(
                phoneX + 7,
                toolbarY - 5,
                phoneX + PHONE_WIDTH - 7,
                phoneY + PHONE_HEIGHT - 27,
                0xF7F2F2F7
        );

        int backColor = browser.canGoBack()
                ? 0xFF0A84FF
                : 0xFFB2B2B7;

        int forwardColor = browser.canGoForward()
                ? 0xFF0A84FF
                : 0xFFB2B2B7;

        drawBackArrow(graphics, phoneX + 37, toolbarY + 7, backColor);
        drawForwardArrow(graphics, phoneX + 82, toolbarY + 7, forwardColor);
        drawReload(graphics, phoneX + 127, toolbarY + 7, 0xFF0A84FF);
        drawTabs(graphics, phoneX + 174, toolbarY + 7, 0xFF0A84FF);

        roundedRect(
                graphics,
                phoneX + 15,
                addressBackgroundY,
                PHONE_WIDTH - 30,
                32,
                11,
                0xFFE5E5EA
        );

        drawLock(
                graphics,
                phoneX + 25,
                addressBackgroundY + 9,
                browser.response() != null
                        && browser.response().success()
                        ? 0xFF34C759
                        : 0xFF8E8E93
        );

        if (addressBox != null
                && addressBox.getValue().isBlank()
                && !addressBox.isFocused()) {
            graphics.drawString(
                    font,
                    "Website address",
                    phoneX + 42,
                    addressBackgroundY + 12,
                    0xFF8E8E93,
                    false
            );
        }
    }

    private void drawCompass(GuiGraphics graphics, int centerX, int centerY) {
        roundedRect(
                graphics,
                centerX - 22,
                centerY - 22,
                44,
                44,
                22,
                0xFF0A84FF
        );

        roundedRect(
                graphics,
                centerX - 18,
                centerY - 18,
                36,
                36,
                18,
                0xFFFFFFFF
        );

        graphics.fill(
                centerX - 2,
                centerY - 14,
                centerX + 1,
                centerY + 2,
                0xFFFF453A
        );

        graphics.fill(
                centerX - 1,
                centerY,
                centerX + 3,
                centerY + 15,
                0xFF0A84FF
        );
    }

    private void drawBackArrow(GuiGraphics graphics, int x, int y, int color) {
        graphics.hLine(x - 8, x + 6, y, color);
        graphics.fill(x - 8, y - 1, x - 5, y + 2, color);
        graphics.fill(x - 6, y - 4, x - 4, y - 1, color);
        graphics.fill(x - 6, y + 2, x - 4, y + 5, color);
    }

    private void drawForwardArrow(GuiGraphics graphics, int x, int y, int color) {
        graphics.hLine(x - 6, x + 8, y, color);
        graphics.fill(x + 5, y - 1, x + 8, y + 2, color);
        graphics.fill(x + 4, y - 4, x + 6, y - 1, color);
        graphics.fill(x + 4, y + 2, x + 6, y + 5, color);
    }

    private void drawReload(GuiGraphics graphics, int x, int y, int color) {
        graphics.hLine(x - 6, x + 5, y - 5, color);
        graphics.vLine(x - 6, y - 5, y + 4, color);
        graphics.hLine(x - 6, x + 4, y + 5, color);
        graphics.vLine(x + 5, y - 1, y + 5, color);
        graphics.fill(x + 3, y - 7, x + 7, y - 3, color);
    }

    private void drawTabs(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x - 7, y - 6, x + 5, y + 6, color);
        graphics.fill(x - 5, y - 4, x + 3, y + 4, 0xFFF2F2F7);
        graphics.fill(x - 3, y - 8, x + 8, y + 3, color);
        graphics.fill(x - 1, y - 6, x + 6, y + 1, 0xFFF2F2F7);
    }

    private void drawLock(GuiGraphics graphics, int x, int y, int color) {
        graphics.fill(x, y + 4, x + 9, y + 12, color);
        graphics.fill(x + 2, y, x + 7, y + 2, color);
        graphics.fill(x + 1, y + 1, x + 3, y + 6, color);
        graphics.fill(x + 6, y + 1, x + 8, y + 6, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            BrowserResponse response = browser.response();

            if (response != null && response.openWifiSettingsSuggested()) {
                int buttonX = phoneX + 45;
                int buttonY = contentY + contentHeight - 58;

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
                if (link.contains(mouseX, mouseY)) {
                    String target = BrowserRequest.resolve(
                            browser.currentUrl(),
                            link.href()
                    );

                    browser.navigate(target);
                    addressBox.setValue(target);
                    return true;
                }
            }

            if (inside(mouseX, mouseY, phoneX + 21, toolbarY - 3, 32, 24)) {
                browser.back();
                addressBox.setValue(browser.currentUrl());
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 66, toolbarY - 3, 32, 24)) {
                browser.forward();
                addressBox.setValue(browser.currentUrl());
                return true;
            }

            if (inside(mouseX, mouseY, phoneX + 111, toolbarY - 3, 32, 24)) {
                browser.reload();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && addressBox != null
                && addressBox.isFocused()) {
            navigateAddressBar();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inside(
                mouseX,
                mouseY,
                contentX,
                contentY,
                contentWidth,
                contentHeight
        )) {
            int maxScroll = Math.max(
                    0,
                    lastRender.totalHeight() - contentHeight
            );

            scrollY = Math.max(
                    0,
                    Math.min(
                            maxScroll,
                            scrollY - (int) Math.round(delta * 18.0)
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
        addressBox.setValue(browser.currentUrl());
        addressBox.setFocused(false);
        scrollY = 0;
    }
}
