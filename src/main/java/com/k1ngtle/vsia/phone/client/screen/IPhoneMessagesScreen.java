package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.messages.PhoneMessagesClientState;
import com.k1ngtle.vsia.phone.messages.PhoneMessagesSnapshot;
import com.k1ngtle.vsia.phone.messages.PhoneSmsMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class IPhoneMessagesScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int TEXT = 0xFF111111;
    private static final int MUTED = 0xFF6E6E73;
    private static final int BLUE = 0xFF007AFF;
    private static final int GREEN = 0xFF34C759;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private int contentX;
    private int contentWidth;
    private int listY;
    private int scroll;

    public IPhoneMessagesScreen() {
        super(Component.literal("Messages"));
    }

    @Override
    protected void init() {
        super.init();
        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        listY = phoneY + 85;
        PhoneMessagesClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF111216);
        graphics.fill(
                phoneX + DISPLAY_INSET,
                phoneY + 36,
                phoneX + PHONE_WIDTH - DISPLAY_INSET,
                phoneY + PHONE_HEIGHT - CONTENT_BOTTOM_INSET,
                BG
        );
        renderStatusBar(graphics);

        beginPhoneClip(graphics, 42);

        drawUiText(graphics, "Messages", phoneX + 18, phoneY + 54, TEXT);
        drawUiText(graphics, "+", phoneX + PHONE_WIDTH - 28, phoneY + 53, BLUE);

        PhoneMessagesSnapshot snapshot = PhoneMessagesClientState.get().snapshot();
        String service = snapshot.ownNumber().isBlank()
                ? "No SIM"
                : snapshot.ownNumber() + (snapshot.serviceAvailable() ? "  • SMS" : "  • No Service");
        drawUiText(
                graphics,
                fitUi(service, PHONE_WIDTH - 42),
                phoneX + 18,
                phoneY + 69,
                snapshot.serviceAvailable() ? GREEN : MUTED
        );

        List<PhoneSmsMessage> messages = snapshot.messages();
        int rowHeight = 46;
        int start = Math.max(0, Math.min(scroll, Math.max(0, messages.size() - 6)));
        int visible = Math.min(6, messages.size() - start);

        if (messages.isEmpty()) {
            drawUiCentered(graphics, "No Messages", phoneX + PHONE_WIDTH / 2, phoneY + 192, MUTED);
            drawUiWrappedCentered(
                    graphics,
                    "Tap + to start a conversation",
                    phoneX + PHONE_WIDTH / 2,
                    phoneY + 210,
                    PHONE_WIDTH - 52,
                    12,
                    2,
                    MUTED
            );
        }

        for (int i = 0; i < visible; i++) {
            PhoneSmsMessage sms = messages.get(start + i);
            int y = listY + i * rowHeight;
            roundedRect(graphics, contentX, y, contentWidth, 40, 10, 0xFFFFFFFF);

            boolean incoming = snapshot.ownNumber().equals(sms.to());
            String peer = incoming ? sms.from() : sms.to();
            String prefix = incoming ? "" : "To ";

            String time = TIME.format(
                    Instant.ofEpochMilli(sms.timestampMillis())
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
            );
            int timeWidth = uiWidth(time);
            int peerMax = Math.max(50, contentWidth - 34 - timeWidth);
            drawUiText(graphics, fitUi(prefix + peer, peerMax), contentX + 10, y + 7, TEXT);
            drawUiText(graphics, time, contentX + contentWidth - 10 - timeWidth, y + 7, MUTED);

            String state = sms.state();
            int stateWidth = uiWidth(state);
            int bodyMax = Math.max(60, contentWidth - 34 - stateWidth);
            drawUiText(graphics, fitUi(sms.body(), bodyMax), contentX + 10, y + 22, MUTED);
            drawUiText(
                    graphics,
                    state,
                    contentX + contentWidth - 10 - stateWidth,
                    y + 22,
                    "DELIVERED".equalsIgnoreCase(state) ? GREEN : 0xFFFF9F0A
            );
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int size = PhoneMessagesClientState.get().snapshot().messages().size();
        if (delta < 0) scroll = Math.min(Math.max(0, size - 6), scroll + 1);
        if (delta > 0) scroll = Math.max(0, scroll - 1);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, phoneX + PHONE_WIDTH - 44, phoneY + 42, 34, 34)) {
                minecraft.setScreen(new IPhoneMessageComposeScreen());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
