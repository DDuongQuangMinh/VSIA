package com.k1ngtle.vsia.client.web;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class W129IdeEditBox extends EditBox {
    public W129IdeEditBox(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Component message
    ) {
        super(
                font,
                x,
                y,
                width,
                height,
                message
        );

        setBordered(false);
    }

    @Override
    public void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        int border =
                isFocused()
                        ? W129IdeTheme.ACCENT
                        : W129IdeTheme.BORDER;

        graphics.fill(
                getX(),
                getY(),
                getX() + width,
                getY() + height,
                border
        );

        graphics.fill(
                getX() + 1,
                getY() + 1,
                getX() + width - 1,
                getY() + height - 1,
                0xFF07101A
        );

        super.renderWidget(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }
}
