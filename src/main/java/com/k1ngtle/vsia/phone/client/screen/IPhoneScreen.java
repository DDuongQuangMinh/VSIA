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
        renderBackground(graphics);
        roundedRect(graphics, phoneX, phoneY, PHONE_WIDTH, PHONE_HEIGHT, 24, 0xFF050505);
        roundedRect(graphics, phoneX + 4, phoneY + 4, PHONE_WIDTH - 8, PHONE_HEIGHT - 8, 21, 0xFF16181D);
        IPhoneStatusBar.render(graphics, font, phoneX, phoneY, PHONE_WIDTH);
    }

    protected void renderHomeIndicator(GuiGraphics graphics) {
        int w = 78;
        int x = phoneX + (PHONE_WIDTH - w) / 2;
        int y = phoneY + PHONE_HEIGHT - 18;
        roundedRect(graphics, x, y, w, 4, 2, 0xFFFFFFFF);
    }

    protected void renderHeader(GuiGraphics graphics, String back, String title) {
        if (back != null && !back.isBlank()) {
            graphics.drawString(font, "‹ " + back, phoneX + 16, phoneY + 47, 0xFF5FA9FF, false);
        }
        int tw = font.width(title);
        graphics.drawString(font, title, phoneX + (PHONE_WIDTH - tw) / 2, phoneY + 47, 0xFFFFFFFF, false);
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

    protected static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            int dy = r - i;
            int inset = (int)Math.ceil(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
