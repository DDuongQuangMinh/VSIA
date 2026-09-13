package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneResetOptionsScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int RED = 0xFFFF453A;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 40;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneResetOptionsScreen() {
        super(Component.literal("Reset"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 88;
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
                "Transfer or Reset",
                "Reset"
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                ROW_HEIGHT * 3,
                14,
                CARD
        );

        row(
                graphics,
                groupY,
                "Reset All Settings"
        );
        divider(graphics, groupY + ROW_HEIGHT);

        row(
                graphics,
                groupY + ROW_HEIGHT,
                "Reset Network Settings"
        );
        divider(graphics, groupY + ROW_HEIGHT * 2);

        row(
                graphics,
                groupY + ROW_HEIGHT * 2,
                "Reset Home Screen Layout"
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private void row(
            GuiGraphics graphics,
            int y,
            String label
    ) {
        drawUiText(
                graphics,
                fitUi(label, contentWidth - 26),
                contentX + 13,
                y + 15,
                RED
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
                        new IPhoneTransferResetScreen()
                );
                return true;
            }

            if (!inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY,
                    contentWidth,
                    ROW_HEIGHT * 3
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

            IPhoneResetConfirmScreen.Action action =
                    switch (row) {
                        case 0 ->
                                IPhoneResetConfirmScreen.Action.RESET_ALL_SETTINGS;

                        case 1 ->
                                IPhoneResetConfirmScreen.Action.RESET_NETWORK_SETTINGS;

                        case 2 ->
                                IPhoneResetConfirmScreen.Action.RESET_HOME_SCREEN;

                        default ->
                                null;
                    };

            if (action != null) {
                minecraft.setScreen(
                        new IPhoneResetConfirmScreen(
                                action
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
