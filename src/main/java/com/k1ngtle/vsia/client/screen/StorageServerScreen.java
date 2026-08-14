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
        this.imageWidth = 840; // Wide enough for a 3-column TrueNAS dash
        this.imageHeight = 460;
        updateVisibility();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelY = 10000;

        updateDimensions();

        // Used in BOTH modes, but position/text changes
        this.modeToggleButton = Button.builder(
                Component.literal(isDashboard ? "Configure" : "Dashboard"),
                b -> toggleMode()
        ).bounds(this.leftPos + this.imageWidth - 95, this.topPos + 8, 85, 18).build();
        this.addRenderableWidget(this.modeToggleButton);

        // Terminal Only Buttons
        this.sizeToggleButton = Button.builder(
                Component.literal(currentSize.label),
                b -> toggleSize()
        ).bounds(this.leftPos + 10, this.topPos - 22, 90, 18).build();
        this.sizeToggleButton.visible = !isDashboard;
        this.addRenderableWidget(this.sizeToggleButton);

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

        // Dashboard Only Buttons
        this.checkUpdatesButton = Button.builder(
                Component.literal("Check for Updates"),
                b -> {}
        ).bounds(this.leftPos + 35, this.topPos + 225, 120, 20).build();
        this.checkUpdatesButton.visible = isDashboard;
        this.addRenderableWidget(this.checkUpdatesButton);
    }

    private void updateDimensions() {
        if (isDashboard) {
            this.imageWidth = 840;
            this.imageHeight = 480;
        } else {
            this.imageWidth = currentSize.width;
            this.imageHeight = currentSize.height;
        }
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
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
            if (isDashboard) {
                this.modeToggleButton.setPosition(this.leftPos + this.imageWidth - 95, this.topPos + 8);
            } else {
                this.modeToggleButton.setPosition(this.leftPos + this.imageWidth - 95, this.topPos - 22);
            }
        }
        if (this.sizeToggleButton != null) {
            this.sizeToggleButton.setPosition(this.leftPos + 10, this.topPos - 22);
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
        if (this.checkUpdatesButton != null) {
            this.checkUpdatesButton.setPosition(this.leftPos + 35, this.topPos + 225);
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

        // ---- ITEM RENDERING TRICK ----
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

        // Restore & Draw custom numbers
        if (!isDashboard && currentTab == Tab.ITEMS && openFile == null) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem() && realCounts[i] >= 1000) {
                    slot.getItem().setCount(realCounts[i]); // Restore immediately

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
        // ------------------------------

        if (!isDashboard) {
            // Search box background
            g.fill(this.leftPos + 105, this.topPos + 4, this.leftPos + 210, this.topPos + 20, 0xFF141414);
            g.fill(this.leftPos + 106, this.topPos + 5, this.leftPos + 209, this.topPos + 19, 0xFF000000);
            this.searchBox.render(g, mouseX, mouseY, partialTick);

            if (this.currentTab == Tab.FILES && openFile == null) {
                this.uploadButton.render(g, mouseX, mouseY, partialTick);
                this.deleteButton.render(g, mouseX, mouseY, partialTick);
            }

            if (openFile != null) {
                renderFileViewerOverlay(g, mouseX, mouseY);
            }

            if (this.currentTab == Tab.ITEMS && openFile == null) {
                this.renderTooltip(g, mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        // Handled completely in render()
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isDashboard) {
            if (this.uploadButton.isMouseOver(mouseX, mouseY) && this.uploadButton.visible) {
                this.uploadButton.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            if (this.deleteButton.isMouseOver(mouseX, mouseY) && this.deleteButton.visible) {
                this.deleteButton.mouseClicked(mouseX, mouseY, button);
                return true;
            }

            int x = this.leftPos;
            int y = this.topPos;

            // Side Tabs
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

            // File Viewer Modal
            if (this.currentTab == Tab.FILES) {
                if (openFile != null) {
                    int modalX = x + 10;
                    int modalY = y + 15;
                    int modalW = Math.max(250, this.imageWidth - 20); // Scale with terminal size
                    if (mouseX >= modalX + modalW - 16 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 16) {
                        this.openFile = null;
                        updateVisibility();
                        return true;
                    }
                } else {
                    List<StoredFile> files = this.menu.getFiles();
                    int listY = y + 25;
                    for (int i = 0; i < files.size(); i++) {
                        if (mouseY >= listY && mouseY < listY + 14 && mouseX >= x + 8 && mouseX <= x + 160) {
                            this.openFile = files.get(i);
                            updateVisibility();
                            return true;
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
            return false; // Disable vanilla slot clicking on dashboard
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isDashboard) {
            if (currentTab == Tab.ITEMS) {
                this.scrollOffset = (float) Math.max(0.0, Math.min(1.0, this.scrollOffset - (delta * 0.1)));
                this.menu.scrollTo(this.scrollOffset);
            } else if (currentTab == Tab.FILES) {
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
                return true; // Prevents 'E' from closing GUI while typing
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderFileViewerOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int modalX = this.leftPos + 10;
        int modalY = this.topPos + 15;
        int modalW = Math.max(250, this.imageWidth - 20);
        int modalH = this.imageHeight - 30;

        g.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141415);
        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + modalH - 1, 0xFF1E1F22);

        // Header bar
        g.fill(modalX + 1, modalY + 1, modalX + modalW - 1, modalY + 20, 0xFF2B2D31);
        g.drawString(this.font, "📜 " + openFile.getName() + " (" + openFile.getLanguage() + ")", modalX + 8, modalY + 6, 0xFFFFFF, false);

        // Close 'X'
        g.fill(modalX + modalW - 16, modalY + 4, modalX + modalW - 4, modalY + 16, 0xFFED4245);
        g.drawString(this.font, "X", modalX + modalW - 13, modalY + 6, 0xFFFFFF, false);

        // Content body
        g.fill(modalX + 8, modalY + 26, modalX + modalW - 8, modalY + modalH - 8, 0xFF111214);

        String[] lines = openFile.getContent().split("\n");
        // Calculate max chars based on modal width. font.width("a") is usually 5-6 pixels.
        int maxCharsPerLine = (modalW - 24) / 6;
        int drawY = modalY + 30;

        for (int i = 0; i < lines.length && drawY < modalY + modalH - 15; i++) {
            String line = lines[i];

            // Text Wrapping logic
            while (line.length() > 0 && drawY < modalY + modalH - 15) {
                int breakIndex = Math.min(line.length(), maxCharsPerLine);
                String renderLine = line.substring(0, breakIndex);
                g.drawString(this.font, renderLine, modalX + 12, drawY, 0x55FF55, false);
                line = line.substring(breakIndex);
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

    private void renderTerminalMode(GuiGraphics g, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Side Tabs
        g.fill(x - 28, y + 10, x, y + 34, currentTab == Tab.ITEMS ? 0xFF2A2B2D : 0xFF141414);
        g.fill(x - 27, y + 11, x, y + 33, currentTab == Tab.ITEMS ? 0xFF212224 : 0xFF1E1E1E);
        g.drawString(this.font, "I", x - 18, y + 18, currentTab == Tab.ITEMS ? 0xFFFFFF : 0x888888, false);

        g.fill(x - 28, y + 38, x, y + 62, currentTab == Tab.FILES ? 0xFF2A2B2D : 0xFF141414);
        g.fill(x - 27, y + 39, x, y + 61, currentTab == Tab.FILES ? 0xFF212224 : 0xFF1E1E1E);
        g.drawString(this.font, "F", x - 18, y + 46, currentTab == Tab.FILES ? 0xFFFFFF : 0x888888, false);

        // Main Background
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
        } else {
            // Files Tab
            g.fill(x + 7, y + 20, trackX - 2, y + 108, 0xFF141414);

            List<StoredFile> files = this.menu.getFiles();
            int listY = y + 25;
            for (int i = 0; i < files.size() && i < 6; i++) {
                StoredFile f = files.get(i);
                g.drawString(this.font, "📜 " + f.getName(), x + 10, listY, 0xFFFFFF, false);
                g.drawString(this.font, f.getFormattedSize(), x + 125, listY, 0x888888, false);
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

    // -------------------------------------------------------------------------------------
    // TRUENAS DASHBOARD RE-DESIGN
    // -------------------------------------------------------------------------------------
    private void renderDashboardMode(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;

        // Base Dashboard Background
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF141517); // Darker border
        g.fill(x + 1, y + 32, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF1A1B1D); // Main BG

        // Header Bar
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + 31, 0xFF252526);
        g.drawString(this.font, "Dashboard", x + 16, y + 12, 0xFFFFFF, false);

        int panelGap = 12;
        int colW = (this.imageWidth - (panelGap * 4)) / 3;

        // ================= COLUMN 1 =================
        int c1X = x + panelGap;

        // --- Widget: System Information ---
        int w1Y = y + 42;
        int w1H = 260;
        drawPanelBg(g, c1X, w1Y, colW, w1H);

        // TrueNAS Logo & Server Rack Mock
        g.drawString(this.font, "TrueNAS", c1X + 15, w1Y + 15, 0xFFFFFF, false);
        g.pose().pushPose();
        g.pose().scale(0.5f, 0.5f, 1f);
        g.drawString(this.font, "SCALE", (c1X + 60) * 2, (w1Y + 17) * 2, 0xAAAAAA, false);
        g.pose().popPose();

        int rackY = w1Y + 40;
        g.fill(c1X + 15, rackY, c1X + colW - 15, rackY + 30, 0xFF111111);
        g.fill(c1X + 15, rackY, c1X + colW - 15, rackY + 1, 0xFF333333);
        g.fill(c1X + 15, rackY + 29, c1X + colW - 15, rackY + 30, 0xFF333333);
        for(int i = 0; i < 8; i++) {
            g.fill(c1X + 20 + (i*28), rackY + 12, c1X + 42 + (i*28), rackY + 26, 0xFF222222); // Drive bays
            g.fill(c1X + 22 + (i*28), rackY + 20, c1X + 24 + (i*28), rackY + 22, (i==2)?0xFF3B82F6:0xFF22C55E); // LEDs
        }

        g.drawString(this.font, "System Information", c1X + 15, w1Y + 85, 0xFFFFFF, false);
        g.drawString(this.font, "Overview", c1X + 15, w1Y + 105, 0xAAAAAA, false);
        g.drawString(this.font, "Platform:", c1X + 15, w1Y + 125, 0x888888, false);
        g.drawString(this.font, "TRUENAS-MINI-R", c1X + 85, w1Y + 125, 0xFFFFFF, false);
        g.drawString(this.font, "Version:", c1X + 15, w1Y + 145, 0x888888, false);
        g.drawString(this.font, "ElectricEel-24.10...", c1X + 85, w1Y + 145, 0xFFFFFF, false);
        g.drawString(this.font, "Hostname:", c1X + 15, w1Y + 165, 0x888888, false);
        g.drawString(this.font, "re-minir-102", c1X + 85, w1Y + 165, 0xFFFFFF, false);
        g.drawString(this.font, "Uptime:", c1X + 15, w1Y + 185, 0x888888, false);
        g.drawString(this.font, "1h 28m", c1X + 85, w1Y + 185, 0xFFFFFF, false);

        // --- Widget: Memory ---
        int w2Y = w1Y + w1H + panelGap;
        int w2H = 160;
        drawPanelBg(g, c1X, w2Y, colW, w2H);

        g.drawString(this.font, "Memory", c1X + 15, w2Y + 15, 0xFFFFFF, false);

        g.pose().pushPose();
        g.pose().scale(2.5f, 2.5f, 1f);
        g.drawString(this.font, "31.3", (int)((c1X + 15)/2.5f), (int)((w2Y + 45)/2.5f), 0xFFFFFF, false);
        g.pose().popPose();
        g.drawString(this.font, "GiB", c1X + 72, w2Y + 55, 0xAAAAAA, false);
        g.drawString(this.font, "total available (ECC)", c1X + 15, w2Y + 75, 0x888888, false);

        g.fill(c1X + 15, w2Y + 100, c1X + 21, w2Y + 106, 0xFF0092C8);
        g.drawString(this.font, "Free: 27.0 GiB", c1X + 28, w2Y + 99, 0xAAAAAA, false);
        g.fill(c1X + 15, w2Y + 115, c1X + 21, w2Y + 121, 0xFFB82DB8);
        g.drawString(this.font, "ZFS Cache: 1.1 GiB", c1X + 28, w2Y + 114, 0xAAAAAA, false);
        g.fill(c1X + 15, w2Y + 130, c1X + 21, w2Y + 136, 0xFFE6A23C);
        g.drawString(this.font, "Services: 3.2 GiB", c1X + 28, w2Y + 129, 0xAAAAAA, false);

        renderMemoryDonutChart(g, c1X + 185, w2Y + 85, 45, 30);


        // ================= COLUMN 2 =================
        int c2X = c1X + colW + panelGap;

        // --- Widget: CPU Usage Per Core ---
        int w3Y = y + 42;
        int w3H = 125;
        drawPanelBg(g, c2X, w3Y, colW, w3H);
        g.drawString(this.font, "CPU Usage Per Core", c2X + 15, w3Y + 15, 0xFFFFFF, false);
        renderBarGraph(g, c2X + 30, w3Y + 35, colW - 50, 75, 0xFF0092C8, new float[]{0.22f, 0.48f, 0.16f, 0.75f, 0.40f, 0.68f, 0.14f, 0.48f});

        // --- Widget: CPU Temp Per Core ---
        int w4Y = w3Y + w3H + panelGap;
        int w4H = 125;
        drawPanelBg(g, c2X, w4Y, colW, w4H);
        g.drawString(this.font, "CPU Temperature Per Core", c2X + 15, w4Y + 15, 0xFFFFFF, false);
        renderBarGraph(g, c2X + 30, w4Y + 35, colW - 50, 75, 0xFFB82DB8, new float[]{0.65f, 0.68f, 0.64f, 0.70f, 0.66f, 0.67f, 0.63f, 0.65f});

        // --- Widget: Storage ---
        int w5Y = w4Y + w4H + panelGap;
        int w5H = 160;
        drawPanelBg(g, c2X, w5Y, colW, w5H);
        g.drawString(this.font, "Storage", c2X + 15, w5Y + 15, 0xFFFFFF, false);
        g.drawString(this.font, "tank", c2X + 15, w5Y + 35, 0xFFFFFF, false);

        g.drawString(this.font, "Status: ONLINE", c2X + 15, w5Y + 55, 0xFF22C55E, false);
        g.drawString(this.font, "Used Space: 0%", c2X + 15, w5Y + 68, 0xFF22C55E, false);
        g.drawString(this.font, "Disks Error: 0", c2X + 15, w5Y + 81, 0xFF22C55E, false);

        g.drawString(this.font, "Free Space: 1.75 TiB", c2X + 125, w5Y + 55, 0xAAAAAA, false);
        g.drawString(this.font, "Total Disks: 2", c2X + 125, w5Y + 68, 0xAAAAAA, false);

        g.fill(c2X + 15, w5Y + 105, c2X + colW - 15, w5Y + 145, 0xFF1A1B1D); // Create Pool Box
        g.drawString(this.font, "Create Pool", c2X + (colW/2) - 30, w5Y + 120, 0x888888, false);


        // ================= COLUMN 3 =================
        int c3X = c2X + colW + panelGap;

        // --- Widget: CPU Dial & Model ---
        int w6Y = y + 42;
        int w6H = 100;
        int halfW = (colW - panelGap) / 2;

        drawPanelBg(g, c3X, w6Y, halfW, w6H);
        g.drawString(this.font, "CPU Usage", c3X + 10, w6Y + 10, 0xFFFFFF, false);
        renderMemoryDonutChart(g, c3X + (halfW/2), w6Y + 55, 25, 20); // Reuse donut for dial
        g.drawString(this.font, "1%", c3X + (halfW/2) - 5, w6Y + 51, 0xFFFFFF, false);

        drawPanelBg(g, c3X + halfW + panelGap, w6Y, halfW, w6H);
        g.drawString(this.font, "CPU Model", c3X + halfW + panelGap + 10, w6Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "Intel(R) Atom(TM)", c3X + halfW + panelGap + 10, w6Y + 45, 0xAAAAAA, false);
        g.drawString(this.font, "C3758 @ 2.20GHz", c3X + halfW + panelGap + 10, w6Y + 58, 0xAAAAAA, false);

        // --- Widget: CPU Recent Usage (Line Graph) ---
        int w7Y = w6Y + w6H + panelGap;
        int w7H = 100;
        drawPanelBg(g, c3X, w7Y, colW, w7H);
        g.drawString(this.font, "CPU Recent Usage", c3X + 15, w7Y + 15, 0xFFFFFF, false);
        // Quick line graph mock
        g.fill(c3X + 30, w7Y + 80, c3X + colW - 15, w7Y + 81, 0xFF444444);
        for(int i = 0; i < 20; i++) {
            int lx = c3X + 30 + (i * 10);
            int ly = w7Y + 75 - (int)(Math.random() * 20);
            g.fill(lx, ly, lx+2, ly+2, 0xFF0092C8);
        }

        // --- Widget: Backup Tasks ---
        int w8Y = w7Y + w7H + panelGap;
        int w8H = 100;
        drawPanelBg(g, c3X, w8Y, colW, w8H);
        g.drawString(this.font, "Backup Tasks", c3X + 15, w8Y + 15, 0xFFFFFF, false);
        g.drawString(this.font, "1 of 1 task failed", c3X + 90, w8Y + 15, 0xFFE6A23C, false);

        g.fill(c3X + 15, w8Y + 35, c3X + colW - 15, w8Y + 85, 0xFF1A1B1D);
        g.drawString(this.font, "Rsync", c3X + 30, w8Y + 55, 0xAAAAAA, false);
        g.drawString(this.font, "1 send task", c3X + 100, w8Y + 45, 0xFFE6A23C, false);
        g.drawString(this.font, "0 receive tasks", c3X + 100, w8Y + 58, 0xFF22C55E, false);
        g.drawString(this.font, "Total failed: 1", c3X + 100, w8Y + 71, 0xFFE6A23C, false);
    }

    private void drawPanelBg(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF252526);
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
        g.fill(x, y + h - 1, x + w, y + h, 0xFF444444); // X Axis
        g.fill(x, y, x + 1, y + h, 0xFF444444);         // Y Axis

        int numBars = values.length;
        int gap = 12;
        int barWidth = (w - (numBars + 1) * gap) / numBars;

        for (int i = 0; i < numBars; i++) {
            int barX = x + gap + i * (barWidth + gap);
            int barH = (int) (values[i] * (h - 15));
            int barY = y + h - 1 - barH;
            g.fill(barX, barY, barX + barWidth, y + h - 1, color);
            g.drawString(this.font, String.valueOf(i + 1), barX + (barWidth / 2) - 3, y + h + 2, 0x888888, false);
        }
    }

    private void renderRecessedSlotBox(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 19, y + 19, 0xFF121212);
        g.fill(x, y, x + 19, y + 19, 0xFF4A4D52);
        g.fill(x, y, x + 18, y + 18, 0xFF2B2C2E);
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