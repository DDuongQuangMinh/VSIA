package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhonePrivacyClientState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhonePrivacySecurityScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int GREEN = 0xFF30D158;

    private int contentX;
    private int contentWidth;
    private int localNetworkY;

    public IPhonePrivacySecurityScreen() {
        super(Component.literal("Privacy & Security"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        localNetworkY = phoneY + 90;
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
                "Privacy & Security"
        );

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "APP PRIVACY",
                contentX + 4,
                localNetworkY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                localNetworkY,
                contentWidth,
                50,
                14,
                CARD
        );

        drawUiText(
                graphics,
                "Local Network",
                contentX + 13,
                localNetworkY + 18,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                localNetworkY + 15,
                PhonePrivacyClientState
                        .localNetworkAllowed()
        );

        drawUiWrappedCentered(
                graphics,
                "Controls whether VS:IA Web can send requests over the simulated local network.",
                phoneX + PHONE_WIDTH / 2,
                localNetworkY + 67,
                PHONE_WIDTH - 48,
                11,
                4,
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
                    localNetworkY,
                    contentWidth,
                    50
            )) {
                PhonePrivacyClientState
                        .setLocalNetworkAllowed(
                                !PhonePrivacyClientState
                                        .localNetworkAllowed()
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
