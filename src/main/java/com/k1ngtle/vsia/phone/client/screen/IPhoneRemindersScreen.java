package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IPhoneRemindersScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 40;

    private String inputText = "";
    private boolean inputActive;

    private int contentX;
    private int contentWidth;
    private int listY;
    private int inputY;
    private int addY;

    public IPhoneRemindersScreen() {
        super(Component.literal("Reminders"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        listY = phoneY + 82;
        inputY = phoneY + PHONE_HEIGHT - 108;
        addY = phoneY + PHONE_HEIGHT - 68;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);

        drawUiCentered(
                graphics,
                "Reminders",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        List<PhonePersonalAppsState.Reminder> reminders =
                PhonePersonalAppsState.reminders();

        if (reminders.isEmpty()) {
            drawUiCentered(
                    graphics,
                    "No Reminders",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 34,
                    MUTED
            );
        } else {
            int visible =
                    Math.min(
                            6,
                            reminders.size()
                    );

            roundedRect(
                    graphics,
                    contentX,
                    listY,
                    contentWidth,
                    visible * ROW_HEIGHT,
                    14,
                    CARD
            );

            for (int i = 0; i < visible; i++) {
                PhonePersonalAppsState.Reminder reminder =
                        reminders.get(i);

                int y =
                        listY + i * ROW_HEIGHT;

                roundedRect(
                        graphics,
                        contentX + 11,
                        y + 11,
                        14,
                        14,
                        7,
                        reminder.completed()
                                ? GREEN
                                : 0xFF5A5A5E
                );

                if (reminder.completed()) {
                    drawUiCentered(
                            graphics,
                            "✓",
                            contentX + 18,
                            y + 13,
                            TEXT
                    );
                }

                drawUiText(
                        graphics,
                        fitUi(
                                reminder.title(),
                                contentWidth - 52
                        ),
                        contentX + 34,
                        y + 14,
                        reminder.completed()
                                ? MUTED
                                : TEXT
                );

                if (i + 1 < visible) {
                    graphics.fill(
                            contentX + 34,
                            y + ROW_HEIGHT - 1,
                            contentX + contentWidth - 13,
                            y + ROW_HEIGHT,
                            DIVIDER
                    );
                }
            }
        }

        roundedRect(
                graphics,
                contentX,
                inputY,
                contentWidth,
                32,
                10,
                CARD
        );

        drawUiText(
                graphics,
                inputText.isEmpty()
                        ? "Type reminder..."
                        : fitUi(
                                inputText
                                        + (inputActive ? "|" : ""),
                                contentWidth - 26
                        ),
                contentX + 13,
                inputY + 11,
                inputText.isEmpty()
                        ? MUTED
                        : TEXT
        );

        roundedRect(
                graphics,
                contentX,
                addY,
                contentWidth,
                32,
                10,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "+ Add Reminder",
                phoneX + PHONE_WIDTH / 2,
                addY + 11,
                BLUE
        );

        if (reminders.stream().anyMatch(
                PhonePersonalAppsState.Reminder::completed
        )) {
            drawUiText(
                    graphics,
                    "Clear Completed",
                    contentX + 5,
                    inputY - 20,
                    BLUE
            );
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    inputY,
                    contentWidth,
                    32
            )) {
                inputActive = true;
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    addY,
                    contentWidth,
                    32
            )) {
                addReminder();
                return true;
            }

            List<PhonePersonalAppsState.Reminder> reminders =
                    PhonePersonalAppsState.reminders();

            int visible =
                    Math.min(
                            6,
                            reminders.size()
                    );

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    listY,
                    contentWidth,
                    visible * ROW_HEIGHT
            )) {
                int index =
                        ((int) mouseY - listY)
                                / ROW_HEIGHT;

                if (index >= 0
                        && index < visible) {
                    PhonePersonalAppsState.toggleReminder(
                            reminders.get(index).id()
                    );

                    return true;
                }
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    inputY - 25,
                    100,
                    22
            )) {
                PhonePersonalAppsState.clearCompletedReminders();
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers
    ) {
        if (!inputActive
                || Character.isISOControl(codePoint)) {
            return false;
        }

        if (inputText.length() < 120) {
            inputText += codePoint;
        }

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == 259
                && inputActive) {
            if (!inputText.isEmpty()) {
                inputText =
                        inputText.substring(
                                0,
                                inputText.length() - 1
                        );
            }

            return true;
        }

        if ((keyCode == 257
                || keyCode == 335)
                && inputActive) {
            addReminder();
            return true;
        }

        if (keyCode == 256
                && inputActive) {
            inputActive = false;
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    private void addReminder() {
        if (inputText.isBlank()) {
            inputActive = true;
            return;
        }

        PhonePersonalAppsState.addReminder(
                inputText
        );

        inputText = "";
        inputActive = true;
    }
}
