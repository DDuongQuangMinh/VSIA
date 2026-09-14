package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCallState;
import com.k1ngtle.vsia.phone.messages.PhoneMessagesClientState;
import com.k1ngtle.vsia.phone.messages.PhoneMessagesSnapshot;
import com.k1ngtle.vsia.phone.messages.PhoneSmsMessage;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberClientState;
import com.k1ngtle.vsia.phone.subscriber.PhoneSubscriberSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class IPhonePhoneScreen extends IPhoneScreen {
    private static final int BG = 0xFFF2F2F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF151515;
    private static final int MUTED = 0xFF77777C;
    private static final int GREEN = 0xFF30D158;
    private static final int BLUE = 0xFF007AFF;
    private static final int RED = 0xFFFF3B30;

    private static final int ROW = 46;

    private int x;
    private int w;
    private int tabsY;
    private int listY;
    private int keypadY;
    private boolean keypad;
    private String typedNumber = "";

    public IPhonePhoneScreen() {
        super(Component.literal("Phone"));
    }

    @Override
    protected void init() {
        super.init();

        x = phoneX + 14;
        w = PHONE_WIDTH - 28;
        tabsY = phoneY + 78;
        listY = tabsY + 42;
        keypadY = listY + 28;

        PhoneMessagesClientState.get()
                .requestRefresh();

        PhoneSubscriberClientState.get()
                .requestRefresh();
    }

    @Override
    public void render(
            GuiGraphics g,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(g, BG);
        renderStatusBar(g);

        drawUiCentered(
                g,
                "Phone",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(g, 68);

        renderTabs(g);

        if (PhoneCallState.active()) {
            renderActiveCall(g);
        } else if (keypad) {
            renderKeypad(g);
        } else {
            renderContacts(g);
        }

        endPhoneClip(g);
        renderHomeIndicator(g);
    }

    private void renderTabs(GuiGraphics g) {
        roundedRect(
                g,
                x,
                tabsY,
                w,
                31,
                11,
                0xFFE3E3E8
        );

        roundedRect(
                g,
                keypad
                        ? x + w / 2
                        : x,
                tabsY,
                w / 2,
                31,
                11,
                CARD
        );

        drawUiCentered(
                g,
                "Contacts",
                x + w / 4,
                tabsY + 11,
                keypad
                        ? MUTED
                        : BLUE
        );

        drawUiCentered(
                g,
                "Keypad",
                x + w * 3 / 4,
                tabsY + 11,
                keypad
                        ? BLUE
                        : MUTED
        );
    }

    private void renderContacts(GuiGraphics g) {
        PhoneMessagesSnapshot messageSnapshot =
                PhoneMessagesClientState.get()
                        .snapshot();

        PhoneSubscriberSnapshot subscriber =
                PhoneSubscriberClientState.get()
                        .snapshot();

        String ownNumber =
                !messageSnapshot.ownNumber().isBlank()
                        ? messageSnapshot.ownNumber()
                        : subscriber.msisdn();

        drawUiText(
                g,
                "MY NUMBER",
                x + 4,
                listY - 18,
                MUTED
        );

        roundedRect(
                g,
                x,
                listY,
                w,
                40,
                12,
                CARD
        );

        drawUiText(
                g,
                ownNumber.isBlank()
                        ? "No SIM Number"
                        : ownNumber,
                x + 12,
                listY + 14,
                ownNumber.isBlank()
                        ? MUTED
                        : TEXT
        );

        drawUiText(
                g,
                subscriber.carrier().isBlank()
                        ? ""
                        : subscriber.carrier(),
                x + w - 12 - uiWidth(
                        subscriber.carrier()
                ),
                listY + 14,
                MUTED
        );

        List<String> contacts =
                contactNumbers(
                        messageSnapshot,
                        ownNumber
                );

        int contactsY =
                listY + 67;

        drawUiText(
                g,
                "CONTACT NUMBERS",
                x + 4,
                contactsY - 18,
                MUTED
        );

        if (contacts.isEmpty()) {
            roundedRect(
                    g,
                    x,
                    contactsY,
                    w,
                    64,
                    12,
                    CARD
            );

            drawUiCentered(
                    g,
                    "No contacts yet",
                    phoneX + PHONE_WIDTH / 2,
                    contactsY + 20,
                    MUTED
            );

            drawUiCentered(
                    g,
                    "Numbers from Messages appear here",
                    phoneX + PHONE_WIDTH / 2,
                    contactsY + 37,
                    MUTED
            );

            return;
        }

        int visible =
                Math.min(
                        6,
                        contacts.size()
                );

        roundedRect(
                g,
                x,
                contactsY,
                w,
                visible * ROW,
                14,
                CARD
        );

        for (int i = 0;
             i < visible;
             i++) {
            int y =
                    contactsY + i * ROW;

            String number =
                    contacts.get(i);

            roundedRect(
                    g,
                    x + 10,
                    y + 9,
                    28,
                    28,
                    14,
                    GREEN
            );

            drawPhoneGlyph(
                    g,
                    x + 24,
                    y + 23,
                    0xFFFFFFFF
            );

            drawUiText(
                    g,
                    fitUi(
                            number,
                            w - 80
                    ),
                    x + 48,
                    y + 17,
                    TEXT
            );

            drawUiText(
                    g,
                    "Call",
                    x + w - 34,
                    y + 17,
                    GREEN
            );

            if (i + 1
                    < visible) {
                g.fill(
                        x + 48,
                        y + ROW - 1,
                        x + w - 12,
                        y + ROW,
                        0xFFE5E5EA
                );
            }
        }
    }

    private void renderKeypad(GuiGraphics g) {
        drawUiCentered(
                g,
                typedNumber.isBlank()
                        ? "Enter Number"
                        : fitUi(
                        typedNumber,
                        w - 20
                ),
                phoneX + PHONE_WIDTH / 2,
                keypadY,
                typedNumber.isBlank()
                        ? MUTED
                        : TEXT
        );

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"*", "0", "#"}
        };

        int keySize = 44;
        int gap = 15;
        int total =
                keySize * 3
                        + gap * 2;

        int startX =
                phoneX
                        + (PHONE_WIDTH - total)
                        / 2;

        int startY =
                keypadY + 34;

        for (int row = 0;
             row < 4;
             row++) {
            for (int column = 0;
                 column < 3;
                 column++) {
                int keyX =
                        startX
                                + column * (keySize + gap);

                int keyY =
                        startY
                                + row * (keySize + 9);

                roundedRect(
                        g,
                        keyX,
                        keyY,
                        keySize,
                        keySize,
                        keySize / 2,
                        0xFFE3E3E8
                );

                drawUiCentered(
                        g,
                        keys[row][column],
                        keyX + keySize / 2,
                        keyY + 16,
                        TEXT
                );
            }
        }

        int callY =
                startY + 4 * (keySize + 9);

        roundedRect(
                g,
                phoneX + PHONE_WIDTH / 2 - 25,
                callY,
                50,
                36,
                18,
                GREEN
        );

        drawUiCentered(
                g,
                "Call",
                phoneX + PHONE_WIDTH / 2,
                callY + 13,
                0xFFFFFFFF
        );

        drawUiText(
                g,
                "⌫",
                phoneX + PHONE_WIDTH / 2 + 52,
                callY + 13,
                MUTED
        );
    }

    private void renderActiveCall(GuiGraphics g) {
        roundedRect(
                g,
                x,
                listY,
                w,
                205,
                18,
                0xFF1C1C1E
        );

        roundedRect(
                g,
                phoneX + PHONE_WIDTH / 2 - 31,
                listY + 19,
                62,
                62,
                31,
                GREEN
        );

        drawPhoneGlyph(
                g,
                phoneX + PHONE_WIDTH / 2,
                listY + 50,
                0xFFFFFFFF
        );

        drawUiCentered(
                g,
                fitUi(
                        PhoneCallState.number(),
                        w - 24
                ),
                phoneX + PHONE_WIDTH / 2,
                listY + 98,
                0xFFFFFFFF
        );

        drawUiCentered(
                g,
                PhoneCallState.status(),
                phoneX + PHONE_WIDTH / 2,
                listY + 119,
                PhoneCallState.status().equals(
                        "No Service"
                )
                        ? RED
                        : 0xFFAEAEB2
        );

        drawUiCentered(
                g,
                formatDuration(
                        PhoneCallState.elapsedSeconds()
                ),
                phoneX + PHONE_WIDTH / 2,
                listY + 140,
                0xFFAEAEB2
        );

        roundedRect(
                g,
                phoneX + PHONE_WIDTH / 2 - 36,
                listY + 159,
                72,
                34,
                17,
                RED
        );

        drawUiCentered(
                g,
                "End",
                phoneX + PHONE_WIDTH / 2,
                listY + 171,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button != 0) {
            return super.mouseClicked(
                    mouseX,
                    mouseY,
                    button
            );
        }

        if (PhoneCallState.active()) {
            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + PHONE_WIDTH / 2 - 36,
                    listY + 159,
                    72,
                    34
            )) {
                PhoneCallState.end();
                return true;
            }

            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                x,
                tabsY,
                w / 2,
                31
        )) {
            keypad = false;
            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                x + w / 2,
                tabsY,
                w / 2,
                31
        )) {
            keypad = true;
            return true;
        }

        if (keypad) {
            return handleKeypadClick(
                    mouseX,
                    mouseY
            );
        }

        PhoneMessagesSnapshot snapshot =
                PhoneMessagesClientState.get()
                        .snapshot();

        String own =
                !snapshot.ownNumber().isBlank()
                        ? snapshot.ownNumber()
                        : PhoneSubscriberClientState.get()
                        .snapshot()
                        .msisdn();

        List<String> contacts =
                contactNumbers(
                        snapshot,
                        own
                );

        int contactsY =
                listY + 67;

        int visible =
                Math.min(
                        6,
                        contacts.size()
                );

        if (inside(
                mouseX,
                mouseY,
                x,
                contactsY,
                w,
                visible * ROW
        )) {
            int index =
                    ((int) mouseY - contactsY)
                            / ROW;

            if (index >= 0
                    && index < visible) {
                PhoneCallState.start(
                        contacts.get(index)
                );

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private boolean handleKeypadClick(
            double mouseX,
            double mouseY
    ) {
        int keySize = 44;
        int gap = 15;
        int total =
                keySize * 3
                        + gap * 2;

        int startX =
                phoneX
                        + (PHONE_WIDTH - total)
                        / 2;

        int startY =
                keypadY + 34;

        String[][] keys = {
                {"1", "2", "3"},
                {"4", "5", "6"},
                {"7", "8", "9"},
                {"*", "0", "#"}
        };

        for (int row = 0;
             row < 4;
             row++) {
            for (int column = 0;
                 column < 3;
                 column++) {
                int keyX =
                        startX
                                + column * (keySize + gap);

                int keyY =
                        startY
                                + row * (keySize + 9);

                if (inside(
                        mouseX,
                        mouseY,
                        keyX,
                        keyY,
                        keySize,
                        keySize
                )) {
                    if (typedNumber.length()
                            < 24) {
                        typedNumber +=
                                keys[row][column];
                    }

                    return true;
                }
            }
        }

        int callY =
                startY + 4 * (keySize + 9);

        if (inside(
                mouseX,
                mouseY,
                phoneX + PHONE_WIDTH / 2 - 25,
                callY,
                50,
                36
        )) {
            if (!typedNumber.isBlank()) {
                PhoneCallState.start(
                        typedNumber
                );
            }

            return true;
        }

        if (inside(
                mouseX,
                mouseY,
                phoneX + PHONE_WIDTH / 2 + 38,
                callY,
                45,
                36
        )) {
            if (!typedNumber.isEmpty()) {
                typedNumber =
                        typedNumber.substring(
                                0,
                                typedNumber.length() - 1
                        );
            }

            return true;
        }

        return false;
    }

    private static List<String> contactNumbers(
            PhoneMessagesSnapshot snapshot,
            String ownNumber
    ) {
        Set<String> values =
                new LinkedHashSet<>();

        for (PhoneSmsMessage message :
                snapshot.messages()) {
            addContact(
                    values,
                    message.from(),
                    ownNumber
            );

            addContact(
                    values,
                    message.to(),
                    ownNumber
            );
        }

        return new ArrayList<>(
                values
        );
    }

    private static void addContact(
            Set<String> values,
            String value,
            String ownNumber
    ) {
        if (value == null
                || value.isBlank()) {
            return;
        }

        if (!ownNumber.isBlank()
                && ownNumber.equals(
                value
        )) {
            return;
        }

        values.add(
                value
        );
    }


    private void drawPhoneGlyph(
            GuiGraphics g,
            int cx,
            int cy,
            int color
    ) {
        roundedRect(
                g,
                cx - 8,
                cy + 2,
                7,
                5,
                3,
                color
        );

        g.fill(
                cx - 5,
                cy - 4,
                cx,
                cy + 5,
                color
        );

        g.fill(
                cx - 1,
                cy - 7,
                cx + 5,
                cy,
                color
        );

        roundedRect(
                g,
                cx + 2,
                cy - 10,
                8,
                6,
                3,
                color
        );
    }

    private static String formatDuration(
            long seconds
    ) {
        return String.format(
                "%02d:%02d",
                seconds / 60L,
                seconds % 60L
        );
    }
}
