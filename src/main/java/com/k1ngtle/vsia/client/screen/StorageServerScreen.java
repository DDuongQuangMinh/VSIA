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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class StorageServerScreen extends AbstractContainerScreen<StorageServerMenu> {

    private boolean isDashboard = true;
    private float scrollOffset = 0.0f;
    private float fileScrollOffset = 0.0f;

    // Total capacity based on 360 slots * 600,000 limit
    private static final long MAX_ITEM_CAPACITY = 360L * 600_000L;

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
        ITEMS, FILES, NETWORK
    }

    private TerminalSize currentSize = TerminalSize.NORMAL;
    private Tab currentTab = Tab.ITEMS;
    private StoredFile openFile = null;
    private String fileSearchTerm = "";

    private Button modeToggleButton;
    private Button sizeToggleButton;
    private Button checkUpdatesButton;
    private EditBox searchBox;

    private Button uploadButton;
    private Button deleteButton;

    private EditBox ipBox;
    private EditBox ipv6Box;
    private EditBox subnetBox;
    private EditBox gatewayBox;
    private Button dhcpButton;

    public StorageServerScreen(StorageServerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 840;
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
        ).bounds(this.leftPos + 22, this.topPos + 172, 115, 20).build();
        this.checkUpdatesButton.visible = isDashboard;
        this.addRenderableWidget(this.checkUpdatesButton);

        this.searchBox = new EditBox(this.font, this.leftPos + 115, this.topPos + 6, 85, 12, Component.literal("Search"));
        this.searchBox.setMaxLength(30);
        this.searchBox.setBordered(false);
        this.searchBox.setVisible(!isDashboard);
        this.searchBox.setResponder(text -> {
            this.menu.updateSearch(text);
            this.fileSearchTerm = text.toLowerCase();
            this.fileScrollOffset = 0.0f;
            this.scrollOffset = 0.0f;
        });
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

        this.ipBox = new EditBox(this.font, this.leftPos + 90, this.topPos + 34, 85, 12, Component.literal("IPv4 Address"));
        this.ipBox.setMaxLength(15);
        this.ipBox.setVisible(false);
        this.addRenderableWidget(this.ipBox);

        this.ipv6Box = new EditBox(this.font, this.leftPos + 90, this.topPos + 50, 85, 12, Component.literal("IPv6 Address"));
        this.ipv6Box.setMaxLength(40);
        this.ipv6Box.setVisible(false);
        this.addRenderableWidget(this.ipv6Box);

        this.subnetBox = new EditBox(this.font, this.leftPos + 90, this.topPos + 66, 85, 12, Component.literal("Subnet Mask"));
        this.subnetBox.setMaxLength(15);
        this.subnetBox.setVisible(false);
        this.addRenderableWidget(this.subnetBox);

        this.gatewayBox = new EditBox(this.font, this.leftPos + 90, this.topPos + 82, 85, 12, Component.literal("Gateway"));
        this.gatewayBox.setMaxLength(15);
        this.gatewayBox.setVisible(false);
        this.addRenderableWidget(this.gatewayBox);

        this.dhcpButton = Button.builder(
                Component.literal("DHCP: ON"),
                b -> {
                    if (this.menu.blockEntity != null) {
                        boolean current = this.menu.blockEntity.isDhcpEnabled();
                        this.menu.blockEntity.setDhcpEnabled(!current);
                        b.setMessage(Component.literal("DHCP: " + (!current ? "ON" : "OFF")));
                    }
                }
        ).bounds(this.leftPos + 90, this.topPos + 98, 85, 18).build();
        this.dhcpButton.visible = false;
        this.addRenderableWidget(this.dhcpButton);

        if (this.menu.blockEntity != null) {
            this.ipBox.setValue(this.menu.blockEntity.getIpAddress());
            this.ipv6Box.setValue(this.menu.blockEntity.getIpv6Address());
            this.subnetBox.setValue(this.menu.blockEntity.getSubnetMask());
            this.gatewayBox.setValue(this.menu.blockEntity.getGateway());
            this.dhcpButton.setMessage(Component.literal("DHCP: " + (this.menu.blockEntity.isDhcpEnabled() ? "ON" : "OFF")));
        }

        repositionWidgets();
    }

    private void updateDimensions() {
        if (isDashboard) {
            this.imageWidth = 840;
            this.imageHeight = 460;
            this.leftPos = (this.width - this.imageWidth) / 2;
            // Snug fit towards the top, removing the old +20 offset
            this.topPos = Math.max(30, (this.height - this.imageHeight) / 2);
        } else {
            this.imageWidth = currentSize.width;
            this.imageHeight = currentSize.height;
            this.leftPos = (this.width - this.imageWidth) / 2;
            this.topPos = Math.max(22, (this.height - this.imageHeight) / 2);
        }
    }

    private void updateVisibility() {
        this.menu.slotsVisible = (!this.isDashboard && this.currentTab == Tab.ITEMS && this.openFile == null);
        if (this.uploadButton != null) this.uploadButton.visible = (!this.isDashboard && this.currentTab == Tab.FILES && this.openFile == null);
        if (this.deleteButton != null) this.deleteButton.visible = (!this.isDashboard && this.currentTab == Tab.FILES && this.openFile == null);
        if (this.searchBox != null) this.searchBox.setVisible(!this.isDashboard && this.currentTab != Tab.NETWORK);

        boolean showNetwork = (!this.isDashboard && this.currentTab == Tab.NETWORK);
        if (this.ipBox != null) this.ipBox.setVisible(showNetwork);
        if (this.ipv6Box != null) this.ipv6Box.setVisible(showNetwork);
        if (this.subnetBox != null) this.subnetBox.setVisible(showNetwork);
        if (this.gatewayBox != null) this.gatewayBox.setVisible(showNetwork);
        if (this.dhcpButton != null) this.dhcpButton.visible = showNetwork;
    }

    private void toggleMode() {
        this.isDashboard = !this.isDashboard;
        this.openFile = null;
        this.modeToggleButton.setMessage(Component.literal(isDashboard ? "Configure" : "Dashboard"));
        this.sizeToggleButton.visible = !isDashboard;
        this.checkUpdatesButton.visible = isDashboard;

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

        // Exact position for the Check for Updates button in dashboard mode
        if (this.checkUpdatesButton != null) {
            int col1X = this.leftPos + 12;
            int p1Y = this.topPos + 12;
            this.checkUpdatesButton.setPosition(col1X + 10, p1Y + 174);
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

        if (this.ipBox != null) this.ipBox.setPosition(this.leftPos + 90, this.topPos + 34);
        if (this.ipv6Box != null) this.ipv6Box.setPosition(this.leftPos + 90, this.topPos + 50);
        if (this.subnetBox != null) this.subnetBox.setPosition(this.leftPos + 90, this.topPos + 66);
        if (this.gatewayBox != null) this.gatewayBox.setPosition(this.leftPos + 90, this.topPos + 82);
        if (this.dhcpButton != null) this.dhcpButton.setPosition(this.leftPos + 90, this.topPos + 98);
    }

    private long getTotalStoredItems() {
        long total = 0;
        if (this.menu.blockEntity != null) {
            IItemHandler handler = this.menu.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
            if (handler != null) {
                for (int i = 0; i < handler.getSlots(); i++) {
                    total += StorageServerBlockEntity.getRealCount(handler.getStackInSlot(i));
                }
                return total;
            }
        }
        for (int i = 0; i < 54 && i < this.menu.slots.size(); i++) {
            total += StorageServerBlockEntity.getRealCount(this.menu.slots.get(i).getItem());
        }
        return total;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);

        if (isDashboard) {
            renderDashboardMode(g);
        } else {
            renderTerminalMode(g, mouseX, mouseY);
        }

        // Hide vanilla item counts for our custom massive formatting
        int[] realCounts = new int[this.menu.slots.size()];
        if (!isDashboard && currentTab == Tab.ITEMS && openFile == null) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem()) {
                    realCounts[i] = StorageServerBlockEntity.getRealCount(slot.getItem());
                    slot.getItem().setCount(1);
                }
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        // Restore custom counts and draw them uniformly over ALL items (vanilla included)
        if (!isDashboard && currentTab == Tab.ITEMS && openFile == null) {
            for (int i = 0; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.hasItem()) {
                    slot.getItem().setCount(realCounts[i] > 64 ? 64 : realCounts[i]);

                    if (realCounts[i] > 1) {
                        String formatted = realCounts[i] >= 1000 ? formatLargeNumber(realCounts[i]) : String.valueOf(realCounts[i]);
                        int slotX = this.leftPos + slot.x;
                        int slotY = this.topPos + slot.y;

                        g.pose().pushPose();
                        // Translate absolutely to bottom-right of the slot with a high Z value (250)
                        g.pose().translate(slotX + 16, slotY + 16, 250);

                        // Scale the text down so large numbers fit perfectly inside the slot
                        float textScale = 0.55f;
                        g.pose().scale(textScale, textScale, 1.0f);

                        int textWidth = this.font.width(formatted);
                        int textHeight = this.font.lineHeight;

                        // Draw a dark mask behind our custom text to cleanly obscure any vanilla text that might leak
                        g.fill(-textWidth - 1, -textHeight, 1, 1, 0xDD111111);

                        // Draw our scaled, formatted text directly on top
                        g.drawString(this.font, formatted, -textWidth, -textHeight + 1, 0xFFFFFF, true);
                        g.pose().popPose();
                    }
                }
            }
        }

        if (!isDashboard) {
            if (this.currentTab != Tab.NETWORK) {
                g.fill(this.leftPos + 105, this.topPos + 4, this.leftPos + 210, this.topPos + 20, 0xFF141414);
                g.fill(this.leftPos + 106, this.topPos + 5, this.leftPos + 209, this.topPos + 19, 0xFF000000);
                this.searchBox.render(g, mouseX, mouseY, partialTick);
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
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) { }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (!this.isDashboard) {
            if (pKeyCode == 256) { // Escape
                if (this.searchBox != null) this.searchBox.setFocused(false);
                if (this.ipBox != null) this.ipBox.setFocused(false);
                if (this.ipv6Box != null) this.ipv6Box.setFocused(false);
                if (this.subnetBox != null) this.subnetBox.setFocused(false);
                if (this.gatewayBox != null) this.gatewayBox.setFocused(false);
                return true;
            }
            if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
            if (this.ipBox != null && this.ipBox.isFocused() && this.ipBox.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
            if (this.ipv6Box != null && this.ipv6Box.isFocused() && this.ipv6Box.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
            if (this.subnetBox != null && this.subnetBox.isFocused() && this.subnetBox.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
            if (this.gatewayBox != null && this.gatewayBox.isFocused() && this.gatewayBox.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (!this.isDashboard) {
            if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.ipBox != null && this.ipBox.isFocused() && this.ipBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.ipv6Box != null && this.ipv6Box.isFocused() && this.ipv6Box.charTyped(pCodePoint, pModifiers)) return true;
            if (this.subnetBox != null && this.subnetBox.isFocused() && this.subnetBox.charTyped(pCodePoint, pModifiers)) return true;
            if (this.gatewayBox != null && this.gatewayBox.isFocused() && this.gatewayBox.charTyped(pCodePoint, pModifiers)) return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isDashboard) {
            if (this.searchBox != null && this.searchBox.mouseClicked(mouseX, mouseY, button)) {
                this.searchBox.setFocused(true); return true;
            } else if (this.searchBox != null) { this.searchBox.setFocused(false); }

            if (this.currentTab == Tab.NETWORK) {
                if (this.ipBox != null && this.ipBox.mouseClicked(mouseX, mouseY, button)) { this.ipBox.setFocused(true); return true; } else if (this.ipBox != null) { this.ipBox.setFocused(false); }
                if (this.ipv6Box != null && this.ipv6Box.mouseClicked(mouseX, mouseY, button)) { this.ipv6Box.setFocused(true); return true; } else if (this.ipv6Box != null) { this.ipv6Box.setFocused(false); }
                if (this.subnetBox != null && this.subnetBox.mouseClicked(mouseX, mouseY, button)) { this.subnetBox.setFocused(true); return true; } else if (this.subnetBox != null) { this.subnetBox.setFocused(false); }
                if (this.gatewayBox != null && this.gatewayBox.mouseClicked(mouseX, mouseY, button)) { this.gatewayBox.setFocused(true); return true; } else if (this.gatewayBox != null) { this.gatewayBox.setFocused(false); }
            }

            int x = this.leftPos;
            int y = this.topPos;

            // Side tabs
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
            if (mouseX >= x - 28 && mouseX <= x && mouseY >= y + 66 && mouseY <= y + 90) {
                this.currentTab = Tab.NETWORK;
                this.openFile = null;
                updateVisibility();
                return true;
            }

            if (this.currentTab == Tab.FILES) {
                if (openFile != null) {
                    // Close Modal
                    int modalX = x + 10;
                    int modalY = y + 15;
                    int modalW = Math.max(250, this.imageWidth - 200);
                    if (mouseX >= modalX + modalW - 16 && mouseX <= modalX + modalW - 4 && mouseY >= modalY + 4 && mouseY <= modalY + 16) {
                        this.openFile = null;
                        updateVisibility();
                        return true;
                    }
                } else {
                    // Open File
                    List<StoredFile> files = this.menu.getFiles().stream()
                            .filter(f -> this.fileSearchTerm.isEmpty() || f.getName().toLowerCase().contains(this.fileSearchTerm) || f.getLanguage().toLowerCase().contains(this.fileSearchTerm))
                            .toList();
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

    private void renderDashboardMode(GuiGraphics g) {
        int x = this.leftPos;
        int y = this.topPos;

        // Overall background
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1B1D);
        g.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF252526);

        int col1X = x + 12;
        int col2X = x + 288;
        int col3X = x + 564;
        int panelWidth = 264;

        // --- COL 1: System Info & Memory ---
        int p1Y = y + 12;
        int p1H = 198; // Adjusted height for Check Updates button

        g.fill(col1X, p1Y, col1X + panelWidth, p1Y + p1H, 0xFF2A2B2D);
        g.fill(col1X + 1, p1Y + 1, col1X + panelWidth - 1, p1Y + p1H - 1, 0xFF1E1E1E);

        g.drawString(this.font, "TrueNAS SCALE", col1X + 10, p1Y + 10, 0xFFFFFF, false);
        g.fill(col1X + 10, p1Y + 24, col1X + panelWidth - 10, p1Y + 44, 0xFF111111);
        g.fill(col1X + 15, p1Y + 32, col1X + 20, p1Y + 37, 0xFF22C55E);
        g.fill(col1X + 35, p1Y + 32, col1X + 40, p1Y + 37, 0xFF22C55E);
        g.fill(col1X + 55, p1Y + 32, col1X + 60, p1Y + 37, 0xFF0092C8);
        g.fill(col1X + 75, p1Y + 32, col1X + 80, p1Y + 37, 0xFF22C55E);

        g.drawString(this.font, "System Information", col1X + 10, p1Y + 56, 0xFF0092C8, false);
        g.fill(col1X + 10, p1Y + 68, col1X + panelWidth - 10, p1Y + 69, 0xFF333333);

        g.drawString(this.font, "Overview", col1X + 10, p1Y + 76, 0xAAAAAA, false);
        g.drawString(this.font, "Platform: TRUENAS-MINI-R", col1X + 10, p1Y + 92, 0xAAAAAA, false);
        g.drawString(this.font, "Version: ElectricEel-24.10.0-MASTER-2...", col1X + 10, p1Y + 108, 0xAAAAAA, false);
        g.drawString(this.font, "Hostname: re-minir-102", col1X + 10, p1Y + 124, 0xAAAAAA, false);

        String displayIp = this.menu.blockEntity != null ? this.menu.blockEntity.getIpAddress() : "192.168.1.100";
        g.drawString(this.font, "IP: " + displayIp, col1X + 10, p1Y + 140, 0xAAAAAA, false);
        g.drawString(this.font, "Uptime: 1h 28m", col1X + 10, p1Y + 156, 0x888888, false);
        // Note: The Check for Updates button is automatically drawn here by repositionWidgets()

        int p2Y = y + 212;
        int p2H = 120;
        g.fill(col1X, p2Y, col1X + panelWidth, p2Y + p2H, 0xFF2A2B2D);
        g.fill(col1X + 1, p2Y + 1, col1X + panelWidth - 1, p2Y + p2H - 1, 0xFF1E1E1E);

        g.drawString(this.font, "Memory", col1X + 10, p2Y + 10, 0xFFFFFF, false);
        g.pose().pushPose();
        g.pose().scale(1.5f, 1.5f, 1.0f);
        g.drawString(this.font, "31.3", (int)((col1X + 10) / 1.5f), (int)((p2Y + 40) / 1.5f), 0xFFFFFF, true);
        g.pose().popPose();
        g.drawString(this.font, "GiB", col1X + 50, p2Y + 45, 0xAAAAAA, false);
        g.drawString(this.font, "total available (ECC)", col1X + 10, p2Y + 60, 0x777777, false);

        g.fill(col1X + 10, p2Y + 80, col1X + 16, p2Y + 86, 0xFF0092C8);
        g.drawString(this.font, "Free: 27.0 GiB", col1X + 22, p2Y + 78, 0xAAAAAA, false);
        g.fill(col1X + 10, p2Y + 95, col1X + 16, p2Y + 101, 0xFFB82DB8);
        g.drawString(this.font, "ZFS Cache: 1.1 GiB", col1X + 22, p2Y + 93, 0xAAAAAA, false);
        g.fill(col1X + 10, p2Y + 110, col1X + 16, p2Y + 116, 0xFFE6A23C);
        g.drawString(this.font, "Services: 3.2 GiB", col1X + 22, p2Y + 108, 0xAAAAAA, false);

        // Animated Memory Donut Chart
        renderAnimatedDonutChart(g, col1X + 190, p2Y + 60, 36, 20, System.currentTimeMillis() / 25.0);

        // --- COL 2: CPU Graphs ---
        int p3Y = y + 12;
        int p3H = 213;
        g.fill(col2X, p3Y, col2X + panelWidth, p3Y + p3H, 0xFF2A2B2D);
        g.fill(col2X + 1, p3Y + 1, col2X + panelWidth - 1, p3Y + p3H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Usage Per Core", col2X + 10, p3Y + 10, 0xFFFFFF, false);
        renderBarGraph(g, col2X + 20, p3Y + 40, 224, 150, 0xFF0092C8, new float[]{0.22f, 0.48f, 0.16f, 0.75f, 0.40f, 0.68f, 0.14f, 0.48f}, true);

        int p4Y = y + 235;
        int p4H = 213;
        g.fill(col2X, p4Y, col2X + panelWidth, p4Y + p4H, 0xFF2A2B2D);
        g.fill(col2X + 1, p4Y + 1, col2X + panelWidth - 1, p4Y + p4H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Temperature Per Core", col2X + 10, p4Y + 10, 0xFFFFFF, false);
        renderBarGraph(g, col2X + 20, p4Y + 40, 224, 150, 0xFFB82DB8, new float[]{0.65f, 0.68f, 0.64f, 0.70f, 0.66f, 0.67f, 0.63f, 0.65f}, true);

        // --- COL 3: CPU Metrics, Backup Tasks & Storage ---
        int p5aY = y + 12;
        int p5H = 90;
        g.fill(col3X, p5aY, col3X + 127, p5aY + p5H, 0xFF2A2B2D);
        g.fill(col3X + 1, p5aY + 1, col3X + 126, p5aY + p5H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Usage", col3X + 10, p5aY + 8, 0xFFFFFF, false);
        renderAnimatedDonutChart(g, col3X + 63, p5aY + 55, 20, 12, System.currentTimeMillis() / 15.0);
        g.drawString(this.font, "1%", col3X + 57, p5aY + 51, 0xFFFFFF, false);

        int p5bX = col3X + 137;
        g.fill(p5bX, p5aY, p5bX + 127, p5aY + p5H, 0xFF2A2B2D);
        g.fill(p5bX + 1, p5aY + 1, p5bX + 126, p5aY + p5H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Model", p5bX + 10, p5aY + 8, 0xFFFFFF, false);
        g.drawString(this.font, "Intel(R) Atom(TM)", p5bX + 10, p5aY + 40, 0xCCCCCC, false);
        g.drawString(this.font, "C3758 @ 2.20GHz", p5bX + 10, p5aY + 54, 0x888888, false);

        int p6Y = y + 112;
        int p6H = 90;
        g.fill(col3X, p6Y, col3X + panelWidth, p6Y + p6H, 0xFF2A2B2D);
        g.fill(col3X + 1, p6Y + 1, col3X + panelWidth - 1, p6Y + p6H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "CPU Recent Usage", col3X + 10, p6Y + 8, 0xFFFFFF, false);
        g.fill(col3X + 20, p6Y + 70, col3X + 244, p6Y + 71, 0xFF444444);
        for(int i=0; i<15; i++) {
            g.fill(col3X + 20 + (i*15), p6Y + 60 + (int)(Math.sin(System.currentTimeMillis()/300.0 + i)*12), col3X + 22 + (i*15), p6Y + 62 + (int)(Math.sin(System.currentTimeMillis()/300.0 + i)*12), 0xFF0092C8);
        }

        int p7Y = y + 212;
        int p7H = 113;
        g.fill(col3X, p7Y, col3X + panelWidth, p7Y + p7H, 0xFF2A2B2D);
        g.fill(col3X + 1, p7Y + 1, col3X + panelWidth - 1, p7Y + p7H - 1, 0xFF1E1E1E);
        g.drawString(this.font, "Backup Tasks", col3X + 10, p7Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "1 of 1 task failed", col3X + 90, p7Y + 10, 0xFFAA00, false);
        g.drawString(this.font, "Rsync", col3X + 20, p7Y + 46, 0xAAAAAA, false);
        g.drawString(this.font, "1 send task", col3X + 90, p7Y + 36, 0xFFAA00, false);
        g.drawString(this.font, "0 receive tasks", col3X + 90, p7Y + 48, 0x55FF55, false);
        g.drawString(this.font, "Total failed: 1", col3X + 90, p7Y + 60, 0xFFAA00, false);

        int p8Y = y + 335;
        int p8H = 113;
        g.fill(col3X, p8Y, col3X + panelWidth, p8Y + p8H, 0xFF2A2B2D);
        g.fill(col3X + 1, p8Y + 1, col3X + panelWidth - 1, p8Y + p8H - 1, 0xFF1E1E1E);

        long totalItems = getTotalStoredItems();
        float percent = Math.min(1.0f, (float) totalItems / MAX_ITEM_CAPACITY);
        double freeTiB = 9.0 - (9.0 * percent);

        g.drawString(this.font, "Storage", col3X + 10, p8Y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "tank", col3X + 10, p8Y + 28, 0xFFFFFF, false);
        g.drawString(this.font, "Status: ONLINE", col3X + 10, p8Y + 46, 0x55FF55, false);
        g.drawString(this.font, String.format(java.util.Locale.ROOT, "Used Space: %.1f%%", percent * 100), col3X + 10, p8Y + 60, 0x55FF55, false);
        g.drawString(this.font, "Disks Error: 0", col3X + 10, p8Y + 74, 0x55FF55, false);
        g.drawString(this.font, String.format(java.util.Locale.ROOT, "Free Space: %.2f TiB", freeTiB), col3X + 120, p8Y + 46, 0xAAAAAA, false);
        g.drawString(this.font, "Total Disks: 2", col3X + 120, p8Y + 60, 0xAAAAAA, false);

        g.fill(col3X + 10, p8Y + 90, col3X + panelWidth - 10, p8Y + 105, 0xFF222222);
        g.drawString(this.font, "Create Pool", col3X + 105, p8Y + 94, 0x888888, false);
    }

    private void renderAnimatedDonutChart(GuiGraphics g, int centerX, int centerY, int outerR, int innerR, double timeOffset) {
        int freeColor = 0xFF0092C8;
        int zfsColor = 0xFFB82DB8;
        int serviceColor = 0xFFE6A23C;

        for (int r = innerR; r <= outerR; r++) {
            for (int angle = 0; angle < 360; angle += 4) {
                double animatedAngle = (angle + timeOffset) % 360;
                double rad = Math.toRadians(animatedAngle);
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

        g.fill(x - 28, y + 66, x, y + 90, currentTab == Tab.NETWORK ? 0xFF2A2B2D : 0xFF141414);
        g.fill(x - 27, y + 67, x, y + 89, currentTab == Tab.NETWORK ? 0xFF212224 : 0xFF1E1E1E);
        g.drawString(this.font, "N", x - 18, y + 74, currentTab == Tab.NETWORK ? 0xFFFFFF : 0x888888, false);

        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF2A2B2D);
        g.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF212224);

        g.drawString(this.font, "Storage Terminal", x + 8, y + 8, 0xFFFFFF, false);

        int trackX = x + 171;
        int trackY = y + 21;

        if (currentTab == Tab.ITEMS) {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    renderRecessedSlotBox(g, x + 7 + col * 18, y + 20 + row * 18);
                }
            }

            g.fill(trackX, trackY, trackX + 14, trackY + 108, 0xFF141414);
            int thumbY = trackY + 1 + (int) (this.scrollOffset * 91.0f);
            g.fill(trackX + 1, thumbY, trackX + 13, thumbY + 15, 0xFF0092C8);
            g.fill(trackX + 2, thumbY + 1, trackX + 12, thumbY + 14, 0xFF007BA8);

        } else if (currentTab == Tab.FILES) {
            g.fill(x + 7, y + 20, trackX - 2, y + 108, 0xFF141414);

            List<StoredFile> files = this.menu.getFiles().stream()
                    .filter(f -> this.fileSearchTerm.isEmpty() || f.getName().toLowerCase().contains(this.fileSearchTerm) || f.getLanguage().toLowerCase().contains(this.fileSearchTerm))
                    .toList();
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
        } else if (currentTab == Tab.NETWORK) {
            g.drawString(this.font, "Network Configuration", x + 10, y + 22, 0x0092C8, false);
            g.drawString(this.font, "IPv4 Address", x + 10, y + 36, 0xAAAAAA, false);
            g.drawString(this.font, "IPv6 Address", x + 10, y + 52, 0xAAAAAA, false);
            g.drawString(this.font, "Subnet Mask", x + 10, y + 68, 0xAAAAAA, false);
            g.drawString(this.font, "Default Gateway", x + 10, y + 84, 0xAAAAAA, false);
            g.drawString(this.font, "DHCP Mode", x + 10, y + 103, 0xAAAAAA, false);

            if (this.menu.blockEntity != null) {
                // Instantly sync visual text entry fields with block entity on the client
                this.menu.blockEntity.setIpAddress(this.ipBox.getValue());
                this.menu.blockEntity.setIpv6Address(this.ipv6Box.getValue());
                this.menu.blockEntity.setSubnetMask(this.subnetBox.getValue());
                this.menu.blockEntity.setGateway(this.gatewayBox.getValue());
            }
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

            // Shifted Y coordinates down by 15px to avoid overlapping with Network inputs
            int statsY = y + 44;

            g.drawString(this.font, "Server Status", sideX + 8, statsY, 0xFFFFFF, false);
            g.drawString(this.font, "Pool: tank", sideX + 8, statsY + 16, 0xCCCCCC, false);
            g.drawString(this.font, "Status: ONLINE", sideX + 8, statsY + 30, 0x55FF55, false);

            int barWidth = Math.min(sideW - 16, 120);

            if (currentTab == Tab.ITEMS) {
                long totalItems = getTotalStoredItems();
                float percent = Math.min(1.0f, (float) totalItems / MAX_ITEM_CAPACITY);

                g.drawString(this.font, "Capacity (9 TiB)", sideX + 8, statsY + 50, 0xAAAAAA, false);
                g.fill(sideX + 8, statsY + 62, sideX + 8 + barWidth, statsY + 70, 0xFF333333);
                g.fill(sideX + 8, statsY + 62, sideX + 8 + (int) (barWidth * percent), statsY + 70, 0xFF0092C8);
                g.drawString(this.font, String.format(java.util.Locale.ROOT, "%.1f%% Used", percent * 100), sideX + 8, statsY + 74, 0x888888, false);
            } else {
                g.drawString(this.font, "File Capacity (10 MB)", sideX + 8, statsY + 50, 0xAAAAAA, false);
                g.fill(sideX + 8, statsY + 62, sideX + 8 + barWidth, statsY + 70, 0xFF333333);

                int totalBytes = this.menu.getFiles().stream().mapToInt(StoredFile::getSizeInBytes).sum();
                float percent = Math.min(1.0f, totalBytes / (float) (10 * 1024 * 1024));
                g.fill(sideX + 8, statsY + 62, sideX + 8 + (int) (barWidth * percent), statsY + 70, 0xFFB82DB8);

                g.drawString(this.font, String.format(java.util.Locale.ROOT, "%.1f%% Used", percent * 100), sideX + 8, statsY + 74, 0x888888, false);
            }

            g.drawString(this.font, "Drives: 8 Active", sideX + 8, statsY + 100, 0xCCCCCC, false);
            g.drawString(this.font, "ZFS Cache: 1.1GB", sideX + 8, statsY + 114, 0xFFB82DB8, false);
            g.drawString(this.font, "Network: 10Gbps", sideX + 8, statsY + 128, 0xFFE6A23C, false);

            for (int i = 0; i < 8; i++) {
                int ledX = sideX + 8 + (i * 12);
                int ledY = statsY + 146;
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
        // Open file dialog using LWJGL's native cross-platform TinyFileDialogs
        String filePath = TinyFileDialogs.tinyfd_openFileDialog(
                "Select File to Upload",
                System.getProperty("user.home") + "/",
                null,
                null,
                false
        );

        if (filePath != null) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

                    // Enforce 10 MB file size limit
                    int maxFileSizeBytes = 10 * 1024 * 1024;
                    if (content.length() > maxFileSizeBytes) {
                        content = content.substring(0, maxFileSizeBytes);
                        System.out.println("Warning: File exceeds 10MB limit and was truncated.");
                    }

                    String fileName = file.getName();
                    String ext = "txt"; // Default extension
                    int i = fileName.lastIndexOf('.');
                    if (i > 0 && i < fileName.length() - 1) {
                        ext = fileName.substring(i + 1);
                    }

                    // Add to client-side UI
                    this.menu.addFileClient(fileName, ext, content);
                    updateVisibility(); // Refresh the UI

                    // SEND PACKET TO SERVER TO PERSIST DATA
                    if (this.menu.blockEntity != null) {
                        com.k1ngtle.vsia.network.VsiaNetwork.sendToServer(new com.k1ngtle.vsia.network.UploadFilePacket(
                                this.menu.blockEntity.getBlockPos(), fileName, ext, content
                        ));
                    }

                } catch (Exception e) {
                    System.err.println("Failed to read upload file: " + e.getMessage());
                }
            }
        }
    }

    private void handleDelete() {
        StoredFile fileToDelete = this.openFile;

        if (fileToDelete == null) {
            List<StoredFile> files = this.menu.getFiles();
            if (!files.isEmpty()) {
                fileToDelete = files.get(files.size() - 1);
            }
        }

        if (fileToDelete != null) {
            // Remove from Client UI
            this.menu.removeFileClient(fileToDelete);

            // Send Delete Packet to Server to persist deletion
            if (this.menu.blockEntity != null) {
                com.k1ngtle.vsia.network.VsiaNetwork.sendToServer(new com.k1ngtle.vsia.network.DeleteFilePacket(
                        this.menu.blockEntity.getBlockPos(), fileToDelete.getName()
                ));
            }

            this.openFile = null;
            updateVisibility();
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
        int realCount = StorageServerBlockEntity.getRealCount(stack);
        if (realCount >= LARGE_COUNT_THRESHOLD) {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add(Component.literal(String.format(java.util.Locale.ROOT, "%,d", realCount))
                    .withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }
}