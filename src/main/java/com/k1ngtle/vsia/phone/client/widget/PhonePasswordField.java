package com.k1ngtle.vsia.phone.client.widget;

import com.k1ngtle.vsia.phone.client.PhoneText;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PhonePasswordField
        extends AbstractWidget {

    private final Font font;

    private String value =
            "";

    private String placeholder =
            "Password";

    private int cursor;
    private int maxLength =
            63;

    private boolean reveal;

    public PhonePasswordField(
            Font font,
            int x,
            int y,
            int width,
            int height
    ) {
        super(
                x,
                y,
                width,
                height,
                Component.literal(
                        "Wi-Fi password"
                )
        );

        this.font =
                font;
    }

    public String getValue() {
        return value;
    }

    public void setValue(
            String newValue
    ) {
        value =
                sanitize(
                        newValue == null
                                ? ""
                                : newValue
                );

        if (value.length()
                > maxLength) {
            value =
                    value.substring(
                            0,
                            maxLength
                    );
        }

        cursor =
                value.length();
    }

    public void setPlaceholder(
            String placeholder
    ) {
        this.placeholder =
                placeholder == null
                        ? ""
                        : placeholder;
    }

    public void setMaxLength(
            int maxLength
    ) {
        this.maxLength =
                Math.max(
                        1,
                        maxLength
                );

        if (value.length()
                > this.maxLength) {
            setValue(
                    value.substring(
                            0,
                            this.maxLength
                    )
            );
        }
    }

    public void setReveal(
            boolean reveal
    ) {
        this.reveal =
                reveal;
    }

    public boolean isReveal() {
        return reveal;
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return false;
        }

        boolean inside =
                mouseX >= getX()
                        && mouseX < getX()
                        + width
                        && mouseY >= getY()
                        && mouseY < getY()
                        + height;

        setFocused(
                inside
        );

        if (!inside) {
            return false;
        }

        cursor =
                value.length();

        return true;
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers
    ) {
        if (!isFocused()
                || !SharedConstants
                .isAllowedChatCharacter(
                        codePoint
                )
                || value.length()
                >= maxLength) {
            return false;
        }

        value =
                value.substring(
                        0,
                        cursor
                )
                        + codePoint
                        + value.substring(
                        cursor
                );

        cursor++;

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (!isFocused()) {
            return false;
        }

        if (Screen.hasControlDown()
                && keyCode
                == GLFW.GLFW_KEY_A) {
            cursor =
                    value.length();

            return true;
        }

        if (Screen.hasControlDown()
                && keyCode
                == GLFW.GLFW_KEY_V) {
            insert(
                    Minecraft.getInstance()
                            .keyboardHandler
                            .getClipboard()
            );

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursor > 0) {
                value =
                        value.substring(
                                0,
                                cursor - 1
                        )
                                + value.substring(
                                cursor
                        );

                cursor--;
            }

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_DELETE) {
            if (cursor
                    < value.length()) {
                value =
                        value.substring(
                                0,
                                cursor
                        )
                                + value.substring(
                                cursor + 1
                        );
            }

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_LEFT) {
            cursor =
                    Math.max(
                            0,
                            cursor - 1
                    );

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_RIGHT) {
            cursor =
                    Math.min(
                            value.length(),
                            cursor + 1
                    );

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_HOME) {
            cursor =
                    0;

            return true;
        }

        if (keyCode
                == GLFW.GLFW_KEY_END) {
            cursor =
                    value.length();

            return true;
        }

        return false;
    }

    @Override
    protected void renderWidget(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        String display =
                displayValue();

        int start =
                visibleStart(
                        display
                );

        String shown =
                display.substring(
                        Math.min(
                                start,
                                display.length()
                        )
                );

        graphics.enableScissor(
                getX(),
                getY(),
                getX() + width,
                getY() + height
        );

        if (shown.isBlank()
                && !isFocused()) {
            PhoneText.draw(
                    graphics,
                    font,
                    placeholder,
                    getX() + 4,
                    getY() + 5,
                    0xFF8E8E93
            );
        } else {
            PhoneText.draw(
                    graphics,
                    font,
                    shown,
                    getX() + 4,
                    getY() + 5,
                    0xFFFFFFFF
            );

            if (isFocused()
                    && cursorBlinkVisible()) {
                int relativeCursor =
                        Math.max(
                                0,
                                cursor - start
                        );

                String before =
                        shown.substring(
                                0,
                                Math.min(
                                        relativeCursor,
                                        shown.length()
                                )
                        );

                int cursorX =
                        getX()
                                + 4
                                + PhoneText.width(
                                font,
                                before
                        );

                graphics.fill(
                        cursorX,
                        getY() + 3,
                        cursorX + 1,
                        getY()
                                + height
                                - 3,
                        0xFF0A84FF
                );
            }
        }

        graphics.disableScissor();
    }

    private void insert(
            String input
    ) {
        String clean =
                sanitize(
                        input == null
                                ? ""
                                : input
                );

        int room =
                maxLength
                        - value.length();

        if (room <= 0
                || clean.isEmpty()) {
            return;
        }

        if (clean.length()
                > room) {
            clean =
                    clean.substring(
                            0,
                            room
                    );
        }

        value =
                value.substring(
                        0,
                        cursor
                )
                        + clean
                        + value.substring(
                        cursor
                );

        cursor +=
                clean.length();
    }

    private String displayValue() {
        if (reveal) {
            return value;
        }

        return "*".repeat(
                value.length()
        );
    }

    private int visibleStart(
            String display
    ) {
        int available =
                Math.max(
                        1,
                        width - 8
                );

        if (PhoneText.width(
                font,
                display
        ) <= available) {
            return 0;
        }

        int start =
                Math.min(
                        cursor,
                        display.length()
                );

        while (start > 0
                && PhoneText.width(
                font,
                display.substring(
                        start - 1,
                        Math.min(
                                display.length(),
                                cursor
                        )
                )
        ) <= available) {
            start--;
        }

        return start;
    }

    private boolean cursorBlinkVisible() {
        return System.currentTimeMillis()
                / 500L
                % 2L
                == 0L;
    }

    private static String sanitize(
            String input
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < input.length();
             i++) {
            char c =
                    input.charAt(
                            i
                    );

            if (SharedConstants
                    .isAllowedChatCharacter(
                            c
                    )) {
                builder.append(
                        c
                );
            }
        }

        return builder.toString();
    }

    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput output
    ) {
        defaultButtonNarrationText(
                output
        );
    }
}
