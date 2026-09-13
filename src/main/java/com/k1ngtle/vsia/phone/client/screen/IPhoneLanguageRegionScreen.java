package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneLanguageRegionScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 38;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneLanguageRegionScreen() {
        super(Component.literal("Language & Region"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 82;
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
                "Language & Region"
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                ROW_HEIGHT * 5,
                14,
                CARD
        );

        row(
                graphics,
                groupY,
                "iPhone Language",
                PhoneLocaleSettings.language().displayName()
        );
        divider(graphics, groupY + ROW_HEIGHT);

        row(
                graphics,
                groupY + ROW_HEIGHT,
                "Region",
                PhoneLocaleSettings.region().displayName()
        );
        divider(graphics, groupY + ROW_HEIGHT * 2);

        row(
                graphics,
                groupY + ROW_HEIGHT * 2,
                "Temperature",
                PhoneLocaleSettings.temperatureUnit().displayName()
        );
        divider(graphics, groupY + ROW_HEIGHT * 3);

        row(
                graphics,
                groupY + ROW_HEIGHT * 3,
                "Measurement System",
                PhoneLocaleSettings.measurementSystem().displayName()
        );
        divider(graphics, groupY + ROW_HEIGHT * 4);

        row(
                graphics,
                groupY + ROW_HEIGHT * 4,
                "First Day of Week",
                PhoneLocaleSettings.firstDayOfWeek().displayName()
        );

        drawUiWrappedCentered(
                graphics,
                "Region changes date formatting and default units. Temperature changes the Weather widget immediately.",
                phoneX + PHONE_WIDTH / 2,
                groupY + ROW_HEIGHT * 5 + 19,
                PHONE_WIDTH - 48,
                11,
                5,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void row(
            GuiGraphics graphics,
            int y,
            String left,
            String right
    ) {
        drawUiText(
                graphics,
                fitUi(left, 106),
                contentX + 13,
                y + 14,
                TEXT
        );

        String shown = fitUi(
                right,
                94
        );

        drawUiText(
                graphics,
                shown,
                contentX
                        + contentWidth
                        - uiWidth(shown)
                        - 21,
                y + 14,
                MUTED
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 11,
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

            if (!inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY,
                    contentWidth,
                    ROW_HEIGHT * 5
            )) {
                return super.mouseClicked(
                        mouseX,
                        mouseY,
                        button
                );
            }

            int row =
                    ((int) mouseY - groupY)
                            / ROW_HEIGHT;

            switch (row) {
                case 0 ->
                        PhoneLocaleSettings.cycleLanguage();

                case 1 ->
                        PhoneLocaleSettings.cycleRegion();

                case 2 ->
                        PhoneLocaleSettings.cycleTemperatureUnit();

                case 3 ->
                        PhoneLocaleSettings.cycleMeasurementSystem();

                case 4 ->
                        PhoneLocaleSettings.cycleFirstDayOfWeek();

                default -> {
                }
            }

            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
