package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.IPhoneStatusBar;
import com.k1ngtle.vsia.phone.client.PhoneText;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public abstract class IPhoneScreen extends Screen {
    public static final int PHONE_WIDTH = 236;
    public static final int PHONE_HEIGHT = 438;

    protected static final int SHELL_COLOR = 0xFF050505;
    protected static final int SCREEN_INSET = 4;
    protected static final int SCREEN_RADIUS = 21;
    protected static final int DISPLAY_INSET = 10;
    protected static final int DISPLAY_RADIUS = 18;
    protected static final int CONTENT_BOTTOM_INSET = 28;

    protected int phoneX;
    protected int phoneY;

    protected IPhoneScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        phoneX = (width - PHONE_WIDTH) / 2;
        phoneY = (height - PHONE_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        super.tick();
        PhoneNetworkController.get().tick();
    }

    protected void renderPhoneBase(GuiGraphics graphics) {
        renderPhoneShell(graphics, 0xFF16181D);
        renderStatusBar(graphics);
    }

    protected void renderPhoneShell(GuiGraphics graphics, int screenColor) {
        renderBackground(graphics);
        roundedRect(
                graphics,
                phoneX,
                phoneY,
                PHONE_WIDTH,
                PHONE_HEIGHT,
                24,
                SHELL_COLOR
        );
        roundedRect(
                graphics,
                phoneX + SCREEN_INSET,
                phoneY + SCREEN_INSET,
                PHONE_WIDTH - SCREEN_INSET * 2,
                PHONE_HEIGHT - SCREEN_INSET * 2,
                SCREEN_RADIUS,
                screenColor
        );
    }

    protected final int displayX() {
        return phoneX + DISPLAY_INSET;
    }

    protected final int displayY() {
        return phoneY + DISPLAY_INSET;
    }

    protected final int displayWidth() {
        return PHONE_WIDTH - DISPLAY_INSET * 2;
    }

    protected final int displayHeight() {
        return PHONE_HEIGHT - DISPLAY_INSET * 2;
    }

    protected final void maskDisplayCorners(GuiGraphics graphics) {
        maskRoundedOutside(
                graphics,
                displayX(),
                displayY(),
                displayWidth(),
                displayHeight(),
                DISPLAY_RADIUS,
                SHELL_COLOR
        );
    }

    protected void renderStatusBar(GuiGraphics graphics) {
        IPhoneStatusBar.render(graphics, font, phoneX, phoneY, PHONE_WIDTH);
    }

    protected void renderHomeIndicator(GuiGraphics graphics) {
        int indicatorWidth = 78;
        int x = phoneX + (PHONE_WIDTH - indicatorWidth) / 2;
        int y = phoneY + PHONE_HEIGHT - 18;
        roundedRect(graphics, x, y, indicatorWidth, 4, 2, 0xFFFFFFFF);
    }

    protected void renderHeader(GuiGraphics graphics, String back, String title) {
        String safeTitle = fitUi(title == null ? "" : title, PHONE_WIDTH - 42);
        int titleWidth = uiWidth(safeTitle);
        int titleX = phoneX + (PHONE_WIDTH - titleWidth) / 2;
        int titleRight = titleX + titleWidth;
        int titleY = phoneY + 47;

        if (back != null && !back.isBlank()) {
            String fullBack = "‹ " + back;
            String compactBack = "‹";
            int backX = phoneX + 16;
            int minimumGap = 8;
            int fullBackRight = backX + uiWidth(fullBack);

            String backText = fullBackRight + minimumGap <= titleX
                    ? fullBack
                    : compactBack;

            drawUiText(graphics, backText, backX, titleY, 0xFF5FA9FF);
        }

        int rightSafe = phoneX + PHONE_WIDTH - 16;
        if (titleRight > rightSafe) {
            titleX = rightSafe - titleWidth;
        }

        drawUiText(graphics, safeTitle, titleX, titleY, 0xFFFFFFFF);
    }

    protected Component uiText(String text) {
        return PhoneText.component(text);
    }

    protected FormattedCharSequence uiSequence(String text) {
        return PhoneText.sequence(text);
    }

    protected int uiWidth(String text) {
        return font.width(uiText(text));
    }

    protected void drawUiText(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawString(font, uiText(text), x, y, color, false);
    }

    protected void drawUiCentered(GuiGraphics graphics, String text, int centerX, int y, int color) {
        Component component = uiText(text);
        graphics.drawString(font, component, centerX - font.width(component) / 2, y, color, false);
    }

    protected String fitUi(String value, int maxWidth) {
        String text = value == null ? "" : value;
        if (uiWidth(text) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty() && uiWidth(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text.isEmpty() ? "..." : text + "...";
    }

    protected int drawUiWrappedCentered(
            GuiGraphics graphics,
            String text,
            int centerX,
            int y,
            int maxWidth,
            int lineHeight,
            int maxLines,
            int color
    ) {
        List<FormattedCharSequence> lines = font.split(uiText(text), maxWidth);
        int count = Math.min(maxLines, lines.size());

        for (int i = 0; i < count; i++) {
            FormattedCharSequence line = lines.get(i);
            int x = centerX - font.width(line) / 2;
            graphics.drawString(font, line, x, y + i * lineHeight, color, false);
        }

        return count;
    }

    protected void beginPhoneClip(GuiGraphics graphics, int topOffset) {
        graphics.enableScissor(
                phoneX + DISPLAY_INSET,
                phoneY + topOffset,
                phoneX + PHONE_WIDTH - DISPLAY_INSET,
                phoneY + PHONE_HEIGHT - CONTENT_BOTTOM_INSET
        );
    }

    protected void endPhoneClip(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    protected boolean clickedHome(double mouseX, double mouseY) {
        return mouseX >= phoneX + 70
                && mouseX <= phoneX + PHONE_WIDTH - 70
                && mouseY >= phoneY + PHONE_HEIGHT - 30
                && mouseY <= phoneY + PHONE_HEIGHT;
    }

    protected boolean clickedBack(double mouseX, double mouseY) {
        return mouseX >= phoneX + 8
                && mouseX <= phoneX + 82
                && mouseY >= phoneY + 38
                && mouseY <= phoneY + 68;
    }

    protected void goHome() {
        if (!(this instanceof IPhoneHomeScreen)) {
            minecraft.setScreen(new IPhoneHomeScreen());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && clickedHome(mouseX, mouseY)) {
            goHome();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    protected static void roundedRect(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        graphics.fill(x + radius, y, x + width - radius, y + height, color);
        graphics.fill(x, y + radius, x + width, y + height - radius, color);

        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(Math.max(0, radius * radius - dy * dy))
            );

            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }

    private static void maskRoundedOutside(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int radius,
            int color
    ) {
        for (int i = 0; i < radius; i++) {
            int dy = radius - i;
            int inset = (int) Math.ceil(
                    radius - Math.sqrt(Math.max(0, radius * radius - dy * dy))
            );

            if (inset <= 0) {
                continue;
            }

            graphics.fill(x, y + i, x + inset, y + i + 1, color);
            graphics.fill(x + width - inset, y + i, x + width, y + i + 1, color);
            graphics.fill(x, y + height - i - 1, x + inset, y + height - i, color);
            graphics.fill(
                    x + width - inset,
                    y + height - i - 1,
                    x + width,
                    y + height - i,
                    color
            );
        }
    }

    protected static boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX < x + width
                && mouseY >= y
                && mouseY < y + height;
    }
}
