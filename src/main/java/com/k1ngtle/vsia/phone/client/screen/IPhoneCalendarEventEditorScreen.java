package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalDate;
import java.time.LocalTime;

public final class IPhoneCalendarEventEditorScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int RED = 0xFFFF453A;
    private static final int BLUE = 0xFF0A84FF;
    private static final int DIVIDER = 0xFF3A3A3C;

    private final long eventId;

    private String titleText = "";
    private LocalDate eventDate;
    private int hour = 9;
    private int minute;

    private int contentX;
    private int contentWidth;
    private int titleY;
    private int detailsY;
    private int saveY;

    public IPhoneCalendarEventEditorScreen(
            long eventId,
            LocalDate initialDate
    ) {
        super(Component.literal("Add Event"));
        this.eventId = eventId;
        this.eventDate =
                initialDate == null
                        ? PhoneLocaleSettings.currentDate()
                        : initialDate;
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        titleY = phoneY + 83;
        detailsY = titleY + 54;
        saveY = phoneY + PHONE_HEIGHT - 70;

        if (eventId > 0L) {
            PhonePersonalAppsState.CalendarEvent event =
                    PhonePersonalAppsState.event(
                            eventId
                    );

            if (event != null) {
                titleText = event.title();
                eventDate =
                        LocalDate.ofEpochDay(
                                event.epochDay()
                        );

                hour = event.hour();
                minute = event.minute();
            }
        }
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
                eventId > 0L
                        ? "Edit Event"
                        : "Add Event",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                titleY,
                contentWidth,
                40,
                12,
                CARD
        );

        drawUiText(
                graphics,
                titleText.isEmpty()
                        ? "Event Title"
                        : fitUi(
                                titleText + "|",
                                contentWidth - 26
                        ),
                contentX + 13,
                titleY + 14,
                titleText.isEmpty()
                        ? MUTED
                        : TEXT
        );

        roundedRect(
                graphics,
                contentX,
                detailsY,
                contentWidth,
                82,
                12,
                CARD
        );

        drawUiText(
                graphics,
                "Date",
                contentX + 13,
                detailsY + 14,
                TEXT
        );

        drawUiText(
                graphics,
                "‹",
                contentX + 84,
                detailsY + 14,
                BLUE
        );

        String dateText =
                fitUi(
                        PhoneLocaleSettings.formatDate(
                                eventDate
                        ),
                        90
                );

        drawUiText(
                graphics,
                dateText,
                contentX + 101,
                detailsY + 14,
                MUTED
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 18,
                detailsY + 14,
                BLUE
        );

        graphics.fill(
                contentX + 13,
                detailsY + 41,
                contentX + contentWidth - 13,
                detailsY + 42,
                DIVIDER
        );

        drawUiText(
                graphics,
                "Time",
                contentX + 13,
                detailsY + 56,
                TEXT
        );

        drawUiText(
                graphics,
                "‹",
                contentX + 84,
                detailsY + 56,
                BLUE
        );

        String timeText =
                PhoneLocaleSettings.formatTime(
                        LocalTime.of(
                                hour,
                                minute
                        ),
                        PhoneSystemSettings.use24HourTime()
                );

        drawUiText(
                graphics,
                timeText,
                contentX + 101,
                detailsY + 56,
                MUTED
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 18,
                detailsY + 56,
                BLUE
        );

        roundedRect(
                graphics,
                contentX,
                saveY,
                contentWidth,
                36,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "Save",
                phoneX + PHONE_WIDTH / 2,
                saveY + 13,
                RED
        );

        if (eventId > 0L) {
            drawUiText(
                    graphics,
                    "Delete",
                    contentX + 5,
                    saveY - 20,
                    RED
            );
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers
    ) {
        if (!Character.isISOControl(codePoint)
                && titleText.length() < 80) {
            titleText += codePoint;
            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == 259) {
            if (!titleText.isEmpty()) {
                titleText =
                        titleText.substring(
                                0,
                                titleText.length() - 1
                        );
            }

            return true;
        }

        if (keyCode == 256) {
            minecraft.setScreen(
                    new IPhoneCalendarScreen()
            );

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
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
                    contentX + 72,
                    detailsY,
                    28,
                    41
            )) {
                eventDate =
                        eventDate.minusDays(1L);

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + contentWidth - 40,
                    detailsY,
                    40,
                    41
            )) {
                eventDate =
                        eventDate.plusDays(1L);

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + 72,
                    detailsY + 42,
                    28,
                    40
            )) {
                adjustTime(-30);
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + contentWidth - 40,
                    detailsY + 42,
                    40,
                    40
            )) {
                adjustTime(30);
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    saveY,
                    contentWidth,
                    36
            )) {
                PhonePersonalAppsState.saveEvent(
                        eventId,
                        eventDate,
                        hour,
                        minute,
                        titleText
                );

                minecraft.setScreen(
                        new IPhoneCalendarScreen()
                );

                return true;
            }

            if (eventId > 0L
                    && inside(
                    mouseX,
                    mouseY,
                    contentX,
                    saveY - 25,
                    55,
                    22
            )) {
                PhonePersonalAppsState.deleteEvent(
                        eventId
                );

                minecraft.setScreen(
                        new IPhoneCalendarScreen()
                );

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private void adjustTime(int minutesDelta) {
        int total =
                hour * 60
                        + minute
                        + minutesDelta;

        total =
                Math.floorMod(
                        total,
                        24 * 60
                );

        hour = total / 60;
        minute = total % 60;
    }
}
