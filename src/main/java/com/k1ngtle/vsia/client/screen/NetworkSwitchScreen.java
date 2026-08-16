package com.k1ngtle.vsia.client.screen;

import com.k1ngtle.vsia.signality.internet.server.NetworkSwitchBlockEntity;
import com.k1ngtle.vsia.world.inventory.NetworkSwitchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class NetworkSwitchScreen extends AbstractContainerScreen<NetworkSwitchMenu> {

    public NetworkSwitchScreen(NetworkSwitchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 160;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10000;
        this.inventoryLabelY = 10000;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);

        int x = this.leftPos;
        int y = this.topPos;

        g.drawString(this.font, "Network Switch: " + this.menu.blockEntity.getSwitchName(), x + 10, y + 10, 0xFFFFFF, false);
        g.drawString(this.font, "Ports (Max 24):", x + 10, y + 25, 0xAAAAAA, false);

        List<BlockPos> connections = this.menu.blockEntity.getConnectedDevices();

        int portX = x + 15;
        int portY = y + 40;

        for (int i = 0; i < NetworkSwitchBlockEntity.MAX_PORTS; i++) {
            // Draw Port Background
            g.fill(portX, portY, portX + 16, portY + 16, 0xFF222222);
            g.fill(portX + 1, portY + 1, portX + 15, portY + 15, 0xFF111111);

            if (i < connections.size()) {
                // Active Port indicator (Green)
                g.fill(portX + 1, portY + 1, portX + 15, portY + 15, 0xFF22C55E);

                // Hover tooltip
                if (mouseX >= portX && mouseX < portX + 16 && mouseY >= portY && mouseY < portY + 16) {
                    BlockPos p = connections.get(i);
                    g.renderTooltip(this.font, Component.literal("Connected: " + p.getX() + ", " + p.getY() + ", " + p.getZ()), mouseX, mouseY);
                }
            } else {
                // Inactive port (Dark Grey)
                g.fill(portX + 6, portY + 12, portX + 10, portY + 14, 0xFF555555);
            }

            portX += 20;
            if ((i + 1) % 12 == 0) {
                portX = x + 15;
                portY += 24;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1B1D);
        g.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF252526);
    }
}