package com.k1ngtle.vsia.client.web;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class W129IdeButton extends Button {
    private W129IdeButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress
    ) {
        super(
                x,
                y,
                width,
                height,
                message,
                onPress,
                Button.DEFAULT_NARRATION
        );
    }

    public static Builder themedBuilder(
            Component message,
            OnPress onPress
    ) {
        return new Builder(
                message,
                onPress
        );
    }

    @Override
    public void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        boolean selected =
                !active;

        int background =
                selected
                        ? W129IdeTheme.SELECTION
                        : (
                        isHoveredOrFocused()
                                ? W129IdeTheme.HOVER
                                : W129IdeTheme.HEADER_ALT
                );

        int border =
                selected
                        ? W129IdeTheme.ACCENT
                        : (
                        isHoveredOrFocused()
                                ? W129IdeTheme.ACCENT_DARK
                                : W129IdeTheme.BORDER_SOFT
                );

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
                background
        );

        String label =
                getMessage().getString();

        boolean fileLike =
                label.contains("/")
                        && (
                        label.startsWith("/")
                                || label.startsWith("G /")
                                || label.startsWith("M /")
                                || label.startsWith("  /")
                );

        if (fileLike) {
            drawFileButton(
                    graphics,
                    label
            );
            return;
        }

        drawActionButton(
                graphics,
                label
        );
    }

    private void drawFileButton(
            GuiGraphics graphics,
            String label
    ) {
        int iconX = getX() + 6;
        int iconY =
                getY()
                        + Math.max(
                        3,
                        (height - 8) / 2
                );

        int iconColor =
                label.contains(".")
                        ? W129IdeTheme.FILE
                        : W129IdeTheme.FOLDER;

        graphics.fill(
                iconX,
                iconY,
                iconX + 7,
                iconY + 8,
                iconColor
        );

        graphics.fill(
                iconX + 2,
                iconY + 2,
                iconX + 7,
                iconY + 3,
                0xAAFFFFFF
        );

        graphics.drawString(
                Minecraft.getInstance().font,
                label.stripLeading(),
                iconX + 12,
                getY()
                        + (
                        height
                                - Minecraft.getInstance()
                                .font.lineHeight
                ) / 2,
                active
                        ? W129IdeTheme.TEXT
                        : 0xFFFFFFFF,
                false
        );
    }

    private void drawActionButton(
            GuiGraphics graphics,
            String label
    ) {
        int textColor =
                active
                        ? W129IdeTheme.TEXT
                        : 0xFFFFFFFF;

        int iconColor =
                actionColor(label);

        if (iconColor != 0) {
            int iconX =
                    getX() + 6;
            int iconY =
                    getY()
                            + Math.max(
                            4,
                            (height - 7) / 2
                    );

            graphics.fill(
                    iconX,
                    iconY,
                    iconX + 7,
                    iconY + 7,
                    iconColor
            );

            int textWidth =
                    Minecraft.getInstance()
                            .font.width(label);

            int textX =
                    Math.max(
                            iconX + 11,
                            getX()
                                    + (
                                    width
                                            - textWidth
                            ) / 2
                    );

            graphics.drawString(
                    Minecraft.getInstance().font,
                    label,
                    textX,
                    getY()
                            + (
                            height
                                    - Minecraft.getInstance()
                                    .font.lineHeight
                    ) / 2,
                    textColor,
                    false
            );

            return;
        }

        graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getMessage(),
                getX() + width / 2,
                getY()
                        + (
                        height
                                - Minecraft.getInstance()
                                .font.lineHeight
                ) / 2,
                textColor
        );
    }

    private int actionColor(
            String label
    ) {
        String value =
                label.toLowerCase();

        if (
                value.equals("run")
                        || value.contains("run current")
        ) {
            return W129IdeTheme.RUN;
        }

        if (
                value.equals("delete")
                        || value.equals("x")
        ) {
            return W129IdeTheme.DELETE;
        }

        if (
                value.equals("build")
                        || value.contains("publish")
                        || value.equals("save")
        ) {
            return W129IdeTheme.ACCENT;
        }

        if (
                value.equals("check")
                        || value.contains("validate")
        ) {
            return W129IdeTheme.SUCCESS;
        }

        return 0;
    }

    public static final class Builder {
        private final Component message;
        private final OnPress onPress;

        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        private Builder(
                Component message,
                OnPress onPress
        ) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(
                int x,
                int y,
                int width,
                int height
        ) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public W129IdeButton build() {
            return new W129IdeButton(
                    x,
                    y,
                    width,
                    height,
                    message,
                    onPress
            );
        }
    }
}
