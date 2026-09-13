package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneResetService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneResetConfirmScreen extends IPhoneScreen {
    public enum Action {
        RESET_ALL_SETTINGS,
        RESET_NETWORK_SETTINGS,
        RESET_HOME_SCREEN,
        ERASE_LOCAL_CONTENT
    }

    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int RED = 0xFFFF453A;
    private static final int BLUE = 0xFF0A84FF;

    private final Action action;

    private int contentX;
    private int contentWidth;
    private int cardY;
    private int confirmY;
    private int cancelY;

    public IPhoneResetConfirmScreen(
            Action action
    ) {
        super(Component.literal("Confirm Reset"));
        this.action = action;
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 18;
        contentWidth = PHONE_WIDTH - 36;
        cardY = phoneY + 100;
        confirmY = cardY + 112;
        cancelY = confirmY + 46;
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
                action == Action.ERASE_LOCAL_CONTENT
                        ? "Transfer or Reset"
                        : "Reset",
                "Confirm"
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                cardY,
                contentWidth,
                92,
                14,
                CARD
        );

        drawUiCentered(
                graphics,
                title(),
                phoneX + PHONE_WIDTH / 2,
                cardY + 14,
                action == Action.ERASE_LOCAL_CONTENT
                        ? RED
                        : TEXT
        );

        drawUiWrappedCentered(
                graphics,
                description(),
                phoneX + PHONE_WIDTH / 2,
                cardY + 36,
                contentWidth - 24,
                11,
                5,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                confirmY,
                contentWidth,
                38,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                confirmLabel(),
                phoneX + PHONE_WIDTH / 2,
                confirmY + 14,
                RED
        );

        roundedRect(
                graphics,
                contentX,
                cancelY,
                contentWidth,
                38,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "Cancel",
                phoneX + PHONE_WIDTH / 2,
                cancelY + 14,
                BLUE
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private String title() {
        return switch (action) {
            case RESET_ALL_SETTINGS ->
                    "Reset All Settings?";

            case RESET_NETWORK_SETTINGS ->
                    "Reset Network Settings?";

            case RESET_HOME_SCREEN ->
                    "Reset Home Screen Layout?";

            case ERASE_LOCAL_CONTENT ->
                    "Erase This VS:IA Phone?";
        };
    }

    private String confirmLabel() {
        return switch (action) {
            case RESET_ALL_SETTINGS ->
                    "Reset All Settings";

            case RESET_NETWORK_SETTINGS ->
                    "Reset Network Settings";

            case RESET_HOME_SCREEN ->
                    "Reset Home Screen";

            case ERASE_LOCAL_CONTENT ->
                    "Erase Local Phone Data";
        };
    }

    private String description() {
        return switch (action) {
            case RESET_ALL_SETTINGS ->
                    "Returns simulated phone settings to defaults without deleting server-side messages or SIM service.";

            case RESET_NETWORK_SETTINGS ->
                    "Clears remembered Wi-Fi passwords and restores local radio preferences.";

            case RESET_HOME_SCREEN ->
                    "Restores the Home Screen wallpaper and Search visibility defaults.";

            case ERASE_LOCAL_CONTENT ->
                    "Clears local settings, notification history, stopwatch state, and remembered Wi-Fi data. Server-side SMS and SIM data remain.";
        };
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
                goBack();
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    confirmY,
                    contentWidth,
                    38
            )) {
                execute();
                minecraft.setScreen(
                        new IPhoneSettingsScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    cancelY,
                    contentWidth,
                    38
            )) {
                goBack();
                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private void execute() {
        switch (action) {
            case RESET_ALL_SETTINGS ->
                    PhoneResetService.resetAllSettings();

            case RESET_NETWORK_SETTINGS ->
                    PhoneResetService.resetNetworkSettings();

            case RESET_HOME_SCREEN ->
                    PhoneResetService.resetHomeScreenLayout();

            case ERASE_LOCAL_CONTENT ->
                    PhoneResetService.eraseLocalContentAndSettings();
        }
    }

    private void goBack() {
        minecraft.setScreen(
                action == Action.ERASE_LOCAL_CONTENT
                        ? new IPhoneTransferResetScreen()
                        : new IPhoneResetOptionsScreen()
        );
    }
}
