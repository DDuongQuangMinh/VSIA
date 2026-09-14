package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLanguageCatalog;
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

    private static final int ROW_HEIGHT = 32;

    private int contentX;
    private int contentWidth;
    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;
    private int scrollOffset;
    private int contentHeight;

    private List<PhoneLanguageCatalog.LanguageProfile> languages;

    public IPhoneLanguagePickerScreen() {
        super(Component.literal("Choose Language"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        viewportTop = phoneY + 79;
        viewportBottom = phoneY + PHONE_HEIGHT - 29;
        viewportHeight = viewportBottom - viewportTop;

        languages =
                PhoneLocaleSettings.availableLanguages();

        contentHeight =
                languages.size() * ROW_HEIGHT;

        scrollOffset =
                centerSelectedLanguage();
    }

    private int centerSelectedLanguage() {
        String selected =
                PhoneLocaleSettings
                        .language()
                        .languageTag();

        for (int i = 0; i < languages.size(); i++) {
            if (languages
                    .get(i)
                    .languageTag()
                    .equalsIgnoreCase(selected)) {
                return clampScroll(
                        i * ROW_HEIGHT
                                - viewportHeight / 2
                );
            }
        }

        return 0;
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

        graphics.enableScissor(
                phoneX + DISPLAY_INSET,
                viewportTop,
                phoneX + PHONE_WIDTH - DISPLAY_INSET,
                viewportBottom
        );

        int start =
                Math.max(
                        0,
                        scrollOffset / ROW_HEIGHT
                );

        int end =
                Math.min(
                        languages.size(),
                        start
                                + viewportHeight / ROW_HEIGHT
                                + 3
                );

        for (int i = start; i < end; i++) {
            PhoneLanguageCatalog.LanguageProfile profile =
                    languages.get(i);

            int y =
                    viewportTop
                            + i * ROW_HEIGHT
                            - scrollOffset;

            drawLanguageRow(
                    graphics,
                    y,
                    profile,
                    i == 0,
                    i == languages.size() - 1
            );
        }

        graphics.disableScissor();
        renderHomeIndicator(graphics);
    }

    private void drawLanguageRow(
            GuiGraphics graphics,
            int y,
            PhoneLanguageCatalog.LanguageProfile profile,
            boolean first,
            boolean last
    ) {
        if (first || last) {
            roundedRect(
                    graphics,
                    contentX,
                    y,
                    contentWidth,
                    ROW_HEIGHT,
                    10,
                    CARD
            );
        } else {
            graphics.fill(
                    contentX,
                    y,
                    contentX + contentWidth,
                    y + ROW_HEIGHT,
                    CARD
            );
        }

        drawUiText(
                graphics,
                fitUi(
                        profile.displayName(),
                        contentWidth - 44
                ),
                contentX + 12,
                y + 11,
                TEXT
        );

        if (profile
                .languageTag()
                .equalsIgnoreCase(
                        PhoneLocaleSettings
                                .language()
                                .languageTag()
                )) {
            drawUiText(
                    graphics,
                    "✓",
                    contentX + contentWidth - 18,
                    y + 11,
                    BLUE
            );
        }

        if (!last) {
            graphics.fill(
                    contentX + 12,
                    y + ROW_HEIGHT - 1,
                    contentX + contentWidth - 12,
                    y + ROW_HEIGHT,
                    DIVIDER
            );
        }
    }

    private int clampScroll(int value) {
        int max =
                Math.max(
                        0,
                        contentHeight - viewportHeight
                );

        return Math.max(
                0,
                Math.min(
                        value,
                        max
                )
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        if (inside(
                mouseX,
                mouseY,
                contentX,
                viewportTop,
                contentWidth,
                viewportHeight
        )) {
            scrollOffset =
                    clampScroll(
                            scrollOffset
                                    + (delta > 0.0
                                    ? -ROW_HEIGHT * 3
                                    : ROW_HEIGHT * 3)
                    );

            return true;
        }

        return super.mouseScrolled(
                mouseX,
                mouseY,
                delta
        );
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
                    viewportTop,
                    contentWidth,
                    viewportHeight
            )) {
                int logicalY =
                        (int) mouseY
                                - viewportTop
                                + scrollOffset;

                int index =
                        logicalY / ROW_HEIGHT;

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
