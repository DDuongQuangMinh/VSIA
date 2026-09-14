package com.k1ngtle.vsia.phone.client.screen;

import com.k1ngtle.vsia.phone.client.PhoneLocaleSettings;
import com.k1ngtle.vsia.phone.client.PhonePersonalAppsState;
import com.k1ngtle.vsia.phone.client.PhonePhotoTextureCache;
import com.k1ngtle.vsia.phone.client.PhoneSystemSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.LocalDateTime;

public final class IPhonePhotoViewerScreen extends IPhoneScreen {
    private static final int BG = 0xFF000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int RED = 0xFFFF453A;

    private final long photoId;

    private int imageX;
    private int imageY;
    private int imageWidth;
    private int imageHeight;
    private int deleteY;

    public IPhonePhotoViewerScreen(long photoId) {
        super(Component.literal("Photo"));
        this.photoId = photoId;
    }

    @Override
    protected void init() {
        super.init();

        imageX = phoneX + 10;
        imageY = phoneY + 67;
        imageWidth = PHONE_WIDTH - 20;
        imageHeight = 238;
        deleteY = phoneY + PHONE_HEIGHT - 68;
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

        drawUiText(
                graphics,
                "‹ Photos",
                phoneX + 16,
                phoneY + 47,
                0xFF5FA9FF
        );

        PhonePersonalAppsState.Photo photo =
                findPhoto();

        beginPhoneClip(graphics, 58);

        if (photo == null) {
            drawUiCentered(
                    graphics,
                    "No Photos",
                    phoneX + PHONE_WIDTH / 2,
                    imageY + 40,
                    MUTED
            );
        } else {
            ResourceLocation texture =
                    PhonePhotoTextureCache.textureFor(
                            photo
                    );

            if (texture != null) {
                graphics.blit(
                        texture,
                        imageX,
                        imageY,
                        0.0F,
                        0.0F,
                        imageWidth,
                        imageHeight,
                        PhonePhotoTextureCache.textureWidth(
                                photo.id()
                        ),
                        PhonePhotoTextureCache.textureHeight(
                                photo.id()
                        )
                );
            }

            drawUiCentered(
                    graphics,
                    formatTimestamp(
                            photo.capturedAt()
                    ),
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight + 18,
                    TEXT
            );

            drawUiCentered(
                    graphics,
                    photo.dimension()
                            + " · "
                            + photo.x()
                            + ", "
                            + photo.y()
                            + ", "
                            + photo.z(),
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight + 36,
                    MUTED
            );

            roundedRect(
                    graphics,
                    phoneX + 24,
                    deleteY,
                    PHONE_WIDTH - 48,
                    34,
                    12,
                    0xFF252528
            );

            drawUiCentered(
                    graphics,
                    "Delete",
                    phoneX + PHONE_WIDTH / 2,
                    deleteY + 12,
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
                    phoneX + 8,
                    phoneY + 39,
                    70,
                    30
            )) {
                minecraft.setScreen(
                        new IPhonePhotosScreen()
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 24,
                    deleteY,
                    PHONE_WIDTH - 48,
                    34
            )) {
                PhonePersonalAppsState.deletePhoto(
                        photoId
                );

                minecraft.setScreen(
                        new IPhonePhotosScreen()
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

    private PhonePersonalAppsState.Photo findPhoto() {
        for (PhonePersonalAppsState.Photo photo :
                PhonePersonalAppsState.photos()) {
            if (photo.id() == photoId) {
                return photo;
            }
        }

        return null;
    }

    private String formatTimestamp(long millis) {
        LocalDateTime time =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(
                                millis
                        ),
                        PhoneLocaleSettings.regionZoneId()
                );

        return PhoneLocaleSettings.formatDate(
                time.toLocalDate()
        )
                + " "
                + PhoneLocaleSettings.formatTime(
                time.toLocalTime(),
                PhoneSystemSettings.use24HourTime()
        );
    }
}
