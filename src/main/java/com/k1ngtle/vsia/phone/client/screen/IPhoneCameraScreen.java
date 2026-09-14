package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneCameraCaptureService;
import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class IPhoneCameraScreen extends IPhoneScreen {
    private static final int BG = 0xFF000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int YELLOW = 0xFFFFD60A;

    private boolean frontCamera;
    private int zoom = 1;
    private long flashUntil;

    private int previewX;
    private int previewY;
    private int previewWidth;
    private int previewHeight;
    private int shutterX;
    private int shutterY;

    public IPhoneCameraScreen() {
        super(Component.literal("Camera"));
    }

    @Override
    protected void init() {
        super.init();

        previewX = phoneX + 10;
        previewY = phoneY + 58;
        previewWidth = PHONE_WIDTH - 20;
        previewHeight = 238;

        shutterX =
                phoneX + PHONE_WIDTH / 2 - 18;

        shutterY =
                phoneY + PHONE_HEIGHT - 69;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        PhoneCameraCaptureService.updatePreview(
                previewWidth,
                previewHeight,
                zoom,
                frontCamera
        );

        String completedStill =
                PhoneCameraCaptureService
                        .consumeCompletedStillPath();

        if (!completedStill.isBlank()) {
            PhonePersonalAppsState.capturePhoto(
                    frontCamera,
                    zoom,
                    completedStill
            );

            flashUntil =
                    System.currentTimeMillis()
                            + 220L;
        }

        renderPhoneShell(graphics, BG);
        renderStatusBar(graphics);

        beginPhoneClip(graphics, 48);

        ResourceLocation preview =
                PhoneCameraCaptureService.previewLocation();

        if (preview != null) {
            graphics.blit(
                    preview,
                    previewX,
                    previewY,
                    0.0F,
                    0.0F,
                    previewWidth,
                    previewHeight,
                    previewWidth,
                    previewHeight
            );
        } else {
            roundedRect(
                    graphics,
                    previewX,
                    previewY,
                    previewWidth,
                    previewHeight,
                    8,
                    0xFF252528
            );

            drawUiCentered(
                    graphics,
                    "Camera Preview",
                    phoneX + PHONE_WIDTH / 2,
                    previewY + previewHeight / 2,
                    MUTED
            );
        }

        drawUiCentered(
                graphics,
                frontCamera
                        ? "Front"
                        : "Rear",
                phoneX + PHONE_WIDTH / 2,
                previewY + 14,
                TEXT
        );

        drawUiCentered(
                graphics,
                zoom + "x",
                phoneX + PHONE_WIDTH / 2,
                previewY + previewHeight - 22,
                YELLOW
        );

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player != null) {
            String position =
                    minecraft.player
                            .blockPosition()
                            .getX()
                            + ", "
                            + minecraft.player
                            .blockPosition()
                            .getY()
                            + ", "
                            + minecraft.player
                            .blockPosition()
                            .getZ();

            drawUiCentered(
                    graphics,
                    position,
                    phoneX + PHONE_WIDTH / 2,
                    previewY + previewHeight - 42,
                    TEXT
            );
        }

        roundedRect(
                graphics,
                phoneX + 24,
                shutterY + 4,
                54,
                28,
                14,
                0xFF252528
        );

        drawUiCentered(
                graphics,
                frontCamera
                        ? "Rear"
                        : "Front",
                phoneX + 51,
                shutterY + 14,
                TEXT
        );

        roundedRect(
                graphics,
                shutterX,
                shutterY,
                36,
                36,
                18,
                0xFFFFFFFF
        );

        roundedRect(
                graphics,
                shutterX + 4,
                shutterY + 4,
                28,
                28,
                14,
                0xFF1C1C1E
        );

        roundedRect(
                graphics,
                shutterX + 7,
                shutterY + 7,
                22,
                22,
                11,
                0xFFFFFFFF
        );

        roundedRect(
                graphics,
                phoneX + PHONE_WIDTH - 78,
                shutterY + 4,
                54,
                28,
                14,
                0xFF252528
        );

        drawUiCentered(
                graphics,
                zoom == 1
                        ? "2x"
                        : "1x",
                phoneX + PHONE_WIDTH - 51,
                shutterY + 14,
                TEXT
        );

        if (System.currentTimeMillis()
                < flashUntil) {
            graphics.fill(
                    previewX,
                    previewY,
                    previewX + previewWidth,
                    previewY + previewHeight,
                    0xCCFFFFFF
            );

            drawUiCentered(
                    graphics,
                    "Captured",
                    phoneX + PHONE_WIDTH / 2,
                    previewY + previewHeight / 2,
                    0xFF111111
            );
        }

        endPhoneClip(graphics);
        renderHomeIndicator(graphics);
    }

    @Override
    public void removed() {
        PhoneCameraCaptureService.releasePreview();
        super.removed();
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
                    phoneX + 24,
                    shutterY + 4,
                    54,
                    28
            )) {
                frontCamera =
                        !frontCamera;

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + PHONE_WIDTH - 78,
                    shutterY + 4,
                    54,
                    28
            )) {
                zoom =
                        zoom == 1
                                ? 2
                                : 1;

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    shutterX,
                    shutterY,
                    36,
                    36
            )) {
                PhoneCameraCaptureService
                        .requestStillCapture();

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }
}
