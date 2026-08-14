package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.StoredFile;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StorageServerScreen extends AbstractContainerScreen<StorageServerMenu> {

    private boolean isDashboard = true;
    private float scrollOffset = 0.0f;
    private float fileScrollOffset = 0.0f;

    public enum TerminalSize {
        SMALL("Size: Small", 300, 236),
        NORMAL("Size: Normal", 340, 246),
        FULL("Size: Full", 380, 256);

        public final String label;
        public final int width;
        public final int height;

        TerminalSize(String label, int width, int height) {
            this.label = label;
            this.width = width;
            this.height = height;
        }
    }

    public enum Tab {
        ITEMS, FILES
    }

    private TerminalSize currentSize = TerminalSize.NORMAL;
    private Tab currentTab = Tab.ITEMS;
    private StoredFile openFile = null;

    private Button modeToggleButton;
    private Button sizeToggleButton;
    private Button checkUpdatesButton;
    private EditBox searchBox;

    private Button uploadButton;
    private Button deleteButton;

    public StorageServerScreen(StorageServerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 840; // Expanded to fit 3 large columns
        this.imageHeight = 460;
        updateVisibility();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelY = 10000;

        updateDimensions();

        this.modeToggleButton = Button.builder(
                Component.literal(isDashboard ? "Configure" : "Dashboard"),
                b -> toggleMode()
        ).bounds(this.leftPos + this.imageWidth - 95, this.topPos - 22, 85, 18).build();
        this.addRenderableWidget(this.modeToggleButton);

        this.sizeToggleButton = Button.builder(
                Component.literal(currentSize.label),
                b -> toggleSize()
        ).bounds(this.leftPos + 10, this.topPos - 22, 90, 18).build();
        this.sizeToggleButton.visible = !isDashboard;
        this.addRenderableWidget(this.sizeToggleButton);

        this.checkUpdatesButton = Button.builder(
                Component.literal("Check for Updates"),
                b -> {}
        ).bounds(this.leftPos + 22, this.topPos + 218, 120, 20).build();
        this.checkUpdatesButton.visible = isDashboard;
        this.addRenderableWidget(this.checkUpdatesButton);

        this.searchBox = new EditBox(this.font, this.leftPos + 115, this.topPos + 6, 85, 12, Component.literal("Search"));
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(!isDashboard);
        this.addRenderableWidget(this.searchBox);

        this.uploadButton = Button.builder(
                Component.literal("Upload File"),
                b -> handleUpload()
        ).bounds(this.leftPos + 7, this.topPos + 112, 75, 18).build();
        this.uploadButton.visible = false;
        this.addRenderableWidget(this.uploadButton);

        this.deleteButton = Button.builder(
                Component.literal("Delete File"),
                b -> handleDelete()
        ).bounds(this.leftPos + 94, this.topPos + 112, 75, 18).build();
        this.deleteButton.visible = false;
        this.addRenderableWidget(this.deleteButton);
    }

    private void updateDimensions() {
        if (isDashboard) {
            this.imageWidth = 840;
            this.imageHeight = 460;
            // Add a small +10 offset so the "Configure" button isn't squished against the top
            this.leftPos = (this.width - this.imageWidth) / 2;
            this.topPos = ((this.height - this.imageHeight) / 2) + 10;
        } else {
            this.imageWidth = currentSize.width;
            this.imageHeight = currentSize.height;
            this.leftPos = (this.width - this.imageWidth) / 2;
            this.topPos = (this.height - this.imageHeight) / 2;
        }
    }

    private void updateVisibility() {
        this.menu.slotsVisible = (!this.isDashboard && this.currentTab == Tab.ITEMS && this.openFile == null);
        if (this.uploadButton != null) this.uploadButton.visible = (!this.isDashboard && this.currentTab == Tab.FILES && this.openFile == null);
        if (this.deleteButton != null) this.deleteButton.visible = (!this.isDashboard && this.currentTab == Tab.FILES && this.openFile == null);
    }

    private void toggleMode() {
        this.isDashboard = !this.isDashboard;
        this.openFile = null;
        this.modeToggleButton.setMessage(Component.literal(isDashboard ? "Configure" : "Dashboard"));
        this.sizeToggleButton.visible = !isDashboard;
        this.checkUpdatesButton.visible = isDashboard;
        this.searchBox.setVisible(!isDashboard);

        updateVisibility();
        updateDimensions();
        repositionWidgets();
    }

    private void toggleSize() {
        TerminalSize[] values = TerminalSize.values();
        currentSize = values[(currentSize.ordinal() + 1) % values.length];
        this.sizeToggleButton.setMessage(Component.literal(currentSize.label));
        updateDimensions();
        repositionWidgets();
    }

    private void repositionWidgets() {
        if (this.modeToggleButton != null) {
            this.modeToggleButton.setPosition(this.leftPos + this.imageWidth - 95, this.topPos - 22);
        }
        if (this.sizeToggleButton != null) {
            this.sizeToggleButton.setPosition(this.leftPos + 10, this.topPos - 22);
        }
        if (this.checkUpdatesButton != null) {
            // Firmly anchored to the bottom left of the System Info widget in Dashboard mode
            this.checkUpdatesButton.setPosition(this.leftPos + 22, this.topPos + 218);
        }
        if (this.searchBox != null) {
            this.searchBox.setPosition(this.leftPos + 115, this.topPos + 6);
        }
        if (this.uploadButton != null) {
            this.uploadButton.setPosition(this.leftPos + 7, this.topPos + 112);
        }
        if (this.deleteButton != null) {
            this.deleteButton.setPosition(this.leftPos + 94, this.topPos + 112);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        if (isDashboard) {
            renderDashboardMode(g);
        } else {
            renderTerminalMode(g, mouseX, mouseY);
        }

        int[] realCounts = new int[this.menu.slots.size()];
        if (!isDashboard && currentTab == Tab.ITEMS && openFile == null) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem()) {
                    realCounts[i] = slot.getItem().getCount();
                    if (realCounts[i] >= 1000) {
                        slot.getItem().setCount(1);
                    }
                }
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (!isDashboard && currentTab == Tab.ITEMS && openFile == null) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem() && realCounts[i] >= 1000) {
                    slot.getItem().setCount(realCounts[i]);

                    String formatted = formatLargeNumber(realCounts[i]);
                    int slotX = this.leftPos + slot.x;
                    int slotY = this.topPos + slot.y;

                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.drawString(this.font, formatted, slotX + 17 - this.font.width(formatted), slotY + 9, 0xFFFFFF, true);
                    g.pose().popPose();
                }
            }
        }

        if (!isDashboard) {
            g.fill(this.leftPos + 105, this.topPos + 4, this.leftPos + 210, this.topPos + 20, 0xFF141414);
            g.fill(this.leftPos + 106, this.topPos + 5, this.leftPos + 209, this.topPos + 19, 0xFF000000);
            this.searchBox.render(g, mouseX, mouseY, partialTick);

            if (openFile != null) {
                renderFileViewerOverlay(g, mouseX, mouseY);
            }
            if (this.currentTab == Tab.ITEMS && openFile == null) {
                this.renderTooltip(g, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isDashboard) {
            int x = this.leftPos;
            int y = this.topPos;

            if (mouseX >= x - 28 && mouseX <= x && mouseY >= y + 10 && mouseY <= y + 34) {
                this.currentTab = Tab.ITEMS;
                this.openFile = null;
                updateVisibility();
                return true;
            }
            if (mouseX >= x - 28 && mouseX <= x && mouseY >= y + 38 && mouseY <= y + 62) {
                this.currentTab = Tab.FILES;
                this.openFile = null;
                updateVisibility();
                return true;
            }

            if (this.currentTab == Tab.FILES) {
                if (openFile != null) {
                    int modalX = x + 10;
                    int modalY = y + 15;
                    int modalW = Math.max(250, this.imageWidth - 200);
                    if (mouseX >= modalX + modalW - 16 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 16) {
                        this.openFile = null;
                        updateVisibility();
                        return true;
                    }
                } else {
                    List<StoredFile> files = this.menu.getFiles();
                    int listY = y + 25 - (int)(this.fileScrollOffset * Math.max(0, files.size() - 6) * 14);

                    for (int i = 0; i < files.size(); i++) {
                        if (mouseY >= listY && mouseY < listY + 14 && mouseX >= x + 8 && mouseX <= x + 160) {
                            if (mouseY >= y + 20 && mouseY <= y + 108) {
                                this.openFile = files.get(i);
                                updateVisibility();
                                return true;
                            }
                        }
                        listY += 14;
                    }
                }
            }
        }

        if (isDashboard) {
            for (var widget : this.children()) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
            }
            return false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isDashboard) {
            if (currentTab == Tab.ITEMS) {
                this.scrollOffset = (float) Math.max(0.0, Math.min(1.0, this.scrollOffset - (delta * 0.1)));
                this.menu.scrollTo(this.scrollOffset);
            } else if (currentTab == Tab.FILES && openFile == null) {
                this.fileScrollOffset = (float) Math.max(0.0, Math.min(1.0, this.fileScrollOffset - (delta * 0.1)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isDashboard && button == 0 && currentTab == Tab.ITEMS) {
            int trackX = this.leftPos + 171;
            int trackY = this.topPos + 22;
            if (mouseX >= trackX && mouseX <= trackX + 14 && mouseY >= trackY && mouseY <= trackY + 108) {
                this.scrollOffset = (float) (mouseY - (trackY + 7.5)) / 91.0f;
                this.scrollOffset = Math.max(0.0f, Math.min(1.0f, this.scrollOffset));
                this.menu.scrollTo(this.scrollOffset);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isDashboard && this.searchBox != null && this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderDashboardMode(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;

        // Dashboard overall background
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF242424);

        g.drawString(this.font, "Dashboard", x + 16, y - 14, 0xFFFFFF, true);

        // Column Setup - Width 840 allows 3 columns of width 264 with 12px gaps
        int col1X = x + 12;
        int col2X = x + 288;
        int col3X = x + 564;
        int panelWidth = 264;

        // --- COL 1: System Info ---
        int p1Y = y + 12;
        g.fill(col1X, p1Y, col1X + panelWidth, p1Y + 240, 0xFF2A2B2D);
        g.fill(col1X + 1, p1Y + 1, col1X + panelWidth - 1, p1Y + 239, 0xFF1E1E1E);

        g.drawString(this.font, "TrueNAS SCALE", col1X + 10, p1Y + 10, 0xFFFFFF, false);
        g.fill(col1X + 10, p1Y + 24, col1X + panelWidth - 10, p1Y + 44, 0xFF111111);
        g.fill(col1X + 15, p1Y + 32, col1X + 20, p1Y + 37, 0xFF22C55E); // LEDs
        g.fill(col1X + 35, p1Y + 32, col1X + 40, p1Y + 37, 0xFF22C55E);

        g.drawString(this.font, "System Information", col1X + 10, p1Y + 56, 0xFF0092C8, false);
        g.fill(col1X + 10, p1Y + 70, col1X + panelWidth - 10, p1Y + 71, 0xFF333333);

        g.drawString(this.font, "Overview", col1X + 10, p1Y + 80, 0xAAAAAA, false);
        g.drawString(this.font, "Platform: TRUENAS-MINI-R", col1X + 10, p1Y + 100, 0xAAAAAA, false);
        g.drawString(this.font, "Version: ElectricEel-24.10.0-MASTER-2...", col1X + 10, p1Y + 120, 0xAAAAAA, false);
        g.drawString(this.font, "Hostname: re-minir-102", col1X + 10, p1Y + 140, 0xAAAAAA, false);
        g.drawString(this.font, "Uptime: 1h 28m", col1X + 10, p1Y + 160, 0x888888, false);
        // Check for Updates button is rendered at bottom left via the Button Widget

        // --- COL 1: Memory ---
        int p2Y = y + 264;
        g.fill(col1X, p2Y, col1X + panelWidth, p2Y + 184, 0xFF2A2B2D);
        g.fill(col1X + 1, p2Y + 1, col1X + panelWidth - 1, p2Y + 183, 0xFF1E1E1E);

        g.drawString(this.font, "Memory", col1X + 10, p2Y + 10, 0xFFFFFF, false);
        g.pose().pushPose();
        g.pose().scale(1.5f, 1.5f, 1.0f);
        g.drawString(this.font, "31.3", (int)((col1X + 10) / 1.5f), (int)((p2Y + 36) / 1.5f), 0xFFFFFF, true);
        g.pose().popPose();
        g.drawString(this.font, "GiB", col1X + 50, p2Y + 41, 0xAAAAAA, false);
        g.drawString(this.font, "total available (ECC)", col1X + 10, p2Y + 56, 0x777777, false);

        g.drawString(this.font, "Free: 27.0 GiB", col1X + 22, p2Y + 100, 0xAAAAAA, false);
        g.fill(col1X + 10, p2Y + 102, col1X + 16, p2Y + 108, 0xFF0092C8);

        g.drawString(this.font, "ZFS Cache: 1.1 GiB", col1X + 22, p2Y + 120, 0xAAAAAA, false);
        g.fill(col1X + 10, p2Y + 122, col1X + 16, p2Y + 128, 0xFFB82DB8);

        g.drawString(this.font, "Services: 3.2 GiB", col1X + 22, p2Y + 140, 0xAAAAAA, false);
        g.fill(col1X + 10, p2Y + 142, col1X + 16, p2Y + 148, 0xFFE6A23C);

        renderMemoryDonutChart(g, col1X + 190, p2Y + 92, 45, 30);

        // --- COL 2: CPU Cores ---
        int p3Y = y + 12;
        g.fill(col2X, p3Y, col2X + panelWidth, p3Y + 210, 0xFF2A2B2D);
        g.fill(col2X + 1, p3Y + 1, col2X + panelWidth - 1, p3Y + 209, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Usage Per Core", col2X + 10, p3Y + 10, 0xFFFFFF, false);
        renderBarGraph(g, col2X + 20, p3Y + 40, 224, 150, 0xFF0092C8, new float[]{0.22f, 0.48f, 0.16f, 0.75f, 0.40f, 0.68f, 0.14f, 0.48f}, true);

        // --- COL 2: CPU Temp ---
        int p4Y = y + 234;
        g.fill(col2X, p4Y, col2X + panelWidth, p4Y + 214, 0xFF2A2B2D);
        g.fill(col2X + 1, p4Y + 1, col2X + panelWidth - 1, p4Y + 213, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Temperature Per Core", col2X + 10, p4Y + 10, 0xFFFFFF, false);
        renderBarGraph(g, col2X + 20, p4Y + 40, 224, 150, 0xFFB82DB8, new float[]{0.65f, 0.68f, 0.64f, 0.70f, 0.66f, 0.67f, 0.63f, 0.65f}, true);

        // --- COL 3: CPU Usage / Model ---
        int p5aY = y + 12;
        g.fill(col3X, p5aY, col3X + 126, p5aY + 90, 0xFF2A2B2D);
        g.fill(col3X + 1, p5aY + 1, col3X + 125, p5aY + 89, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Usage", col3X + 10, p5aY + 8, 0xFFFFFF, false);
        renderMemoryDonutChart(g, col3X + 63, p5aY + 55, 20, 15);
        g.drawString(this.font, "1%", col3X + 57, p5aY + 51, 0xFFFFFF, false);

        int p5bX = col3X + 138;
        g.fill(p5bX, p5aY, p5bX + 126, p5aY + 90, 0xFF2A2B2D);
        g.fill(p5bX + 1, p5aY + 1, p5bX + 125, p5aY + 89, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Model", p5bX + 10, p5aY + 8, 0xFFFFFF, false);
        g.drawString(this.font, "Intel(R) Atom(TM)", p5bX + 10, p5aY + 40, 0xCCCCCC, false);
        g.drawString(this.font, "C3758 @ 2.20GHz", p5bX + 10, p5aY + 54, 0x888888, false);

        // --- COL 3: CPU Recent ---
        int p6Y = y + 114;
        g.fill(col3X, p6Y, col3X + panelWidth, p6Y + 90, 0xFF2A2B2D);
        g.fill(col3X + 1, p6Y + 1, col3X + panelWidth - 1, p6Y + 89, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Recent Usage", col3X + 10, p6Y + 8, 0xFFFFFF, false);
        g.fill(col3X + 20, p6Y + 70, col3X + 244, p6Y + 71, 0xFF444444);
        for(int i=0; i<15; i++) {
            g.fill(col3X + 20 + (i*15), p6Y + 60 + (int)(Math.sin(System.currentTimeMillis()/300.0 + i)*12), col3X + 22 + (i*15), p6Y + 62 + (int)(Math.sin(System.currentTimeMillis()/300.0 + i)*12), 0xFF0092C8);
        }

        // --- COL 3: Backup Tasks ---
        int p7Y = y + 216;
        g.fill(col3X, p7Y, col3X + panelWidth, p7Y + 90, 0xFF2A2B2D);
        g.fill(col3X + 1, p7Y + 1, col3X + panelWidth - 1, p7Y + 89, 0xFF1E1E1E);
        g.drawString(this.font, "Backup Tasks", col3X + 10, p7Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "1 of 1 task failed", col3X + 90, p7Y + 10, 0xFFAA00, false);
        g.drawString(this.font, "Rsync", col3X + 20, p7Y + 46, 0xAAAAAA, false);
        g.drawString(this.font, "1 send task", col3X + 90, p7Y + 36, 0xFFAA00, false);
        g.drawString(this.font, "0 receive tasks", col3X + 90, p7Y + 48, 0x55FF55, false);
        g.drawString(this.font, "Total failed: 1", col3X + 90, p7Y + 60, 0xFFAA00, false);

        // --- COL 3: Storage ---
        int p8Y = y + 318;
        g.fill(col3X, p8Y, col3X + panelWidth, p8Y + 130, 0xFF2A2B2D);
        g.fill(col3X + 1, p8Y + 1, col3X + panelWidth - 1, p8Y + 129, 0xFF1E1E1E);
        g.drawString(this.font, "Storage", col3X + 10, p8Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "tank", col3X + 10, p8Y + 30, 0xFFFFFF, false);

        g.drawString(this.font, "Status: ONLINE", col3X + 10, p8Y + 50, 0x55FF55, false);
        g.drawString(this.font, "Used Space: 0%", col3X + 10, p8Y + 65, 0x55FF55, false);
        g.drawString(this.font, "Disks Error: 0", col3X + 10, p8Y + 80, 0x55FF55, false);

        g.drawString(this.font, "Free Space: 1.75 TiB", col3X + 120, p8Y + 50, 0xAAAAAA, false);
        g.drawString(this.font, "Total Disks: 2", col3X + 120, p8Y + 65, 0xAAAAAA, false);

        g.fill(col3X + 10, p8Y + 100, col3X + panelWidth - 10, p8Y + 120, 0xFF222222);
        g.drawString(this.font, "Create Pool", col3X + 100, p8Y + 106, 0x888888, false);
    }

    private void renderMemoryDonutChart(GuiGraphics g, int centerX, int centerY, int outerR, int innerR) {
        int freeColor = 0xFF0092C8;
        int zfsColor = 0xFFB82DB8;
        int serviceColor = 0xFFE6A23C;

        for (int r = innerR; r <= outerR; r++) {
            for (int angle = 0; angle < 360; angle += 2) {
                double rad = Math.toRadians(angle);
                int px = centerX + (int) (r * Math.cos(rad));
                int py = centerY + (int) (r * Math.sin(rad));
                int color = (angle < 280) ? freeColor : (angle < 320 ? zfsColor : serviceColor);
                g.fill(px, py, px + 1, py + 1, color);
            }
        }
    }

    private void renderBarGraph(GuiGraphics g, int x, int y, int w, int h, int color, float[] values, boolean animateFrustration) {
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555); // X Axis
        g.fill(x, y, x + 1, y + h, 0xFF555555);         // Y Axis

        int numBars = values.length;
        int gap = 12;
        int barWidth = (w - (numBars + 1) * gap) / numBars;
        long time = System.currentTimeMillis() / 200;

        for (int i = 0; i < numBars; i++) {
            int barX = x + gap + i * (barWidth + gap);
            float val = values[i];

            if (animateFrustration) {
                float jitter = (float) Math.sin((time + i * 4)) * 0.15f;
                val = Math.max(0.05f, Math.min(1.0f, val + jitter));
            }

            int barH = (int) (val * (h - 20));
            int barY = y + h - 1 - barH;

            g.fill(barX, barY, barX + barWidth, y + h - 1, color);
            g.drawString(this.font, String.valueOf(i + 1), barX + (barWidth / 2) - 3, y + h + 6, 0x888888, false);
        }
    }

    private void renderTerminalMode(GuiGraphics g, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        g.fill(x - 28, y + 10, x, y + 34, currentTab == Tab.ITEMS ? 0xFF2A2B2D : 0xFF141414);
        g.fill(x - 27, y + 11, x, y + 33, currentTab == Tab.ITEMS ? 0xFF212224 : 0xFF1E1E1E);
        g.drawString(this.font, "I", x - 18, y + 18, currentTab == Tab.ITEMS ? 0xFFFFFF : 0x888888, false);

        g.fill(x - 28, y + 38, x, y + 62, currentTab == Tab.FILES ? 0xFF2A2B2D : 0xFF141414);
        g.fill(x - 27, y + 39, x, y + 61, currentTab == Tab.FILES ? 0xFF212224 : 0xFF1E1E1E);
        g.drawString(this.font, "F", x - 18, y + 46, currentTab == Tab.FILES ? 0xFFFFFF : 0x888888, false);

        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A2B2D);
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF212224);

        g.drawString(this.font, "Storage Terminal", x + 8, y + 8, 0xFFFFFF, false);

        int trackX = x + 171;
        int trackY = y + 21;

        if (currentTab == Tab.ITEMS) {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = x + 7 + col * 18;
                    int slotY = y + 20 + row * 18;
                    renderRecessedSlotBox(g, slotX, slotY);
                }
            }

            g.fill(trackX, trackY, trackX + 14, trackY + 108, 0xFF141414);
            int thumbY = trackY + 1 + (int) (this.scrollOffset * 91.0f);
            g.fill(trackX + 1, thumbY, trackX + 13, thumbY + 15, 0xFF0092C8);
            g.fill(trackX + 2, thumbY + 1, trackX + 12, thumbY + 14, 0xFF007BA8);

        } else if (currentTab == Tab.FILES) {
            g.fill(x + 7, y + 20, trackX - 2, y + 108, 0xFF141414);

            List<StoredFile> files = this.menu.getFiles();
            int scrollLines = Math.max(0, files.size() - 6);
            int startIdx = (int)(this.fileScrollOffset * scrollLines);

            int listY = y + 24;
            for (int i = startIdx; i < files.size() && i < startIdx + 6; i++) {
                StoredFile f = files.get(i);
                boolean hover = (mouseY >= listY && mouseY < listY + 14 && mouseX >= x + 8 && mouseX <= trackX - 4);

                if (hover) {
                    g.fill(x + 8, listY, trackX - 3, listY + 14, 0xFF2A2B2D);
                }

                g.drawString(this.font, "📜 " + f.getName(), x + 10, listY + 3, hover ? 0x55FF55 : 0xFFFFFF, false);
                g.drawString(this.font, f.getFormattedSize(), trackX - 45, listY + 3, 0x888888, false);
                listY += 14;
            }

            g.fill(trackX, trackY, trackX + 14, trackY + 108, 0xFF141414);
            int thumbY = trackY + 1 + (int) (this.fileScrollOffset * 91.0f);
            g.fill(trackX + 1, thumbY, trackX + 13, thumbY + 15, 0xFFB82DB8);
            g.fill(trackX + 2, thumbY + 1, trackX + 12, thumbY + 14, 0xFF9C269C);
        }

        g.drawString(this.font, "Inventory", x + 8, y + 133, 0x888888, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderRecessedSlotBox(g, x + 7 + col * 18, y + 144 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            renderRecessedSlotBox(g, x + 7 + col * 18, y + 202);
        }

        int sideX = x + 192;
        int sideW = this.imageWidth - 200;
        if (sideW > 60) {
            g.fill(sideX, y + 21, sideX + sideW, y + 221, 0xFF18191B);

            g.drawString(this.font, "Server Status", sideX + 8, y + 29, 0xFFFFFF, false);
            g.drawString(this.font, "Pool: tank", sideX + 8, y + 45, 0xCCCCCC, false);
            g.drawString(this.font, "Status: ONLINE", sideX + 8, y + 59, 0x55FF55, false);

            int barWidth = Math.min(sideW - 16, 120);

            if (currentTab == Tab.ITEMS) {
                g.drawString(this.font, "Capacity", sideX + 8, y + 79, 0xAAAAAA, false);
                g.fill(sideX + 8, y + 91, sideX + 8 + barWidth, y + 99, 0xFF333333);
                g.fill(sideX + 8, y + 91, sideX + 8 + (int) (barWidth * 0.35f), y + 99, 0xFF0092C8);
                g.drawString(this.font, "35% Used", sideX + 8, y + 103, 0x888888, false);
            } else {
                g.drawString(this.font, "File Capacity (10 MB)", sideX + 8, y + 79, 0xAAAAAA, false);
                g.fill(sideX + 8, y + 91, sideX + 8 + barWidth, y + 99, 0xFF333333);

                int totalBytes = this.menu.getFiles().stream().mapToInt(StoredFile::getSizeInBytes).sum();
                float percent = Math.min(1.0f, totalBytes / (float) (10 * 1024 * 1024));
                g.fill(sideX + 8, y + 91, sideX + 8 + (int) (barWidth * percent), y + 99, 0xFFB82DB8);

                g.drawString(this.font, String.format(java.util.Locale.ROOT, "%.1f%% Used", percent * 100), sideX + 8, y + 103, 0x888888, false);
            }

            g.drawString(this.font, "Drives: 8 Active", sideX + 8, y + 129, 0xCCCCCC, false);
            g.drawString(this.font, "ZFS Cache: 1.1GB", sideX + 8, y + 143, 0xFFB82DB8, false);
            g.drawString(this.font, "Network: 10Gbps", sideX + 8, y + 157, 0xFFE6A23C, false);

            for (int i = 0; i < 8; i++) {
                int ledX = sideX + 8 + (i * 12);
                int ledY = y + 175;
                g.fill(ledX, ledY, ledX + 8, ledY + 8, (i == 7) ? 0xFF0092C8 : 0xFF22C55E);
            }
        }
    }

    private void renderRecessedSlotBox(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 19, y + 19, 0xFF121212);
        g.fill(x, y, x + 19, y + 19, 0xFF4A4D52);
        g.fill(x, y, x + 18, y + 18, 0xFF2B2C2E);
    }

    private void renderFileViewerOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int modalX = this.leftPos + 10;
        int modalY = this.topPos + 15;
        int modalW = Math.max(250, this.imageWidth - 200);
        int modalH = this.imageHeight - 30;

        g.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141415);
        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + modalH - 1, 0xFF1E1F22);

        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + 20, 0xFF2B2D31);
        g.drawString(this.font, "📜 " + openFile.getName() + " (" + openFile.getLanguage() + ")", modalX + 8, modalY + 6, 0xFFFFFF, false);

        g.fill(modalX + modalW - 16, modalY + 4, modalX + modalW - 4, modalY + 16, 0xFFED4245);
        g.drawString(this.font, "X", modalX + modalW - 13, modalY + 6, 0xFFFFFF, false);

        g.fill(modalX + 8, modalY + 26, modalX + modalW - 8, modalY + modalH - 8, 0xFF111214);

        String[] rawLines = openFile.getContent().split("\n");
        int drawY = modalY + 30;
        int maxTextWidth = modalW - 24;

        for (int i = 0; i < rawLines.length && drawY < modalY + modalH - 15; i++) {
            String line = rawLines[i];

            if (line.isEmpty()) {
                drawY += 10;
                continue;
            }

            while (!line.isEmpty() && drawY < modalY + modalH - 15) {
                String renderLine = line;
                // Safely reduce the line character by character until it fits within the box
                while (this.font.width(renderLine) > maxTextWidth && renderLine.length() > 0) {
                    renderLine = renderLine.substring(0, renderLine.length() - 1);
                }

                g.drawString(this.font, renderLine, modalX + 12, drawY, 0x55FF55, false);
                line = line.substring(renderLine.length());
                drawY += 10;
            }
        }
    }

    private void handleUpload() {
        this.menu.addFileClient("test_script.js", "js", "console.log('Server Online');\nfunction test() {\n  return true;\n}");
    }

    private void handleDelete() {
        if (this.openFile != null) {
            this.menu.removeFileClient(this.openFile);
            this.openFile = null;
            updateVisibility();
        } else {
            List<StoredFile> files = this.menu.getFiles();
            if (!files.isEmpty()) {
                this.menu.removeFileClient(files.get(files.size() - 1));
            }
        }
    }

    private static final int LARGE_COUNT_THRESHOLD = 1000;

    private String formatLargeNumber(int number) {
        if (number >= 1_000_000) {
            return formatWithSuffix(number, 1_000_000, "M");
        }
        if (number >= LARGE_COUNT_THRESHOLD) {
            return formatWithSuffix(number, 1_000, "k");
        }
        return String.valueOf(number);
    }

    private String formatWithSuffix(int number, int divisor, String suffix) {
        double value = number / (double) divisor;
        String formatted = String.format(java.util.Locale.ROOT, "%.1f", value).replace('.', ',');
        if (formatted.endsWith(",0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffix;
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltip = super.getTooltipFromContainerItem(stack);
        if (stack.getCount() >= LARGE_COUNT_THRESHOLD) {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add(Component.literal(String.format(java.util.Locale.ROOT, "%,d", stack.getCount()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }
}