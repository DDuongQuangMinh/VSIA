package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.messages.PhoneMessagesClientState;
import com.k1ngtle.vsia.phone.messages.packet.C2SPhoneMessageActionPacket;
import com.k1ngtle.vsia.signality.internet.field.FieldDeviceNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class IPhoneMessageComposeScreen extends IPhoneScreen {
    private EditBox recipient;
    private EditBox message;
    private int x;
    private int w;

    public IPhoneMessageComposeScreen() {
        super(Component.literal("New Message"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 16;
        w = PHONE_WIDTH - 32;

        recipient = new EditBox(
                font,
                x,
                phoneY + 90,
                w,
                25,
                Component.literal("To")
        );
        recipient.setMaxLength(32);
        recipient.setHint(Component.literal("+99910..."));
        addRenderableWidget(recipient);

        message = new EditBox(
                font,
                x,
                phoneY + 134,
                w,
                28,
                Component.literal("Message")
        );
        message.setMaxLength(320);
        message.setHint(Component.literal("Text Message"));
        addRenderableWidget(message);

        setInitialFocus(recipient);
        PhoneMessagesClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF111216);
        graphics.fill(phoneX + 4, phoneY + 36, phoneX + PHONE_WIDTH - 4, phoneY + PHONE_HEIGHT - 20, 0xFFF2F2F7);
        renderStatusBar(graphics);
        renderHeader(graphics, "Messages", "New Message");

        graphics.drawString(font, "To:", x, phoneY + 78, 0xFF6E6E73, false);
        graphics.drawString(font, "Message", x, phoneY + 122, 0xFF6E6E73, false);

        roundedRect(
                graphics,
                x,
                phoneY + 184,
                w,
                36,
                16,
                0xFF34C759
        );
        graphics.drawCenteredString(
                font,
                "Send SMS",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 198,
                0xFFFFFFFF
        );

        String own = PhoneMessagesClientState.get().snapshot().ownNumber();
        graphics.drawCenteredString(
                font,
                own.isBlank() ? "No SIM / eSIM" : "From " + own,
                phoneX + PHONE_WIDTH / 2,
                phoneY + 238,
                0xFF8E8E93
        );
        graphics.drawCenteredString(
                font,
                "DEV: enter your own number for loopback testing",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 255,
                0xFF8E8E93
        );

        super.render(graphics, mouseX, mouseY, partialTick);
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
