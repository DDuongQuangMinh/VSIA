package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSoftwareUpdateState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneGeneralScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 38;

    private int contentX;
    private int contentWidth;
    private int firstY;
    private int secondY;

    public IPhoneGeneralScreen() {
        super(Component.literal("General"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        firstY = phoneY + 82;
        secondY = firstY + ROW_HEIGHT * 2 + 18;
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
        renderHeader(graphics, "Settings", "General");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                firstY,
                contentWidth,
                ROW_HEIGHT * 2,
                14,
                CARD
        );

        navRow(
                graphics,
                firstY,
                "About",
                ""
        );

        divider(
                graphics,
                firstY + ROW_HEIGHT
        );

        navRow(
                graphics,
                firstY + ROW_HEIGHT,
                "Software Update",
                "PhoneOS " + PhoneSoftwareUpdateState.currentVersion()
        );

        roundedRect(
                graphics,
                contentX,
                secondY,
                contentWidth,
                ROW_HEIGHT * 3,
                14,
                CARD
        );

        navRow(
                graphics,
                secondY,
                "Date & Time",
                ""
        );

        divider(
                graphics,
                secondY + ROW_HEIGHT
        );

        navRow(
                graphics,
                secondY + ROW_HEIGHT,
                "Language & Region",
                "English (UK)"
        );

        divider(
                graphics,
                secondY + ROW_HEIGHT * 2
        );

        navRow(
                graphics,
                secondY + ROW_HEIGHT * 2,
                "Transfer or Reset iPhone",
                ""
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void navRow(
            GuiGraphics graphics,
            int y,
            String label,
            String summary
    ) {
        drawUiText(
                graphics,
                label,
                contentX + 13,
                y + 14,
                TEXT
        );

        if (summary != null
                && !summary.isBlank()) {
            String shown = fitUi(
                    summary,
                    92
            );

            drawUiText(
                    graphics,
                    shown,
                    contentX
                            + contentWidth
                            - uiWidth(shown)
                            - 22,
                    y + 14,
                    MUTED
            );
        }

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
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    firstY,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneAboutScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    firstY + ROW_HEIGHT,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneSoftwareUpdateScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    secondY,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneDateTimeScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    secondY + ROW_HEIGHT,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneLanguageRegionScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    secondY + ROW_HEIGHT * 2,
                    contentWidth,
                    ROW_HEIGHT
            )) {
                minecraft.setScreen(
                        new IPhoneTransferResetScreen()
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
