package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.widget.PhonePasswordField;
import com.k1ngtle.vsia.phone.network.PhoneNetworkController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class IPhoneWifiPasswordScreen
        extends IPhoneScreen {

    private final String ssid;
    private final String bssid;
    private final String security;

    private PhonePasswordField passwordField;

    private int contentX;
    private int contentWidth;
    private int fieldY;
    private int joinY;
    private int showY;

    public IPhoneWifiPasswordScreen(
            String ssid,
            String bssid,
            String security
    ) {
        super(
                Component.literal(
                        "Wi-Fi Password"
                )
        );

        this.ssid =
                ssid == null
                        ? ""
                        : ssid;

        this.bssid =
                bssid == null
                        ? ""
                        : bssid;

        this.security =
                security == null
                        ? ""
                        : security;
    }

    @Override
    protected void init() {
        super.init();

        contentX =
                phoneX + 18;

        contentWidth =
                PHONE_WIDTH - 36;

        fieldY =
                phoneY + 174;

        showY =
                fieldY + 46;

        joinY =
                showY + 52;

        passwordField =
                new PhonePasswordField(
                        font,
                        contentX + 10,
                        fieldY + 8,
                        contentWidth - 20,
                        24
                );

        passwordField.setMaxLength(
                63
        );

        passwordField.setPlaceholder(
                "Password"
        );

        passwordField.setFocused(
                true
        );

        setFocused(
                passwordField
        );

        addRenderableWidget(
                passwordField
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                0xFF1C1C1E
        );

        renderStatusBar(
                graphics
        );

        renderHeader(
                graphics,
                "Wi-Fi",
                "Password"
        );

        graphics.drawCenteredString(
                font,
                "Enter Password",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                phoneY + 91,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                fit(
                        ssid,
                        PHONE_WIDTH - 50
                ),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                phoneY + 116,
                0xFF0A84FF
        );

        graphics.drawCenteredString(
                font,
                fit(
                        securityLabel(),
                        PHONE_WIDTH - 50
                ),
                phoneX
                        + PHONE_WIDTH
                        / 2,
                phoneY + 136,
                0xFF8E8E93
        );

        roundedRect(
                graphics,
                contentX,
                fieldY,
                contentWidth,
                40,
                12,
                0xFF2C2C2E
        );

        roundedRect(
                graphics,
                contentX,
                showY,
                contentWidth,
                40,
                12,
                0xFF2C2C2E
        );

        graphics.drawString(
                font,
                "Show Password",
                contentX + 12,
                showY + 15,
                0xFFFFFFFF,
                false
        );

        drawSmallToggle(
                graphics,
                contentX
                        + contentWidth
                        - 43,
                showY + 11,
                passwordField != null
                        && passwordField.isReveal()
        );

        boolean canJoin =
                passwordField != null;

        roundedRect(
                graphics,
                contentX,
                joinY,
                contentWidth,
                42,
                12,
                canJoin
                        ? 0xFF0A84FF
                        : 0xFF3A3A3C
        );

        graphics.drawCenteredString(
                font,
                "Join",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                joinY + 16,
                canJoin
                        ? 0xFFFFFFFF
                        : 0xFF8E8E93
        );

        graphics.drawCenteredString(
                font,
                "Password is checked by the server AP",
                phoneX
                        + PHONE_WIDTH
                        / 2,
                joinY + 62,
                0xFF636366
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        renderHomeIndicator(
                graphics
        );
    }

    private void drawSmallToggle(
            GuiGraphics graphics,
            int x,
            int y,
            boolean enabled
    ) {
        roundedRect(
                graphics,
                x,
                y,
                31,
                18,
                9,
                enabled
                        ? 0xFF30D158
                        : 0xFF636366
        );

        roundedRect(
                graphics,
                enabled
                        ? x + 16
                        : x + 3,
                y + 3,
                12,
                12,
                6,
                0xFFFFFFFF
        );
    }

    private String securityLabel() {
        String value =
                security
                        .replace(
                                "signality:",
                                ""
                        )
                        .replace(
                                '_',
                                ' '
                        );

        if (value.isBlank()) {
            return "Protected Network";
        }

        return value.toUpperCase();
    }

    private String fit(
            String value,
            int maxWidth
    ) {
        String text =
                value == null
                        ? ""
                        : value;

        if (font.width(
                text
        ) <= maxWidth) {
            return text;
        }

        while (!text.isEmpty()
                && font.width(
                text + "..."
        ) > maxWidth) {
            text =
                    text.substring(
                            0,
                            text.length() - 1
                    );
        }

        return text + "...";
    }

    private void submit() {
        if (passwordField == null) {
            return;
        }

        PhoneNetworkController
                .get()
                .connectWifi(
                        bssid,
                        passwordField
                                .getValue()
                );

        minecraft.setScreen(
                new IPhoneWifiScreen()
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
                        new IPhoneWifiScreen()
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    showY,
                    contentWidth,
                    40
            )) {
                if (passwordField != null) {
                    passwordField.setReveal(
                            !passwordField.isReveal()
                    );
                }

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    joinY,
                    contentWidth,
                    42
            )) {
                submit();

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode
                == GLFW.GLFW_KEY_ENTER
                || keyCode
                == GLFW.GLFW_KEY_KP_ENTER) {
            submit();

            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }
}
