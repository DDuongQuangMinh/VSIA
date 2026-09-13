package com.k1ngtle.vsia.phone.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneAboutScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int DIVIDER = 0xFF3A3A3C;

    private int contentX;
    private int contentWidth;
    private int groupY;

    public IPhoneAboutScreen() {
        super(Component.literal("About"));
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
        renderHeader(graphics, "General", "About");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                38 * 5,
                14,
                CARD
        );

        pair(graphics, groupY, "Name", "VS:IA iPhone");
        divider(graphics, groupY + 38);

        pair(
                graphics,
                groupY + 38,
                "PhoneOS Version",
                "26.0"
        );
        divider(graphics, groupY + 76);

        pair(
                graphics,
                groupY + 76,
                "Model Name",
                "VSIA Phone"
        );
        divider(graphics, groupY + 114);

        pair(
                graphics,
                groupY + 114,
                "Model Number",
                "VSIA-A26"
        );
        divider(graphics, groupY + 152);

        pair(
                graphics,
                groupY + 152,
                "Serial Number",
                "MINECRAFT-VSIA"
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

        int available = Math.max(
                54,
                contentWidth
                        - 38
                        - uiWidth(left)
        );

        String shown = fitUi(
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

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0
                && clickedBack(
                mouseX,
                mouseY
        )) {
            minecraft.setScreen(
                    new IPhoneGeneralScreen()
            );
            return true;
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
