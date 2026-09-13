package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.messages.PhoneMessagesClientState;
import com.k1ngtle.vsia.phone.messages.packet.C2SPhoneMessageActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class IPhoneMessageComposeScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int TEXT = 0xFF111111;
    private static final int MUTED = 0xFF6E6E73;
    private static final int GREEN = 0xFF34C759;

    private EditBox recipient;
    private EditBox message;
    private int x;
    private int w;
    private int bodyLeft;
    private int bodyRight;

    public IPhoneMessageComposeScreen() {
        super(Component.literal("New Message"));
    }

    @Override
    protected void init() {
        super.init();
        bodyLeft = phoneX + DISPLAY_INSET;
        bodyRight = phoneX + PHONE_WIDTH - DISPLAY_INSET;
        x = phoneX + 18;
        w = PHONE_WIDTH - 36;

        recipient = new EditBox(
                font,
                x,
                phoneY + 90,
                w,
                25,
                uiText("To")
        );
        recipient.setMaxLength(32);
        recipient.setHint(uiText("+99910..."));
        recipient.setFormatter((value, offset) -> uiSequence(value));
        addRenderableWidget(recipient);

        message = new EditBox(
                font,
                x,
                phoneY + 134,
                w,
                28,
                uiText("Message")
        );
        message.setMaxLength(320);
        message.setHint(uiText("Text Message"));
        message.setFormatter((value, offset) -> uiSequence(value));
        addRenderableWidget(message);

        setInitialFocus(recipient);
        PhoneMessagesClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF111216);
        graphics.fill(
                bodyLeft,
                phoneY + 36,
                bodyRight,
                phoneY + PHONE_HEIGHT - CONTENT_BOTTOM_INSET,
                BG
        );
        renderStatusBar(graphics);
        renderHeader(graphics, "Messages", "New Message");

        beginPhoneClip(graphics, 68);

        drawUiText(graphics, "To:", x, phoneY + 78, MUTED);
        drawUiText(graphics, "Message", x, phoneY + 122, MUTED);

        roundedRect(
                graphics,
                x,
                phoneY + 184,
                w,
                36,
                16,
                GREEN
        );
        drawUiCentered(
                graphics,
                "Send SMS",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 197,
                0xFFFFFFFF
        );

        String own = PhoneMessagesClientState.get().snapshot().ownNumber();
        drawUiCentered(
                graphics,
                own.isBlank() ? "No SIM / eSIM" : "From " + own,
                phoneX + PHONE_WIDTH / 2,
                phoneY + 238,
                MUTED
        );

        drawUiWrappedCentered(
                graphics,
                "DEV: enter your own number for loopback testing",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 255,
                PHONE_WIDTH - 52,
                12,
                2,
                MUTED
        );

        super.render(graphics, mouseX, mouseY, partialTick);
        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneMessagesScreen());
                return true;
            }

            if (inside(mouseX, mouseY, x, phoneY + 184, w, 36)) {
                FieldDeviceNetwork.sendToServer(
                        C2SPhoneMessageActionPacket.send(
                                recipient.getValue(),
                                message.getValue()
                        )
                );
                minecraft.setScreen(new IPhoneMessagesScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
