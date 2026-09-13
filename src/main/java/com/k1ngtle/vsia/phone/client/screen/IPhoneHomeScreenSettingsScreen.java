package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneHomeScreenSettingsScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int searchY;

    public IPhoneHomeScreenSettingsScreen() {
        super(Component.literal("Home Screen & App Library"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        searchY = phoneY + 94;
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
                "Settings",
                "Home Screen"
        );

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "SEARCH",
                contentX + 4,
                searchY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                searchY,
                contentWidth,
                50,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Show on Home Screen",
                contentX + 13,
                searchY + 18,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                searchY + 15,
                PhoneSystemSettings.showHomeSearch()
        );

        drawUiWrappedCentered(
                graphics,
                "Controls the Search pill shown above the dock.",
                phoneX + PHONE_WIDTH / 2,
                searchY + 67,
                PHONE_WIDTH - 50,
                11,
                3,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
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
                enabled ? GREEN : 0xFF636366
        );

        roundedRect(
                graphics,
                enabled ? x + 19 : x + 3,
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
                    searchY,
                    contentWidth,
                    50
            )) {
                PhoneSystemSettings.setShowHomeSearch(
                        !PhoneSystemSettings.showHomeSearch()
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
