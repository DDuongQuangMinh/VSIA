package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import com.k1ngtle.vsia.phone.client.PhoneText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class IPhoneMailComposeScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int BLUE = 0xFF007AFF;

    private String to = "";
    private String subject = "";
    private String body = "";
    private int field;

    private int x;
    private int w;
    private int topY;
    private int bodyY;
    private int sendY;

    public IPhoneMailComposeScreen() {
        super(Component.literal("New Message"));
    }

    @Override
    protected void init() {
        super.init();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        topY = phoneY + 82;
        bodyY = topY + 82;
        sendY = phoneY + PHONE_HEIGHT - 70;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        renderHeader(g, "Mail", "New Message");
        beginPhoneClip(g, 68);

        roundedRect(g, x, topY, w, 72, 12, CARD);
        drawUiText(g, "To:", x + 10, topY + 13, MUTED);
        drawUiText(g, fitUi(to + (field == 0 ? "|" : ""), w - 52), x + 40, topY + 13, TEXT);
        g.fill(x + 10, topY + 35, x + w - 10, topY + 36, 0xFFE2E2E6);
        drawUiText(g, "Subject:", x + 10, topY + 49, MUTED);
        drawUiText(g, fitUi(subject + (field == 1 ? "|" : ""), w - 72), x + 61, topY + 49, TEXT);

        roundedRect(g, x, bodyY, w, 160, 12, CARD);
        if (body.isEmpty()) {
            drawUiText(g, "Message", x + 10, bodyY + 11, MUTED);
        } else {
            List<FormattedCharSequence> lines = PhoneText.split(font, body + (field == 2 ? "|" : ""), w - 20);

            for (int i = 0; i < lines.size() && i < 11; i++) {
                g.drawString(font, lines.get(i), x + 10, bodyY + 11 + i * 13, TEXT, false);
            }
        }

        roundedRect(g, x, sendY, w, 34, 12, 0xFFFFFFFF);
        drawUiCentered(g, "Send", phoneX + PHONE_WIDTH / 2, sendY + 12, BLUE);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (clickedBack(mouseX, mouseY)) {
                minecraft.setScreen(new IPhoneMailScreen());
                return true;
            }

            if (inside(mouseX, mouseY, x, topY, w, 36)) {
                field = 0;
                return true;
            }

            if (inside(mouseX, mouseY, x, topY + 36, w, 36)) {
                field = 1;
                return true;
            }

            if (inside(mouseX, mouseY, x, bodyY, w, 160)) {
                field = 2;
                return true;
            }

            if (inside(mouseX, mouseY, x, sendY, w, 34)) {
                PhoneCoreAppsState.sendMail(to, subject, body);
                minecraft.setScreen(new IPhoneMailScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }

        switch (field) {
            case 0 -> {
                if (to.length() < 80) to += codePoint;
            }
            case 1 -> {
                if (subject.length() < 100) subject += codePoint;
            }
            default -> {
                if (body.length() < 1600) body += codePoint;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259) {
            switch (field) {
                case 0 -> to = trimLast(to);
                case 1 -> subject = trimLast(subject);
                default -> body = trimLast(body);
            }
            return true;
        }

        if ((keyCode == 257 || keyCode == 335) && field == 2) {
            body += "\n";
            return true;
        }

        if (keyCode == 258) {
            field = (field + 1) % 3;
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static String trimLast(String value) {
        return value.isEmpty() ? value : value.substring(0, value.length() - 1);
    }
}
