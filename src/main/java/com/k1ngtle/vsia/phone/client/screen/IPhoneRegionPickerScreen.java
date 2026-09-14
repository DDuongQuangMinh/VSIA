package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class IPhoneRegionPickerScreen extends IPhoneScreen {
    private static final int BG = 0xFF1C1C1E;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;
    private static final int DIVIDER = 0xFF3A3A3C;

    private static final int ROW_HEIGHT = 31;

    private int contentX;
    private int contentWidth;
    private int viewportTop;
    private int viewportBottom;
    private int viewportHeight;
    private int scrollOffset;
    private int contentHeight;

    private List<PhoneLocaleSettings.RegionOption> regions;

    public IPhoneRegionPickerScreen() {
        super(Component.literal("Choose Region"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        viewportTop = phoneY + 79;
        viewportBottom = phoneY + PHONE_HEIGHT - 29;
        viewportHeight = viewportBottom - viewportTop;

        regions =
                PhoneLocaleSettings.availableRegions();

        contentHeight =
                regions.size() * ROW_HEIGHT;

        scrollOffset =
                centerSelectedRegion(
                        scrollOffset
                );
    }

    private int centerSelectedRegion(
            int current
    ) {
        String selected =
                PhoneLocaleSettings.regionCode();

        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i)
                    .code()
                    .equals(selected)) {
                int desired =
                        i * ROW_HEIGHT
                                - viewportHeight / 2;

                return clampScroll(
                        desired
                );
            }
        }

        return clampScroll(
                current
        );
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
                "Region"
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
                        regions.size(),
                        start
                                + viewportHeight / ROW_HEIGHT
                                + 3
                );

        for (int i = start; i < end; i++) {
            PhoneLocaleSettings.RegionOption option =
                    regions.get(i);

            int y =
                    viewportTop
                            + i * ROW_HEIGHT
                            - scrollOffset;

            drawRegionRow(
                    graphics,
                    y,
                    option,
                    i == 0,
                    i == regions.size() - 1
            );
        }

        graphics.disableScissor();
        renderHomeIndicator(graphics);
    }

    private void drawRegionRow(
            GuiGraphics graphics,
            int y,
            PhoneLocaleSettings.RegionOption option,
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
                        option.displayName(),
                        contentWidth - 78
                ),
                contentX + 12,
                y + 11,
                TEXT
        );

        String zone =
                fitUi(
                        option.timeZoneId(),
                        70
                );

        drawUiText(
                graphics,
                zone,
                contentX
                        + contentWidth
                        - uiWidth(zone)
                        - 31,
                y + 11,
                MUTED
        );

        if (option.code()
                .equals(
                        PhoneLocaleSettings.regionCode()
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

    private int clampScroll(
            int value
    ) {
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
                        && index < regions.size()) {
                    PhoneLocaleSettings.setRegionCode(
                            regions.get(index).code()
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
