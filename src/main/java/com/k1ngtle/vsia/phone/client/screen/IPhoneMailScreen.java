package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCoreAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IPhoneMailScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int BLUE = 0xFF007AFF;
    private static final int ROW = 52;

    private boolean sentView;
    private int x;
    private int w;
    private int tabsY;
    private int listY;
    private int composeY;

    public IPhoneMailScreen() {
        super(Component.literal("Mail"));
    }

    @Override
    protected void init() {
        super.init();
        PhoneCoreAppsState.ensureLoaded();
        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        tabsY = phoneY + 79;
        listY = tabsY + 38;
        composeY = phoneY + PHONE_HEIGHT - 70;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);
        drawUiCentered(g, "Mail", phoneX + PHONE_WIDTH / 2, phoneY + 51, TEXT);
        beginPhoneClip(g, 68);

        roundedRect(g, x, tabsY, w, 30, 12, 0xFFE3E3E8);
        roundedRect(g, sentView ? x + w / 2 : x, tabsY, w / 2, 30, 12, 0xFFFFFFFF);
        drawUiCentered(g, "Inbox", x + w / 4, tabsY + 10, sentView ? MUTED : BLUE);
        drawUiCentered(g, "Sent", x + w * 3 / 4, tabsY + 10, sentView ? BLUE : MUTED);

        List<PhoneCoreAppsState.MailMessage> mail = sentView
                ? PhoneCoreAppsState.sent()
                : PhoneCoreAppsState.inbox();

        if (mail.isEmpty()) {
            drawUiCentered(g, sentView ? "No Sent Mail" : "Inbox is Empty", phoneX + PHONE_WIDTH / 2, listY + 38, MUTED);
        } else {
            int visible = Math.min(5, mail.size());
            roundedRect(g, x, listY, w, visible * ROW, 14, CARD);

            for (int i = 0; i < visible; i++) {
                PhoneCoreAppsState.MailMessage message = mail.get(i);
                int y = listY + i * ROW;
                drawUiText(g, fitUi(sentView ? message.to() : message.from(), w - 28), x + 12, y + 8, TEXT);
                drawUiText(g, fitUi(message.subject(), w - 28), x + 12, y + 23, 0xFF3A3A3C);
                drawUiText(g, fitUi(message.body().replace('\n', ' '), w - 28), x + 12, y + 37, MUTED);

                if (i + 1 < visible) {
                    g.fill(x + 12, y + ROW - 1, x + w - 12, y + ROW, 0xFFE2E2E6);
                }
            }
        }

        roundedRect(g, x, composeY, w, 34, 12, 0xFFFFFFFF);
        drawUiCentered(g, "Compose", phoneX + PHONE_WIDTH / 2, composeY + 12, BLUE);

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x, tabsY, w / 2, 30)) {
                sentView = false;
                return true;
            }

            if (inside(mouseX, mouseY, x + w / 2, tabsY, w / 2, 30)) {
                sentView = true;
                return true;
            }

            if (inside(mouseX, mouseY, x, composeY, w, 34)) {
                minecraft.setScreen(new IPhoneMailComposeScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
