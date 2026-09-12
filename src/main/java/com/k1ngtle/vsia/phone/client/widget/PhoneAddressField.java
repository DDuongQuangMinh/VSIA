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

public final class PhoneAddressField extends AbstractWidget {
    private final Font font;

    private String value = "";
    private String placeholder = "Search or enter website name";

    private int cursor;
    private int anchor;
    private int maxLength = 512;

    public PhoneAddressField(
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
                Component.literal("Website address")
        );

        this.font = font;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String newValue) {
        value = sanitize(
                newValue == null
                        ? ""
                        : newValue
        );

        if (value.length() > maxLength) {
            value = value.substring(
                    0,
                    maxLength
            );
        }

        cursor = value.length();
        anchor = cursor;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder =
                placeholder == null
                        ? ""
                        : placeholder;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength =
                Math.max(
                        1,
                        maxLength
                );

        if (value.length() > this.maxLength) {
            setValue(
                    value.substring(
                            0,
                            this.maxLength
                    )
            );
        }
    }

    public void moveCursorToEnd() {
        cursor = value.length();
        anchor = cursor;
    }

    public void selectAll() {
        anchor = 0;
        cursor = value.length();
    }

    public boolean hasSelection() {
        return cursor != anchor;
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
                        && mouseX < getX() + width
                        && mouseY >= getY()
                        && mouseY < getY() + height;

        setFocused(inside);

        if (!inside) {
            return false;
        }

        int start = visibleStart();
        int localX =
                Math.max(
                        0,
                        (int) mouseX - getX() - 2
                );

        int best = start;

        for (int i = start;
             i <= value.length();
             i++) {
            int width =
                    PhoneText.width(
                            font,
                            value.substring(
                                    start,
                                    i
                            )
                    );

            if (width <= localX) {
                best = i;
            } else {
                break;
            }
        }

        cursor = best;
        anchor = cursor;

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
                )) {
            return false;
        }

        replaceSelection(
                Character.toString(
                        codePoint
                )
        );

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

        boolean control =
                Screen.hasControlDown();

        if (control
                && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }

        if (control
                && keyCode == GLFW.GLFW_KEY_C) {
            copySelection();
            return true;
        }

        if (control
                && keyCode == GLFW.GLFW_KEY_X) {
            copySelection();
            deleteSelection();
            return true;
        }

        if (control
                && keyCode == GLFW.GLFW_KEY_V) {
            replaceSelection(
                    Minecraft.getInstance()
                            .keyboardHandler
                            .getClipboard()
            );

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursor > 0) {
                value =
                        value.substring(
                                0,
                                cursor - 1
                        )
                                + value.substring(
                                cursor
                        );

                cursor--;
                anchor = cursor;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursor < value.length()) {
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

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            if (hasSelection()
                    && !Screen.hasShiftDown()) {
                cursor =
                        Math.min(
                                cursor,
                                anchor
                        );

                anchor = cursor;
            } else {
                cursor =
                        Math.max(
                                0,
                                cursor - 1
                        );

                if (!Screen.hasShiftDown()) {
                    anchor = cursor;
                }
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (hasSelection()
                    && !Screen.hasShiftDown()) {
                cursor =
                        Math.max(
                                cursor,
                                anchor
                        );

                anchor = cursor;
            } else {
                cursor =
                        Math.min(
                                value.length(),
                                cursor + 1
                        );

                if (!Screen.hasShiftDown()) {
                    anchor = cursor;
                }
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursor = 0;

            if (!Screen.hasShiftDown()) {
                anchor = cursor;
            }

            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_END) {
            cursor = value.length();

            if (!Screen.hasShiftDown()) {
                anchor = cursor;
            }

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
        int start = visibleStart();
        String shown =
                value.substring(
                        Math.min(
                                start,
                                value.length()
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
                    getX() + 2,
                    getY() + 4,
                    0xFF8E8E93
            );
        } else {
            renderSelection(
                    graphics,
                    start
            );

            PhoneText.draw(
                    graphics,
                    font,
                    shown,
                    getX() + 2,
                    getY() + 4,
                    0xFF111111
            );

            if (isFocused()
                    && cursorBlinkVisible()) {
                int cursorX =
                        getX()
                                + 2
                                + PhoneText.width(
                                font,
                                value.substring(
                                        start,
                                        Math.max(
                                                start,
                                                cursor
                                        )
                                )
                        );

                graphics.fill(
                        cursorX,
                        getY() + 2,
                        cursorX + 1,
                        getY() + height - 2,
                        0xFF0A84FF
                );
            }
        }

        graphics.disableScissor();
    }

    private void renderSelection(
            GuiGraphics graphics,
            int visibleStart
    ) {
        if (!hasSelection()) {
            return;
        }

        int selectionStart =
                Math.min(
                        cursor,
                        anchor
                );

        int selectionEnd =
                Math.max(
                        cursor,
                        anchor
                );

        if (selectionEnd <= visibleStart) {
            return;
        }

        int drawStart =
                Math.max(
                        selectionStart,
                        visibleStart
                );

        int x1 =
                getX()
                        + 2
                        + PhoneText.width(
                        font,
                        value.substring(
                                visibleStart,
                                drawStart
                        )
                );

        int x2 =
                getX()
                        + 2
                        + PhoneText.width(
                        font,
                        value.substring(
                                visibleStart,
                                selectionEnd
                        )
                );

        graphics.fill(
                x1,
                getY() + 2,
                x2,
                getY() + height - 2,
                0x663A86FF
        );
    }

    private int visibleStart() {
        if (value.isEmpty()) {
            return 0;
        }

        int available =
                Math.max(
                        1,
                        width - 5
                );

        int start =
                Math.min(
                        cursor,
                        value.length()
                );

        while (start > 0
                && PhoneText.width(
                font,
                value.substring(
                        start - 1,
                        Math.min(
                                value.length(),
                                Math.max(
                                        cursor,
                                        start
                                )
                        )
                )
        ) <= available) {
            start--;
        }

        if (PhoneText.width(
                font,
                value
        ) <= available) {
            return 0;
        }

        return start;
    }

    private boolean cursorBlinkVisible() {
        return System.currentTimeMillis()
                / 500L
                % 2L
                == 0L;
    }

    private void replaceSelection(
            String insertion
    ) {
        String clean =
                sanitize(
                        insertion == null
                                ? ""
                                : insertion
                );

        int start =
                Math.min(
                        cursor,
                        anchor
                );

        int end =
                Math.max(
                        cursor,
                        anchor
                );

        int allowed =
                maxLength
                        - (
                        value.length()
                                - (
                                end - start
                        )
                );

        if (allowed <= 0) {
            return;
        }

        if (clean.length() > allowed) {
            clean =
                    clean.substring(
                            0,
                            allowed
                    );
        }

        value =
                value.substring(
                        0,
                        start
                )
                        + clean
                        + value.substring(
                        end
                );

        cursor =
                start + clean.length();

        anchor = cursor;
    }

    private void deleteSelection() {
        if (!hasSelection()) {
            return;
        }

        replaceSelection("");
    }

    private void copySelection() {
        if (!hasSelection()) {
            return;
        }

        int start =
                Math.min(
                        cursor,
                        anchor
                );

        int end =
                Math.max(
                        cursor,
                        anchor
                );

        Minecraft.getInstance()
                .keyboardHandler
                .setClipboard(
                        value.substring(
                                start,
                                end
                        )
                );
    }

    private static String sanitize(
            String input
    ) {
        StringBuilder builder =
                new StringBuilder();

        for (int i = 0;
             i < input.length();
             i++) {
            char c = input.charAt(i);

            if (SharedConstants
                    .isAllowedChatCharacter(c)) {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    @Override
    protected void updateWidgetNarration(
            NarrationElementOutput output
    ) {
        defaultButtonNarrationText(output);
    }
}
