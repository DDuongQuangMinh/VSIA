package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneTransferResetScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int RED = 0xFFFF453A;
    private static final int DIVIDER = 0xFF3A3A3C;

    private int contentX;
    private int contentWidth;
    private int prepareY;
    private int actionsY;

    public IPhoneTransferResetScreen() {
        super(Component.literal("Transfer or Reset iPhone"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        prepareY = phoneY + 82;
        actionsY = prepareY + 96;
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
                "Transfer or Reset"
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                prepareY,
                contentWidth,
                72,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Prepare for New iPhone",
                contentX + 13,
                prepareY + 12,
                TEXT
        );

        drawUiWrappedCentered(
                graphics,
                "VS:IA does not yet have a cloud backup service. Your server-side SIM and SMS data are not copied by this screen.",
                phoneX + PHONE_WIDTH / 2,
                prepareY + 31,
                contentWidth - 24,
                11,
                4,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                actionsY,
                contentWidth,
                76,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Reset",
                contentX + 13,
                actionsY + 14,
                BLUE
        );

        drawUiText(
                graphics,
                "›",
                contentX + contentWidth - 11,
                actionsY + 14,
                MUTED
        );

        graphics.fill(
                contentX + 13,
                actionsY + 38,
                contentX + contentWidth - 13,
                actionsY + 39,
                DIVIDER
        );

        drawUiText(
                graphics,
                "Erase All Content and Settings",
                contentX + 13,
                actionsY + 52,
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
                    actionsY,
                    contentWidth,
                    38
            )) {
                minecraft.setScreen(
                        new IPhoneResetOptionsScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    actionsY + 38,
                    contentWidth,
                    38
            )) {
                minecraft.setScreen(
                        new IPhoneResetConfirmScreen(
                                IPhoneResetConfirmScreen.Action.ERASE_LOCAL_CONTENT
                        )
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
