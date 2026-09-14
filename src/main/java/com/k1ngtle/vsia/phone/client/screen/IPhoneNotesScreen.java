package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IPhoneNotesScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int YELLOW = 0xFFFFD60A;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 48;

    private int contentX;
    private int contentWidth;
    private int listY;
    private int newY;

    public IPhoneNotesScreen() {
        super(Component.literal("Notes"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        listY = phoneY + 84;
        newY = phoneY + PHONE_HEIGHT - 70;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);

        drawUiCentered(
                graphics,
                "Notes",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        List<PhonePersonalAppsState.Note> notes =
                PhonePersonalAppsState.notes();

        if (notes.isEmpty()) {
            drawUiCentered(
                    graphics,
                    "No Notes",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 40,
                    MUTED
            );
        } else {
            int visible =
                    Math.min(
                            6,
                            notes.size()
                    );

            roundedRect(
                    graphics,
                    contentX,
                    listY,
                    contentWidth,
                    visible * ROW_HEIGHT,
                    14,
                    CARD
            );

            for (int i = 0; i < visible; i++) {
                PhonePersonalAppsState.Note note =
                        notes.get(i);

                int y =
                        listY + i * ROW_HEIGHT;

                drawUiText(
                        graphics,
                        fitUi(
                                note.title(),
                                contentWidth - 30
                        ),
                        contentX + 13,
                        y + 10,
                        TEXT
                );

                String preview =
                        note.body()
                                .replace(
                                        '\n',
                                        ' '
                                );

                drawUiText(
                        graphics,
                        fitUi(
                                preview,
                                contentWidth - 30
                        ),
                        contentX + 13,
                        y + 28,
                        MUTED
                );

                if (i + 1 < visible) {
                    graphics.fill(
                            contentX + 13,
                            y + ROW_HEIGHT - 1,
                            contentX + contentWidth - 13,
                            y + ROW_HEIGHT,
                            DIVIDER
                    );
                }
            }
        }

        roundedRect(
                graphics,
                contentX,
                newY,
                contentWidth,
                36,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "+ New Note",
                phoneX + PHONE_WIDTH / 2,
                newY + 13,
                YELLOW
        );

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (button == 0) {
            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    newY,
                    contentWidth,
                    36
            )) {
                minecraft.setScreen(
                        new IPhoneNoteEditorScreen(
                                -1L
                        )
                );

                return true;
            }

            List<PhonePersonalAppsState.Note> notes =
                    PhonePersonalAppsState.notes();

            int visible =
                    Math.min(
                            6,
                            notes.size()
                    );

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    listY,
                    contentWidth,
                    visible * ROW_HEIGHT
            )) {
                int index =
                        ((int) mouseY - listY)
                                / ROW_HEIGHT;

                if (index >= 0
                        && index < visible) {
                    minecraft.setScreen(
                            new IPhoneNoteEditorScreen(
                                    notes.get(index).id()
                            )
                    );

                    return true;
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
