package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberService;
import com.k1ngtle.vsia.phone.subscriber.packet.C2SPhoneSubscriberActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class IPhoneEsimActivationScreen extends IPhoneScreen {
    private EditBox activationCode;
    private int contentX;
    private int contentWidth;
    private int fieldY;

    public IPhoneEsimActivationScreen() {
        super(Component.literal("Add eSIM"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 18;
        contentWidth = PHONE_WIDTH - 36;
        fieldY = phoneY + 157;

        activationCode = new EditBox(
                font,
                contentX,
                fieldY,
                contentWidth,
                26,
                uiText("Activation Code")
        );
        activationCode.setMaxLength(192);
        activationCode.setHint(uiText("LPA:1$vsia.smdp$DEV"));
        activationCode.setFormatter((value, offset) -> uiSequence(value));
        addRenderableWidget(activationCode);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF111216);
        renderStatusBar(graphics);
        renderHeader(graphics, "SIMs", "Add eSIM");

        beginPhoneClip(graphics, 68);

        drawUiCentered(
                graphics,
                "Enter Activation Code",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 92,
                0xFFFFFFFF
        );

        drawUiWrappedCentered(
                graphics,
                "The eSIM profile is downloaded over Wi-Fi from the VSIA SM-DP+ server.",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 113,
                PHONE_WIDTH - 48,
                13,
                2,
                0xFFAEAEB2
        );

        roundedRect(
                graphics,
                contentX,
                phoneY + 203,
                contentWidth,
                38,
                16,
                0xFF0A84FF
        );

        drawUiCentered(
                graphics,
                "Activate",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 217,
                0xFFFFFFFF
        );

        drawUiWrappedCentered(
                graphics,
                "DEV code: " + PhoneSubscriberService.DEV_ESIM_ACTIVATION,
                phoneX + PHONE_WIDTH / 2,
                phoneY + 261,
                PHONE_WIDTH - 48,
                12,
                2,
                0xFF6F6F73
        );

        super.render(graphics, mouseX, mouseY, partialTick);
        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneSimManagerScreen());
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    phoneY + 203,
                    contentWidth,
                    38
            )) {
                FieldDeviceNetwork.sendToServer(
                        C2SPhoneSubscriberActionPacket.activateEsim(
                                activationCode.getValue()
                        )
                );
                minecraft.setScreen(new IPhoneSimManagerScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
