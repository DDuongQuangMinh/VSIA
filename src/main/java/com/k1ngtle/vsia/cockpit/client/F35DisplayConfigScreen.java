package com.k1ngtle.vsia.cockpit.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class F35DisplayConfigScreen
        extends Screen {
    private static final int BUTTON_WIDTH =
            220;

    private static final int BUTTON_HEIGHT =
            20;

    public F35DisplayConfigScreen() {
        super(
                Component.literal(
                        "F-35 Display Configuration"
                )
        );
    }

    @Override
    protected void init() {
        F35DisplayClientConfig.ensureLoaded();

        int x =
                width / 2
                        - BUTTON_WIDTH / 2;

        int y =
                height / 2
                        - 78;

        addRenderableWidget(
                Button.builder(
                                brightnessLabel(),
                                button -> {
                                    F35DisplayClientConfig.cycleBrightness();
                                    button.setMessage(
                                            brightnessLabel()
                                    );
                                }
                        )
                        .bounds(
                                x,
                                y,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                scaleLabel(),
                                button -> {
                                    F35DisplayClientConfig.cycleDisplayScale();
                                    button.setMessage(
                                            scaleLabel()
                                    );
                                }
                        )
                        .bounds(
                                x,
                                y + 24,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                radarRangeLabel(),
                                button -> {
                                    F35DisplayClientConfig.cycleRadarRange();
                                    button.setMessage(
                                            radarRangeLabel()
                                    );
                                }
                        )
                        .bounds(
                                x,
                                y + 48,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                trackLabelsLabel(),
                                button -> {
                                    F35DisplayClientConfig.toggleTrackLabels();
                                    button.setMessage(
                                            trackLabelsLabel()
                                    );
                                }
                        )
                        .bounds(
                                x,
                                y + 72,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Reset Display Settings"
                                ),
                                button -> {
                                    F35DisplayClientConfig.resetDefaults();
                                    if (minecraft != null) {
                                        minecraft.setScreen(
                                                new F35DisplayConfigScreen()
                                        );
                                    }
                                }
                        )
                        .bounds(
                                x,
                                y + 104,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );

        addRenderableWidget(
                Button.builder(
                                Component.literal(
                                        "Done"
                                ),
                                button ->
                                        onClose()
                        )
                        .bounds(
                                x,
                                y + 136,
                                BUTTON_WIDTH,
                                BUTTON_HEIGHT
                        )
                        .build()
        );
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderBackground(
                graphics
        );

        graphics.drawCenteredString(
                font,
                title,
                width / 2,
                height / 2 - 112,
                0xFFFFFFFF
        );

        graphics.drawCenteredString(
                font,
                Component.literal(
                        "Press \\ again or Esc to close"
                ),
                width / 2,
                height / 2 + 88,
                0xFFA0A0A0
        );

        graphics.drawCenteredString(
                font,
                Component.literal(
                        "Free-look remains active while seated"
                ),
                width / 2,
                height / 2 + 100,
                0xFF70D7C5
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers
    ) {
        if (F35DisplayKeyMappings
                .CONFIGURE_DISPLAY
                .matches(
                        keyCode,
                        scanCode
                )) {
            onClose();
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component brightnessLabel() {
        return Component.literal(
                "Brightness: "
                        + F35DisplayClientConfig.brightnessPercent()
                        + "%"
        );
    }

    private Component scaleLabel() {
        return Component.literal(
                "Display Scale: "
                        + F35DisplayClientConfig.displayScalePercent()
                        + "%"
        );
    }

    private Component radarRangeLabel() {
        return Component.literal(
                "Radar Range: "
                        + F35DisplayClientConfig.radarRangeLabel()
        );
    }

    private Component trackLabelsLabel() {
        return Component.literal(
                "Track Labels: "
                        + (
                        F35DisplayClientConfig.trackLabels()
                                ? "ON"
                                : "OFF"
                )
        );
    }
}
