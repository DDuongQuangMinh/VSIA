package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneSoftwareUpdateState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class IPhoneSoftwareUpdateScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int GREEN = 0xFF30D158;
    private static final int DIVIDER = 0xFF3A3A3C;

    private int contentX;
    private int contentWidth;
    private int statusY;
    private int automaticY;
    private int actionY;

    public IPhoneSoftwareUpdateScreen() {
        super(Component.literal("Software Update"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        statusY = phoneY + 82;
        automaticY = statusY + 118;
        actionY = automaticY + 98;

        PhoneSoftwareUpdateState.checkForUpdate();
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        PhoneSoftwareUpdateState.tick();

        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);
        renderHeader(graphics, "General", "Software Update");

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                statusY,
                contentWidth,
                98,
                14,
                CARD
        );

        drawUiCentered(
                graphics,
                "PhoneOS " + PhoneSoftwareUpdateState.currentVersion(),
                phoneX + PHONE_WIDTH / 2,
                statusY + 14,
                TEXT
        );

        drawUiWrappedCentered(
                graphics,
                statusMessage(),
                phoneX + PHONE_WIDTH / 2,
                statusY + 36,
                contentWidth - 26,
                12,
                4,
                statusColor()
        );

        drawUiText(
                graphics,
                "AUTOMATIC UPDATES",
                contentX + 4,
                automaticY - 16,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                automaticY,
                contentWidth,
                76,
                14,
                CARD
        );

        toggleRow(
                graphics,
                automaticY,
                "Automatically Download",
                PhoneSoftwareUpdateState.automaticallyDownload()
        );

        graphics.fill(
                contentX + 13,
                automaticY + 38,
                contentX + contentWidth - 13,
                automaticY + 39,
                DIVIDER
        );

        toggleRow(
                graphics,
                automaticY + 38,
                "Automatically Install",
                PhoneSoftwareUpdateState.automaticallyInstall()
        );

        roundedRect(
                graphics,
                contentX,
                actionY,
                contentWidth,
                38,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                actionLabel(),
                phoneX + PHONE_WIDTH / 2,
                actionY + 14,
                BLUE
        );

        drawUiWrappedCentered(
                graphics,
                "This updates the simulated VS:IA PhoneOS state; it does not replace your Minecraft mod files.",
                phoneX + PHONE_WIDTH / 2,
                actionY + 53,
                PHONE_WIDTH - 48,
                11,
                4,
                MUTED
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    private String statusMessage() {
        return switch (PhoneSoftwareUpdateState.state()) {
            case IDLE ->
                    "Tap Check for Update.";
            case CHECKING ->
                    "Checking for Update...";
            case AVAILABLE ->
                    "PhoneOS "
                            + PhoneSoftwareUpdateState.latestVersion()
                            + " is available.";
            case UP_TO_DATE ->
                    "PhoneOS is up to date.";
            case DOWNLOADING ->
                    "Downloading PhoneOS "
                            + PhoneSoftwareUpdateState.latestVersion()
                            + "...";
            case READY_TO_INSTALL ->
                    "Update downloaded and ready to install.";
            case INSTALLING ->
                    "Installing update...";
            case ERROR ->
                    PhoneSoftwareUpdateState.errorMessage();
        };
    }

    private int statusColor() {
        return switch (PhoneSoftwareUpdateState.state()) {
            case AVAILABLE, READY_TO_INSTALL ->
                    BLUE;
            case UP_TO_DATE ->
                    GREEN;
            case ERROR ->
                    0xFFFF453A;
            default ->
                    MUTED;
        };
    }

    private String actionLabel() {
        return switch (PhoneSoftwareUpdateState.state()) {
            case AVAILABLE ->
                    "Download Update";
            case READY_TO_INSTALL ->
                    "Install Now";
            case DOWNLOADING ->
                    "Downloading...";
            case INSTALLING ->
                    "Installing...";
            default ->
                    "Check for Update";
        };
    }

    private void toggleRow(
            GuiGraphics graphics,
            int y,
            String label,
            boolean enabled
    ) {
        drawUiText(
                graphics,
                fitUi(label, contentWidth - 64),
                contentX + 13,
                y + 14,
                TEXT
        );

        drawToggle(
                graphics,
                contentX + contentWidth - 47,
                y + 9,
                enabled
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
                        new IPhoneGeneralScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    automaticY,
                    contentWidth,
                    38
            )) {
                PhoneSoftwareUpdateState.setAutomaticallyDownload(
                        !PhoneSoftwareUpdateState.automaticallyDownload()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    automaticY + 38,
                    contentWidth,
                    38
            )) {
                PhoneSoftwareUpdateState.setAutomaticallyInstall(
                        !PhoneSoftwareUpdateState.automaticallyInstall()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    actionY,
                    contentWidth,
                    38
            )) {
                switch (PhoneSoftwareUpdateState.state()) {
                    case AVAILABLE ->
                            PhoneSoftwareUpdateState.downloadUpdate();

                    case READY_TO_INSTALL ->
                            PhoneSoftwareUpdateState.installUpdate();

                    case DOWNLOADING, INSTALLING -> {
                    }

                    default ->
                            PhoneSoftwareUpdateState.checkForUpdate();
                }

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
