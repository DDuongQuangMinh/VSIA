package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class IPhoneCalendarScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int RED = 0xFFFF453A;
    private static final int BLUE = 0xFF0A84FF;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 42;

    private LocalDate selectedDate;

    private int contentX;
    private int contentWidth;
    private int dateY;
    private int listY;
    private int addY;

    public IPhoneCalendarScreen() {
        super(Component.literal("Calendar"));
    }

    @Override
    protected void init() {
        super.init();

        selectedDate =
                PhoneLocaleSettings.currentDate();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        dateY = phoneY + 80;
        listY = dateY + 68;
        addY = phoneY + PHONE_HEIGHT - 70;
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
                "Calendar",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                dateY,
                contentWidth,
                50,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "‹",
                contentX + 13,
                dateY + 18,
                BLUE
        );

        drawUiCentered(
                graphics,
                PhoneLocaleSettings.formatDate(
                        selectedDate
                ),
                phoneX + PHONE_WIDTH / 2,
                dateY + 12,
                TEXT
        );

        drawUiCentered(
                graphics,
                selectedDate.equals(
                        PhoneLocaleSettings.currentDate()
                )
                        ? "Today"
                        : selectedDate
                        .getDayOfWeek()
                        .getDisplayName(
                                java.time.format.TextStyle.FULL,
                                PhoneLocaleSettings.regionLocale()
                        ),
                phoneX + PHONE_WIDTH / 2,
                dateY + 29,
                RED
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 19,
                dateY + 18,
                BLUE
        );

        List<PhonePersonalAppsState.CalendarEvent> events =
                PhonePersonalAppsState.eventsFor(
                        selectedDate
                );

        if (events.isEmpty()) {
            drawUiCentered(
                    graphics,
                    "No Events",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 34,
                    MUTED
            );
        } else {
            int visible =
                    Math.min(
                            5,
                            events.size()
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
                PhonePersonalAppsState.CalendarEvent event =
                        events.get(i);

                int y =
                        listY + i * ROW_HEIGHT;

                LocalTime time =
                        LocalTime.of(
                                event.hour(),
                                event.minute()
                        );

                drawUiText(
                        graphics,
                        PhoneLocaleSettings.formatTime(
                                time,
                                PhoneSystemSettings.use24HourTime()
                        ),
                        contentX + 13,
                        y + 9,
                        RED
                );

                drawUiText(
                        graphics,
                        fitUi(
                                event.title(),
                                contentWidth - 78
                        ),
                        contentX + 68,
                        y + 9,
                        TEXT
                );

                drawUiText(
                        graphics,
                        "›",
                        contentX + contentWidth - 11,
                        y + 9,
                        MUTED
                );

                if (i + 1 < visible) {
                    graphics.fill(
                            contentX + 13,
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
                addY,
                contentWidth,
                36,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "+ Add Event",
                phoneX + PHONE_WIDTH / 2,
                addY + 13,
                RED
        );

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
                    dateY,
                    42,
                    50
            )) {
                selectedDate =
                        selectedDate.minusDays(1L);

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX + contentWidth - 42,
                    dateY,
                    42,
                    50
            )) {
                selectedDate =
                        selectedDate.plusDays(1L);

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    addY,
                    contentWidth,
                    36
            )) {
                minecraft.setScreen(
                        new IPhoneCalendarEventEditorScreen(
                                -1L,
                                selectedDate
                        )
                );

                return true;
            }

            List<PhonePersonalAppsState.CalendarEvent> events =
                    PhonePersonalAppsState.eventsFor(
                            selectedDate
                    );

            int visible =
                    Math.min(
                            5,
                            events.size()
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
                    minecraft.setScreen(
                            new IPhoneCalendarEventEditorScreen(
                                    events.get(index).id(),
                                    selectedDate
                            )
                    );

                    return true;
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
