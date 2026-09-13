package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.IPhoneStatusBar;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public abstract class IPhoneScreen extends Screen {
    public static final int PHONE_WIDTH = 236;
    public static final int PHONE_HEIGHT = 438;

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
        roundedRect(graphics, phoneX, phoneY, PHONE_WIDTH, PHONE_HEIGHT, 24, 0xFF050505);
        roundedRect(graphics, phoneX + 4, phoneY + 4, PHONE_WIDTH - 8, PHONE_HEIGHT - 8, 21, screenColor);
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
        String safeTitle = fitHeaderTitle(title == null ? "" : title, PHONE_WIDTH - 42);
        int titleWidth = font.width(safeTitle);
        int titleX = phoneX + (PHONE_WIDTH - titleWidth) / 2;
        int titleRight = titleX + titleWidth;
        int titleY = phoneY + 47;

        if (back != null && !back.isBlank()) {
            String fullBack = "‹ " + back;
            String compactBack = "‹";
            int backX = phoneX + 16;
            int minimumGap = 8;
            int fullBackRight = backX + font.width(fullBack);

            String backText = fullBackRight + minimumGap <= titleX
                    ? fullBack
                    : compactBack;

            graphics.drawString(
                    font,
                    backText,
                    backX,
                    titleY,
                    0xFF5FA9FF,
                    false
            );
        }

        int rightSafe = phoneX + PHONE_WIDTH - 16;
        if (titleRight > rightSafe) {
            titleX = rightSafe - titleWidth;
        }

        graphics.drawString(
                font,
                safeTitle,
                titleX,
                titleY,
                0xFFFFFFFF,
                false
        );
    }

    private String fitHeaderTitle(String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }

        String text = value;
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text.isEmpty() ? "..." : text + "...";
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
