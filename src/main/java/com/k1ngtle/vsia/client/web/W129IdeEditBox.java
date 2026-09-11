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

        int originalY =
                getY();

        graphics.fill(
                getX(),
                originalY,
                getX() + width,
                originalY + height,
                border
        );

        graphics.fill(
                getX() + 1,
                originalY + 1,
                getX() + width - 1,
                originalY + height - 1,
                0xFF07101A
        );

        int textOffset =
                Math.max(
                        0,
                        (height - 8) / 2
                );

        setY(
                originalY
                        + textOffset
        );

        try {
            super.renderWidget(
                    graphics,
                    mouseX,
                    mouseY,
                    partialTick
            );
        } finally {
            setY(originalY);
        }
    }
}
