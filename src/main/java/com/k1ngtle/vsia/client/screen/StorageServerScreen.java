package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.StorageServerBlockEntity;
import com.k1ngtle.vsia.signality.internet.server.StoredFile;
import com.k1ngtle.vsia.world.inventory.StorageServerMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class StorageServerScreen extends AbstractContainerScreen<StorageServerMenu> {

    private boolean isDashboard = true;

    // Tabs for Terminal View
    private enum Tab { ITEMS, FILES }
    private Tab currentTab = Tab.ITEMS;

    private float scrollOffset = 0.0f;
    private float fileScrollOffset = 0.0f;

    // Viewer modal for reading file code
    private StoredFile openFile = null;

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

    private TerminalSize currentSize = TerminalSize.NORMAL;

    private Button modeToggleButton;
    private Button sizeToggleButton;
    private Button checkUpdatesButton;
    private EditBox searchBox;

    public StorageServerScreen(StorageServerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 660;
        this.imageHeight = 380;
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
        ).bounds(this.leftPos + 18, this.topPos + 195, 120, 20).build();
        this.checkUpdatesButton.visible = isDashboard;
        this.addRenderableWidget(this.checkUpdatesButton);

        this.searchBox = new EditBox(this.font, this.leftPos + 115, this.topPos + 6, 85, 12, Component.literal("Search"));
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(!isDashboard);
        this.addRenderableWidget(this.searchBox);
    }

    private void updateDimensions() {
        if (isDashboard) {
            this.imageWidth = 660;
            this.imageHeight = 380;
        } else {
            this.imageWidth = currentSize.width;
            this.imageHeight = currentSize.height;
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
    }

    private void toggleMode() {
        this.isDashboard = !this.isDashboard;
        this.openFile = null;
        updateVisibility();

        this.modeToggleButton.setMessage(Component.literal(isDashboard ? "Configure" : "Dashboard"));
        this.sizeToggleButton.visible = !isDashboard;
        this.checkUpdatesButton.visible = isDashboard;
        this.searchBox.setVisible(!isDashboard);
        updateDimensions();
        repositionWidgets();
    }

    private void updateVisibility() {
        this.menu.slotsVisible = (!this.isDashboard && this.currentTab == Tab.ITEMS && this.openFile == null);
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
            this.checkUpdatesButton.setPosition(this.leftPos + 18, this.topPos + 195);
        }
        if (this.searchBox != null) {
            this.searchBox.setPosition(this.leftPos + 115, this.topPos + 6);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        if (isDashboard) {
            renderDashboardMode(g);
        } else {
            renderTerminalMode(g);
        }

        // Hide count overlay on large stacks temporarily during super render
        int[] originalCounts = new int[this.menu.slots.size()];
        if (this.menu.slotsVisible) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem()) {
                    originalCounts[i] = slot.getItem().getCount();
                    if (originalCounts[i] >= LARGE_COUNT_THRESHOLD) {
                        slot.getItem().setCount(1);
                    }
                }
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (this.menu.slotsVisible) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem() && originalCounts[i] >= LARGE_COUNT_THRESHOLD) {
                    slot.getItem().setCount(originalCounts[i]);
                }
            }
        }

        if (!isDashboard) {
            g.fill(this.leftPos + 105, this.topPos + 4, this.leftPos + 210, this.topPos + 20, 0xFF141414);
            g.fill(this.leftPos + 106, this.topPos + 5, this.leftPos + 209, this.topPos + 19, 0xFF000000);
            this.searchBox.render(g, mouseX, mouseY, partialTick);

            if (this.currentTab == Tab.ITEMS && openFile == null) {
                renderLargeStackCounts(g);
            }

            if (openFile != null) {
                renderFileViewerOverlay(g, mouseX, mouseY);
            }

            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isDashboard) {
            int x = this.leftPos;
            int y = this.topPos;

            // Handle file viewer overlay close button click
            if (openFile != null) {
                int modalX = x + 10;
                int modalY = y + 15;
                int closeBtnX = modalX + 235;
                int closeBtnY = modalY + 5;
                if (mouseX >= closeBtnX && mouseX <= closeBtnX + 12 && mouseY >= closeBtnY && mouseY <= closeBtnY + 12) {
                    openFile = null;
                    updateVisibility();
                    return true;
                }
                return true; // Block clicks outside close button while viewer is active
            }

            // Check Left Side Tab Clicks
            int sx = x - 26;
            int sy1 = y + 20;
            int sy2 = y + 50;

            if (mouseX >= sx && mouseX <= sx + 26) {
                if (mouseY >= sy1 && mouseY <= sy1 + 26) {
                    this.currentTab = Tab.ITEMS;
                    updateVisibility();
                    return true;
                } else if (mouseY >= sy2 && mouseY <= sy2 + 26) {
                    this.currentTab = Tab.FILES;
                    updateVisibility();
                    return true;
                }
            }

            // Check file item clicks in file tab
            if (currentTab == Tab.FILES) {
                int listX = x + 7;
                int listY = y + 20;
                List<StoredFile> files = this.menu.getFiles();
                int startIndex = (int) (this.fileScrollOffset * Math.max(0, files.size() - 6));

                for (int i = 0; i < 6; i++) {
                    int fileIdx = startIndex + i;
                    if (fileIdx < files.size()) {
                        int rowY = listY + 2 + (i * 18);
                        if (mouseX >= listX && mouseX <= listX + 162 && mouseY >= rowY && mouseY <= rowY + 16) {
                            this.openFile = files.get(fileIdx);
                            updateVisibility();
                            return true;
                        }
                    }
                }
            }
        } else {
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
            } else {
                this.fileScrollOffset = (float) Math.max(0.0, Math.min(1.0, this.fileScrollOffset - (delta * 0.1)));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isDashboard && button == 0 && openFile == null) {
            int trackX = this.leftPos + 171;
            int trackY = this.topPos + 22;
            if (mouseX >= trackX && mouseX <= trackX + 14 && mouseY >= trackY && mouseY <= trackY + 108) {
                float offset = (float) (mouseY - (trackY + 7.5)) / 91.0f;
                offset = Math.max(0.0f, Math.min(1.0f, offset));

                if (currentTab == Tab.ITEMS) {
                    this.scrollOffset = offset;
                    this.menu.scrollTo(this.scrollOffset);
                } else {
                    this.fileScrollOffset = offset;
                }
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
        if (openFile != null && keyCode == 256) { // ESC key closes code viewer
            openFile = null;
            updateVisibility();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderDashboardMode(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;

        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1E1E1E);
        g.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF242424);

        g.drawString(this.font, "Dashboard", x + 16, y - 14, 0xFFFFFF, true);

        // System Information
        int p1X = x + 12;
        int p1Y = y + 12;
        g.fill(p1X, p1Y, p1X + 195, p1Y + 210, 0xFF2A2B2D);
        g.fill(p1X + 1, p1Y + 1, p1X + 194, p1Y + 209, 0xFF1E1E1E);

        g.drawString(this.font, "TrueNAS SCALE", p1X + 10, p1Y + 10, 0xFFFFFF, false);
        g.fill(p1X + 10, p1Y + 24, p1X + 185, p1Y + 38, 0xFF111111);
        g.fill(p1X + 15, p1Y + 29, p1X + 20, p1Y + 33, 0xFF22C55E);
        g.fill(p1X + 23, p1Y + 29, p1X + 28, p1Y + 33, 0xFF3B82F6);

        g.drawString(this.font, "System Information", p1X + 10, p1Y + 46, 0xFF0092C8, false);
        g.fill(p1X + 10, p1Y + 58, p1X + 185, p1Y + 59, 0xFF333333);

        g.drawString(this.font, "Platform: TRUENAS-MINI-R", p1X + 10, p1Y + 66, 0xAAAAAA, false);
        g.drawString(this.font, "Version: ElectricEel-24.10...", p1X + 10, p1Y + 80, 0xAAAAAA, false);
        g.drawString(this.font, "Hostname: re-minir-102", p1X + 10, p1Y + 108, 0xAAAAAA, false);
        g.drawString(this.font, "Uptime: 1h 28m", p1X + 10, p1Y + 122, 0x888888, false);

        // Memory Donut Chart
        int p2X = x + 12;
        int p2Y = y + 232;
        g.fill(p2X, p2Y, p2X + 195, p2Y + 136, 0xFF2A2B2D);
        g.fill(p2X + 1, p2Y + 1, p2X + 194, p2Y + 135, 0xFF1E1E1E);

        g.drawString(this.font, "Memory", p2X + 10, p2Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "31.3 GiB", p2X + 10, p2Y + 26, 0xFFFFFF, true);
        g.drawString(this.font, "Total available", p2X + 10, p2Y + 38, 0x777777, false);

        g.drawString(this.font, "Free: 27.0 GiB", p2X + 10, p2Y + 64, 0xFF0092C8, false);
        g.drawString(this.font, "ZFS Cache: 1.1 GiB", p2X + 10, p2Y + 78, 0xFFB82DB8, false);
        g.drawString(this.font, "Services: 3.2 GiB", p2X + 10, p2Y + 92, 0xFFE6A23C, false);

        renderMemoryDonutChart(g, p2X + 145, p2Y + 70, 30, 20);

        // CPU Usage & Temp Charts
        int p3X = x + 215;
        int p3Y = y + 12;
        g.fill(p3X, p3Y, p3X + 220, p3Y + 175, 0xFF2A2B2D);
        g.fill(p3X + 1, p3Y + 1, p3X + 219, p3Y + 174, 0xFF1E1E1E);

        g.drawString(this.font, "CPU Usage Per Core", p3X + 10, p3Y + 8, 0xFFFFFF, false);
        renderBarGraph(g, p3X + 25, p3Y + 25, 180, 130, 0xFF0092C8, new float[]{0.22f, 0.48f, 0.16f, 0.75f, 0.40f, 0.68f, 0.14f, 0.48f});

        int p4Y = y + 197;
        g.fill(p3X, p4Y, p3X + 220, p4Y + 171, 0xFF2A2B2D);
        g.fill(p3X + 1, p4Y + 1, p3X + 219, p4Y + 170, 0xFF1E1E1E);

        g.drawString(this.font, "CPU Temperature Per Core", p3X + 10, p4Y + 8, 0xFFFFFF, false);
        renderBarGraph(g, p3X + 25, p4Y + 25, 180, 126, 0xFFB82DB8, new float[]{0.65f, 0.68f, 0.64f, 0.70f, 0.66f, 0.67f, 0.63f, 0.65f});

        // Storage Info
        int p5X = x + 443;
        int p5Y = y + 12;
        g.fill(p5X, p5Y, p5X + 205, p5Y + 110, 0xFF2A2B2D);
        g.fill(p5X + 1, p5Y + 1, p5X + 204, p5Y + 109, 0xFF1E1E1E);

        g.drawString(this.font, "Storage", p5X + 10, p5Y + 8, 0xFFFFFF, false);
        g.fill(p5X + 10, p5Y + 22, p5X + 195, p5Y + 23, 0xFF333333);
        g.drawString(this.font, "tank", p5X + 10, p5Y + 32, 0xFFFFFF, false);
        g.drawString(this.font, "Pool Status: ONLINE", p5X + 10, p5Y + 52, 0x55FF55, false);
        g.drawString(this.font, "Free Space: 1.75 TiB", p5X + 10, p5Y + 66, 0xAAAAAA, false);

        int p6Y = y + 130;
        g.fill(p5X, p6Y, p5X + 205, p6Y + 85, 0xFF2A2B2D);
        g.fill(p5X + 1, p6Y + 1, p5X + 204, p6Y + 84, 0xFF1E1E1E);

        g.drawString(this.font, "CPU Model", p5X + 10, p6Y + 8, 0xFFFFFF, false);
        g.drawString(this.font, "Intel(R) Atom(TM) CPU", p5X + 10, p6Y + 38, 0xCCCCCC, false);
        g.drawString(this.font, "C3758 @ 2.20GHz", p5X + 10, p6Y + 52, 0x888888, false);

        int p7Y = y + 225;
        g.fill(p5X, p7Y, p5X + 205, p7Y + 143, 0xFF2A2B2D);
        g.fill(p5X + 1, p7Y + 1, p5X + 204, p7Y + 142, 0xFF1E1E1E);

        g.drawString(this.font, "Backup Tasks", p5X + 10, p7Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "1 of 1 task failed", p5X + 10, p7Y + 38, 0xFFAA00, false);
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

    private void renderBarGraph(GuiGraphics g, int x, int y, int w, int h, int color, float[] values) {
        g.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x, y, x + 1, y + h, 0xFF555555);

        int numBars = values.length;
        int gap = 8;
        int barWidth = (w - (numBars + 1) * gap) / numBars;

        for (int i = 0; i < numBars; i++) {
            int barX = x + gap + i * (barWidth + gap);
            int barH = (int) (values[i] * (h - 15));
            int barY = y + h - 1 - barH;
            g.fill(barX, barY, barX + barWidth, y + h - 1, color);
            g.drawString(this.font, String.valueOf(i + 1), barX + (barWidth / 2) - 3, y + h + 2, 0x888888, false);
        }
    }

    private void renderTerminalMode(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;

        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A2B2D);
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF212224);

        g.drawString(this.font, "Storage Terminal", x + 8, y + 8, 0xFFFFFF, false);

        // --- Render Side Tabs ---
        int sx = x - 26;
        int sy1 = y + 20;
        int sy2 = y + 50;

        boolean itemsActive = (currentTab == Tab.ITEMS);
        g.fill(sx, sy1, x + 1, sy1 + 26, 0xFF2A2B2D);
        g.fill(sx + 1, sy1 + 1, x + 1, sy1 + 25, itemsActive ? 0xFF212224 : 0xFF18191B);
        g.renderItem(new ItemStack(Items.CHEST), sx + 5, sy1 + 5);

        boolean filesActive = (currentTab == Tab.FILES);
        g.fill(sx, sy2, x + 1, sy2 + 26, 0xFF2A2B2D);
        g.fill(sx + 1, sy2 + 1, x + 1, sy2 + 25, filesActive ? 0xFF212224 : 0xFF18191B);
        g.renderItem(new ItemStack(Items.PAPER), sx + 5, sy2 + 5);

        // --- Main Center Content ---
        if (currentTab == Tab.ITEMS) {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    int slotX = x + 7 + col * 18;
                    int slotY = y + 20 + row * 18;
                    renderRecessedSlotBox(g, slotX, slotY);
                }
            }

            int trackX = x + 171;
            int trackY = y + 21;
            g.fill(trackX, trackY, trackX + 14, trackY + 108, 0xFF141414);

            int thumbY = trackY + 1 + (int) (this.scrollOffset * 91.0f);
            g.fill(trackX + 1, thumbY, trackX + 13, thumbY + 15, 0xFF0092C8);
            g.fill(trackX + 2, thumbY + 1, trackX + 12, thumbY + 14, 0xFF007BA8);
        } else {
            int listX = x + 7;
            int listY = y + 20;
            g.fill(listX, listY, listX + 162, listY + 108, 0xFF18191B);

            List<StoredFile> files = this.menu.getFiles();
            int startIndex = (int) (this.fileScrollOffset * Math.max(0, files.size() - 6));
            for (int i = 0; i < 6; i++) {
                int fileIdx = startIndex + i;
                if (fileIdx < files.size()) {
                    StoredFile file = files.get(fileIdx);
                    int rowY = listY + 2 + (i * 18);

                    g.fill(listX + 2, rowY, listX + 160, rowY + 16, 0xFF242528);
                    g.drawString(this.font, "📄 " + file.getName(), listX + 4, rowY + 4, 0xFFFFFF, false);
                    g.drawString(this.font, file.getFormattedSize(), listX + 156 - this.font.width(file.getFormattedSize()), rowY + 4, 0x888888, false);
                }
            }

            int trackX = x + 171;
            int trackY = y + 21;
            g.fill(trackX, trackY, trackX + 14, trackY + 108, 0xFF141414);
            int thumbY = trackY + 1 + (int) (this.fileScrollOffset * 91.0f);
            g.fill(trackX + 1, thumbY, trackX + 13, thumbY + 15, 0xFFB82DB8);
            g.fill(trackX + 2, thumbY + 1, trackX + 12, thumbY + 14, 0xFF9E2A9E);
        }

        // Inventory
        g.drawString(this.font, "Inventory", x + 8, y + 133, 0x888888, false);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                renderRecessedSlotBox(g, x + 7 + col * 18, y + 144 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            renderRecessedSlotBox(g, x + 7 + col * 18, y + 202);
        }

        // Side Panel
        int sideX = x + 192;
        int sideW = this.imageWidth - 200;
        if (sideW > 60) {
            g.fill(sideX, y + 21, sideX + sideW, y + 221, 0xFF18191B);

            g.drawString(this.font, "Server Status", sideX + 8, y + 29, 0xFFFFFF, false);
            g.drawString(this.font, "Pool: tank", sideX + 8, y + 45, 0xCCCCCC, false);
            g.drawString(this.font, "Status: ONLINE", sideX + 8, y + 59, 0x55FF55, false);

            g.drawString(this.font, "Item Capacity", sideX + 8, y + 77, 0xAAAAAA, false);
            int barWidth = Math.min(sideW - 16, 120);
            g.fill(sideX + 8, y + 87, sideX + 8 + barWidth, y + 91, 0xFF333333);
            g.fill(sideX + 8, y + 87, sideX + 8 + (int) (barWidth * 0.35f), y + 91, 0xFF0092C8);

            // 10 MB Capacity Calculation
            int usedBytes = 0;
            if (this.menu.blockEntity != null) {
                usedBytes = this.menu.blockEntity.getTotalUsedFileBytes();
            }
            float fileRatio = (float) usedBytes / (float) StorageServerBlockEntity.MAX_FILE_STORAGE_BYTES;

            g.drawString(this.font, "File Storage (10 MB)", sideX + 8, y + 99, 0xAAAAAA, false);
            g.fill(sideX + 8, y + 109, sideX + 8 + barWidth, y + 113, 0xFF333333);
            g.fill(sideX + 8, y + 109, sideX + 8 + (int) (barWidth * Math.min(1.0f, fileRatio)), y + 113, 0xFFB82DB8);

            g.drawString(this.font, "Drives: 8 Active", sideX + 8, y + 127, 0xCCCCCC, false);
            g.drawString(this.font, "ZFS Cache: 1.1GB", sideX + 8, y + 141, 0xFFB82DB8, false);
            g.drawString(this.font, "Network: 10Gbps", sideX + 8, y + 155, 0xFFE6A23C, false);

            for (int i = 0; i < 8; i++) {
                int ledX = sideX + 8 + (i * 12);
                int ledY = y + 175;
                g.fill(ledX, ledY, ledX + 8, ledY + 8, (i == 7) ? 0xFF0092C8 : 0xFF22C55E);
            }
        }
    }

    private void renderFileViewerOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int modalX = this.leftPos + 10;
        int modalY = this.topPos + 15;
        int modalW = 250;
        int modalH = 210;

        g.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141415);
        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + modalH - 1, 0xFF1E1F22);

        // File Header bar
        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + 20, 0xFF2B2D31);
        g.drawString(this.font, "📜 " + openFile.getName() + " (" + openFile.getLanguage() + ")", modalX + 8, modalY + 6, 0xFFFFFF, false);

        // Close button 'X'
        g.fill(modalX + modalW - 16, modalY + 4, modalX + modalW - 4, modalY + 16, 0xFFED4245);
        g.drawString(this.font, "X", modalX + modalW - 13, modalY + 6, 0xFFFFFF, false);

        // Code content viewing box
        g.fill(modalX + 8, modalY + 26, modalX + modalW - 8, modalY + modalH - 8, 0xFF111214);

        String[] lines = openFile.getContent().split("\n");
        for (int i = 0; i < Math.min(lines.length, 18); i++) {
            g.drawString(this.font, lines[i], modalX + 12, modalY + 30 + (i * 9), 0x55FF55, false);
        }
    }

    private void renderRecessedSlotBox(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 19, y + 19, 0xFF121212);
        g.fill(x, y, x + 19, y + 19, 0xFF4A4D52);
        g.fill(x, y, x + 18, y + 18, 0xFF2B2C2E);
    }

    private void renderLargeStackCounts(GuiGraphics g) {
        for (Slot slot : this.menu.slots) {
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                int count = stack.getCount();
                if (count >= LARGE_COUNT_THRESHOLD) {
                    String formatted = formatLargeNumber(count);
                    int slotX = this.leftPos + slot.x;
                    int slotY = this.topPos + slot.y;

                    g.pose().pushPose();
                    g.pose().translate(0, 0, 200);
                    g.drawString(this.font, formatted, slotX + 17 - this.font.width(formatted), slotY + 9, 0xFFFFFF, true);
                    g.pose().popPose();
                }
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
        String formatted = String.format(java.util.Locale.ROOT, "%.1f", value);
        if (formatted.endsWith(".0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted.replace('.', ',') + suffix;
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