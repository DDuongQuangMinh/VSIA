package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneDateTimeScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 38;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneDateTimeScreen() {
        super(Component.literal("Date & Time"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 84;
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

        renderHeader(
                graphics,
                "General",
                "Date & Time"
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                ROW_HEIGHT * 4,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "24-Hour Time",
                contentX + 13,
                groupY + 14,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                groupY + 9,
                PhoneSystemSettings.use24HourTime()
        );

        divider(
                graphics,
                groupY + ROW_HEIGHT
        );

        pair(
                graphics,
                groupY + ROW_HEIGHT,
                "Date",
                PhoneLocaleSettings.formatDate(
                        PhoneLocaleSettings.currentDate()
                )
        );

        divider(
                graphics,
                groupY + ROW_HEIGHT * 2
        );

        pair(
                graphics,
                groupY + ROW_HEIGHT * 2,
                "Time",
                PhoneLocaleSettings.formatTime(
                        PhoneLocaleSettings.currentTime(),
                        PhoneSystemSettings.use24HourTime()
                )
        );

        divider(
                graphics,
                groupY + ROW_HEIGHT * 3
        );

        pair(
                graphics,
                groupY + ROW_HEIGHT * 3,
                "Time Zone",
                PhoneLocaleSettings.timeZoneId()
        );

        drawUiWrappedCentered(
                graphics,
                "The simulated phone follows the selected region's time zone.",
                phoneX + PHONE_WIDTH / 2,
                groupY + ROW_HEIGHT * 4 + 20,
                PHONE_WIDTH - 48,
                11,
                4,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void pair(
            GuiGraphics graphics,
            int y,
            String left,
            String right
    ) {
        drawUiText(
                graphics,
                left,
                contentX + 13,
                y + 14,
                TEXT
        );

        int available =
                Math.max(
                        58,
                        contentWidth
                                - uiWidth(left)
                                - 40
                );

        String shown =
                fitUi(
                        right,
                        available
                );

        drawUiText(
                graphics,
                shown,
                contentX
                        + contentWidth
                        - uiWidth(shown)
                        - 13,
                y + 14,
                MUTED
        );
    }

    private void divider(
            GuiGraphics graphics,
            int y
    ) {
        graphics.fill(
                contentX + 13,
                y,
                contentX + contentWidth - 13,
                y + 1,
                DIVIDER
        );
    }

    private void drawToggle(
            GuiGraphics graphics,
            int x,
            int y,
            boolean enabled
    ) {
        roundedRect(
                graphics,
                x,
                y,
                36,
                20,
                10,
                enabled
                        ? GREEN
                        : 0xFF636366
        );

        roundedRect(
                graphics,
                enabled
                        ? x + 19
                        : x + 3,
                y + 3,
                14,
                14,
                7,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (clickedBack(
                    mouseX,
                    mouseY
            )) {
                minecraft.setScreen(
                        new IPhoneGeneralScreen()
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                PhoneSystemSettings.setUse24HourTime(
                        !PhoneSystemSettings
                                .use24HourTime()
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
}
