package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IPhoneLanguagePickerScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 42;

    private int contentX;
    private int contentWidth;
    private int groupY;

    private List<PhoneLocaleSettings.Language> languages;

    public IPhoneLanguagePickerScreen() {
        super(Component.literal("Choose Language"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        groupY = phoneY + 86;

        languages =
                PhoneLocaleSettings.availableLanguages();
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

        renderHeader(
                graphics,
                "Language & Region",
                "Choose Language"
        );

        beginPhoneClip(graphics, 68);

        drawUiText(
                graphics,
                "LANGUAGE",
                contentX + 4,
                groupY - 17,
                MUTED
        );

        roundedRect(
                graphics,
                contentX,
                groupY,
                contentWidth,
                ROW_HEIGHT * languages.size(),
                14,
                CARD
        );

        for (int i = 0; i < languages.size(); i++) {
            PhoneLocaleSettings.Language language =
                    languages.get(i);

            int y =
                    groupY + i * ROW_HEIGHT;

            drawUiText(
                    graphics,
                    language.displayName(),
                    contentX + 13,
                    y + 16,
                    TEXT
            );

            if (language
                    == PhoneLocaleSettings.language()) {
                drawUiText(
                        graphics,
                        "✓",
                        contentX + contentWidth - 21,
                        y + 16,
                        BLUE
                );
            }

            if (i + 1 < languages.size()) {
                graphics.fill(
                        contentX + 13,
                        y + ROW_HEIGHT,
                        contentX + contentWidth - 13,
                        y + ROW_HEIGHT + 1,
                        DIVIDER
                );
            }
        }

        drawUiWrappedCentered(
                graphics,
                "Changing iPhone Language translates the VS:IA phone interface immediately.",
                phoneX + PHONE_WIDTH / 2,
                groupY + ROW_HEIGHT * languages.size() + 22,
                PHONE_WIDTH - 48,
                11,
                4,
                MUTED
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
            if (clickedBack(
                    mouseX,
                    mouseY
            )) {
                minecraft.setScreen(
                        new IPhoneLanguageRegionScreen()
                );
                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    contentX,
                    groupY,
                    contentWidth,
                    ROW_HEIGHT * languages.size()
            )) {
                int index =
                        ((int) mouseY - groupY)
                                / ROW_HEIGHT;

                if (index >= 0
                        && index < languages.size()) {
                    PhoneLocaleSettings.setLanguage(
                            languages.get(index)
                    );

                    minecraft.setScreen(
                            new IPhoneLanguageRegionScreen()
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
