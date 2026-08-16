package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.FirewallBlockEntity;
import com.k1ngtle.vsia.world.inventory.FirewallMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FirewallScreen extends AbstractContainerScreen<FirewallMenu> {

    private enum Tab {
        DASHBOARD("Dashboard"),
        RULES("Firewall Rules"),
        SYSTEM("System");

        public final String label;
        Tab(String label) { this.label = label; }
    }

    private Tab currentTab = Tab.DASHBOARD;
    private EditBox ipBox;
    private EditBox nameBox;
    private Button strictToggle;

    public FirewallScreen(FirewallMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 720;
        this.imageHeight = 440;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        int contentX = x + 180;

        this.nameBox = new EditBox(this.font, contentX + 85, y + 62, 300, 12, Component.literal("Device Name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(0xAAAAAA);
        if (this.menu.blockEntity != null) this.nameBox.setValue(this.menu.blockEntity.getDeviceName());
        this.nameBox.setVisible(false);
        this.addRenderableWidget(this.nameBox);

        this.ipBox = new EditBox(this.font, contentX + 85, y + 79, 300, 12, Component.literal("Management IP"));
        this.ipBox.setMaxLength(15);
        this.ipBox.setBordered(false);
        this.ipBox.setTextColor(0xFFFFFF);
        if (this.menu.blockEntity != null) this.ipBox.setValue(this.menu.blockEntity.getManagementIp());
        this.ipBox.setVisible(false);
        this.addRenderableWidget(this.ipBox);

        this.strictToggle = Button.builder(
                Component.literal("Strict Mode: " + (this.menu.blockEntity != null && this.menu.blockEntity.isStrictMode() ? "ON" : "OFF")),
                b -> {
                    if (this.menu.blockEntity != null) {
                        boolean strict = !this.menu.blockEntity.isStrictMode();
                        this.menu.blockEntity.setStrictMode(strict);
                        b.setMessage(Component.literal("Strict Mode: " + (strict ? "ON" : "OFF")));
                    }
                }
        ).bounds(contentX + 80, y + 100, 120, 20).build();
        this.strictToggle.visible = false;
        this.addRenderableWidget(this.strictToggle);

        updateVisibility();
    }

    private void updateVisibility() {
        boolean isSystem = (currentTab == Tab.SYSTEM);
        this.nameBox.setVisible(isSystem);
        this.ipBox.setVisible(isSystem);
        this.strictToggle.visible = isSystem;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (mouseY >= y + 10 && mouseY <= y + 30) {
            for (int i = 0; i < Tab.values().length; i++) {
                int tabX = x + 10 + (i * 120);
                if (mouseX >= tabX && mouseX <= tabX + 115) {
                    this.currentTab = Tab.values()[i];
                    updateVisibility();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) { this.onClose(); return true; }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        g.fill(x, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF181A1D);
        g.fill(x, y + 30, x + this.imageWidth, y + 31, 0xFFD32F2F); // Firewall Red Accent
        g.fill(x, y + 30, x + 1, y + this.imageHeight, 0xFF333333);
        g.fill(x + this.imageWidth - 1, y + 30, x + this.imageWidth, y + this.imageHeight, 0xFF333333);
        g.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF333333);

        for (int i = 0; i < Tab.values().length; i++) {
            int tabX = x + 10 + (i * 120);
            boolean isActive = (this.currentTab == Tab.values()[i]);

            int bgColor = isActive ? 0xFF181A1D : 0xFF2B2D31;
            int textColor = isActive ? 0xFFFFFFFF : 0xFFAAAAAA;

            g.fill(tabX, y + 10, tabX + 115, y + 31, bgColor);
            g.fill(tabX, y + 10, tabX + 115, y + 11, 0xFFD32F2F);
            g.fill(tabX, y + 10, tabX + 1, y + 31, 0xFF333333);
            g.fill(tabX + 114, y + 10, tabX + 115, y + 31, 0xFF333333);

            if (!isActive) {
                g.fill(tabX, y + 30, tabX + 115, y + 31, 0xFFD32F2F);
            }

            int textWidth = this.font.width(Tab.values()[i].label);
            g.drawString(this.font, Tab.values()[i].label, tabX + (57 - textWidth / 2), y + 16, textColor, false);
        }

        switch (currentTab) {
            case DASHBOARD -> renderDashboard(g, x, y);
            case RULES -> renderRules(g, x, y);
            case SYSTEM -> renderSystem(g, x, y);
        }
    }

    private void renderDashboard(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "Security Gateway Overview", x + 30, y + 50, 0xFFFFFF, false);

        int boxW = 200;
        int boxH = 100;

        // Traffic Graph Mock
        g.fill(x + 30, y + 70, x + 30 + boxW, y + 70 + boxH, 0xFF2B2D31);
        g.fill(x + 30, y + 70, x + 30 + boxW, y + 71, 0xFFD32F2F);
        g.drawString(this.font, "Recent Traffic", x + 40, y + 80, 0xFFAAAAAA, false);

        long t = System.currentTimeMillis() / 200;
        for (int i = 0; i < 20; i++) {
            int h = (int)(Math.abs(Math.sin(t + i)) * 40) + 10;
            g.fill(x + 40 + (i * 8), y + 160 - h, x + 46 + (i * 8), y + 160, 0xFF4CAF50);
        }

        // Threats Blocked
        g.fill(x + 250, y + 70, x + 250 + boxW, y + 70 + boxH, 0xFF2B2D31);
        g.fill(x + 250, y + 70, x + 250 + boxW, y + 71, 0xFFD32F2F);
        g.drawString(this.font, "Threats Blocked", x + 260, y + 80, 0xFFAAAAAA, false);
        g.pose().pushPose();
        g.pose().scale(2.5f, 2.5f, 1.0f);
        g.drawString(this.font, "1,024", (int)((x + 300) / 2.5f), (int)((y + 110) / 2.5f), 0xFFD32F2F, false);
        g.pose().popPose();

        // System Load
        g.fill(x + 470, y + 70, x + 470 + boxW, y + 70 + boxH, 0xFF2B2D31);
        g.fill(x + 470, y + 70, x + 470 + boxW, y + 71, 0xFFD32F2F);
        g.drawString(this.font, "CPU & Memory", x + 480, y + 80, 0xFFAAAAAA, false);
        g.drawString(this.font, "CPU: 12%", x + 480, y + 110, 0xFF4CAF50, false);
        g.drawString(this.font, "RAM: 4.2GB / 8GB", x + 480, y + 130, 0xFFFFC107, false);
    }

    private void renderRules(GuiGraphics g, int x, int y) {
        g.drawString(this.font, "Active Firewall Rules", x + 30, y + 50, 0xFFFFFF, false);

        int ty = y + 70;
        g.fill(x + 30, ty, x + 690, ty + 15, 0xFF2B2D31);
        g.drawString(this.font, "Rule Name", x + 40, ty + 4, 0xFFAAAAAA, false);
        g.drawString(this.font, "Action", x + 250, ty + 4, 0xFFAAAAAA, false);
        g.drawString(this.font, "Source", x + 350, ty + 4, 0xFFAAAAAA, false);
        g.drawString(this.font, "Destination", x + 450, ty + 4, 0xFFAAAAAA, false);
        g.drawString(this.font, "Status", x + 600, ty + 4, 0xFFAAAAAA, false);

        if (this.menu.blockEntity != null) {
            ty += 20;
            for (FirewallBlockEntity.FirewallRule rule : this.menu.blockEntity.getRules()) {
                g.drawString(this.font, rule.name, x + 40, ty, 0xFFFFFF, false);
                g.drawString(this.font, rule.action, x + 250, ty, rule.action.equals("ALLOW") ? 0xFF4CAF50 : 0xFFD32F2F, false);
                g.drawString(this.font, rule.source, x + 350, ty, 0xFFFFFF, false);
                g.drawString(this.font, rule.destination, x + 450, ty, 0xFFFFFF, false);
                g.drawString(this.font, rule.enabled ? "Active" : "Disabled", x + 600, ty, rule.enabled ? 0xFF4CAF50 : 0xFF777777, false);
                ty += 15;
            }
        }
    }

    private void renderSystem(GuiGraphics g, int x, int y) {
        int contentX = x + 180;

        g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 76, 0xFF2B2D31);
        g.fill(contentX - 10, y + 77, x + this.imageWidth - 10, y + 93, 0xFF2B2D31);

        g.fill(contentX - 10, y + 60, x + this.imageWidth - 10, y + 61, 0xFF444444);
        g.fill(contentX - 10, y + 76, x + this.imageWidth - 10, y + 77, 0xFF444444);
        g.fill(contentX - 10, y + 93, x + this.imageWidth - 10, y + 94, 0xFF444444);
        g.fill(contentX - 10, y + 60, contentX - 9, y + 94, 0xFF444444);
        g.fill(x + this.imageWidth - 10, y + 60, x + this.imageWidth - 9, y + 94, 0xFF444444);

        g.drawString(this.font, "Device Name", contentX - 5, y + 64, 0xFFFFFF, false);
        g.drawString(this.font, "Management IP", contentX - 5, y + 81, 0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }
}