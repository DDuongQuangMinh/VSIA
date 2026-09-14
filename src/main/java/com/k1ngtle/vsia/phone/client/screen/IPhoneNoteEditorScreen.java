package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import com.k1ngtle.vsia.phone.client.PhoneText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public final class IPhoneNoteEditorScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8E8E93;
    private static final int YELLOW = 0xFFFFD60A;
    private static final int RED = 0xFFFF453A;

    private final long noteId;

    private String titleText = "";
    private String bodyText = "";
    private boolean bodyActive = true;

    private int contentX;
    private int contentWidth;
    private int titleY;
    private int bodyY;
    private int doneY;

    public IPhoneNoteEditorScreen(long noteId) {
        super(Component.literal("Edit Note"));
        this.noteId = noteId;
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        titleY = phoneY + 82;
        bodyY = titleY + 51;
        doneY = phoneY + PHONE_HEIGHT - 70;

        if (noteId > 0L) {
            PhonePersonalAppsState.Note note =
                    PhonePersonalAppsState.note(
                            noteId
                    );

            if (note != null) {
                titleText = note.title();
                bodyText = note.body();
            }
        }
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
                noteId > 0L
                        ? "Edit Note"
                        : "New Note",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        roundedRect(
                graphics,
                contentX,
                titleY,
                contentWidth,
                39,
                11,
                CARD
        );

        drawUiText(
                graphics,
                titleText.isEmpty()
                        ? "Title"
                        : fitUi(
                                titleText
                                        + (!bodyActive ? "|" : ""),
                                contentWidth - 26
                        ),
                contentX + 13,
                titleY + 14,
                titleText.isEmpty()
                        ? MUTED
                        : TEXT
        );

        roundedRect(
                graphics,
                contentX,
                bodyY,
                contentWidth,
                184,
                11,
                CARD
        );

        if (bodyText.isEmpty()) {
            drawUiText(
                    graphics,
                    "Type note...",
                    contentX + 13,
                    bodyY + 12,
                    MUTED
            );
        } else {
            List<FormattedCharSequence> lines =
                    PhoneText.split(
                            font,
                            bodyText + (bodyActive ? "|" : ""),
                            contentWidth - 26
                    );

            int maxLines = 12;

            for (int i = 0;
                 i < lines.size() && i < maxLines;
                 i++) {
                graphics.drawString(
                        font,
                        lines.get(i),
                        contentX + 13,
                        bodyY + 12 + i * 13,
                        TEXT,
                        false
                );
            }
        }

        roundedRect(
                graphics,
                contentX,
                doneY,
                contentWidth,
                36,
                12,
                0xFF343438
        );

        drawUiCentered(
                graphics,
                "Done",
                phoneX + PHONE_WIDTH / 2,
                doneY + 13,
                YELLOW
        );

        if (noteId > 0L) {
            drawUiText(
                    graphics,
                    "Delete",
                    contentX + 5,
                    doneY - 20,
                    RED
            );
        }

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
                    titleY,
                    contentWidth,
                    39
            )) {
                bodyActive = false;
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    bodyY,
                    contentWidth,
                    184
            )) {
                bodyActive = true;
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    doneY,
                    contentWidth,
                    36
            )) {
                saveAndClose();
                return true;
            }

            if (noteId > 0L
                    && inside(
                    mouseX,
                    mouseY,
                    contentX,
                    doneY - 25,
                    55,
                    22
            )) {
                PhonePersonalAppsState.deleteNote(
                        noteId
                );

                minecraft.setScreen(
                        new IPhoneNotesScreen()
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

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers
    ) {
        if (Character.isISOControl(codePoint)) {
            return false;
        }

        if (bodyActive) {
            if (bodyText.length() < 1200) {
                bodyText += codePoint;
            }
        } else {
            if (titleText.length() < 80) {
                titleText += codePoint;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (keyCode == 259) {
            if (bodyActive) {
                if (!bodyText.isEmpty()) {
                    bodyText =
                            bodyText.substring(
                                    0,
                                    bodyText.length() - 1
                            );
                }
            } else if (!titleText.isEmpty()) {
                titleText =
                        titleText.substring(
                                0,
                                titleText.length() - 1
                        );
            }

            return true;
        }

        if (keyCode == 257
                || keyCode == 335) {
            if (bodyActive) {
                if (bodyText.length() < 1200) {
                    bodyText += "\n";
                }
            } else {
                bodyActive = true;
            }

            return true;
        }

        if (keyCode == 258) {
            bodyActive = !bodyActive;
            return true;
        }

        if (keyCode == 256) {
            saveAndClose();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    private void saveAndClose() {
        PhonePersonalAppsState.saveNote(
                noteId,
                titleText,
                bodyText
        );

        minecraft.setScreen(
                new IPhoneNotesScreen()
        );
    }
}
