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
        contentX = phoneX + 12;
        contentWidth = PHONE_WIDTH - 24;
        listY = phoneY + 85;
        PhoneMessagesClientState.get().requestRefresh();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(graphics, 0xFF111216);
        graphics.fill(phoneX + 4, phoneY + 36, phoneX + PHONE_WIDTH - 4, phoneY + PHONE_HEIGHT - 20, BG);
        renderStatusBar(graphics);

        graphics.drawString(font, "Messages", phoneX + 18, phoneY + 54, TEXT, false);
        graphics.drawString(font, "+", phoneX + PHONE_WIDTH - 28, phoneY + 53, BLUE, false);

        PhoneMessagesSnapshot snapshot = PhoneMessagesClientState.get().snapshot();
        String service = snapshot.ownNumber().isBlank()
                ? "No SIM"
                : snapshot.ownNumber() + (snapshot.serviceAvailable() ? "  • SMS" : "  • No Service");
        graphics.drawString(font, fit(service, PHONE_WIDTH - 36), phoneX + 18, phoneY + 69,
                snapshot.serviceAvailable() ? GREEN : MUTED, false);

        List<PhoneSmsMessage> messages = snapshot.messages();
        int rowHeight = 46;
        int start = Math.max(0, Math.min(scroll, Math.max(0, messages.size() - 6)));
        int visible = Math.min(6, messages.size() - start);

        if (messages.isEmpty()) {
            graphics.drawCenteredString(font, "No Messages", phoneX + PHONE_WIDTH / 2, phoneY + 192, MUTED);
            graphics.drawCenteredString(font, "Tap + to start a conversation", phoneX + PHONE_WIDTH / 2, phoneY + 210, MUTED);
        }

        for (int i = 0; i < visible; i++) {
            PhoneSmsMessage message = messages.get(start + i);
            int y = listY + i * rowHeight;
            roundedRect(graphics, contentX, y, contentWidth, 40, 10, 0xFFFFFFFF);

            boolean incoming = snapshot.ownNumber().equals(message.to());
            String peer = incoming ? message.from() : message.to();
            String prefix = incoming ? "" : "To ";
            graphics.drawString(font, prefix + fit(peer, 115), contentX + 10, y + 7, TEXT, false);

            String time = TIME.format(
                    Instant.ofEpochMilli(message.timestampMillis())
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
            );
            graphics.drawString(font, time, contentX + contentWidth - 10 - font.width(time), y + 7, MUTED, false);

            graphics.drawString(font, fit(message.body(), 165), contentX + 10, y + 22, MUTED, false);
            String state = message.state();
            graphics.drawString(font, state, contentX + contentWidth - 10 - font.width(state), y + 22,
                    "DELIVERED".equalsIgnoreCase(state) ? GREEN : 0xFFFF9F0A, false);
        }

        renderHomeIndicator(graphics);
    }

    private String fit(String value, int maxWidth) {
        String text = value == null ? "" : value;
        if (font.width(text) <= maxWidth) return text;
        while (!text.isEmpty() && font.width(text + "...") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "...";
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
