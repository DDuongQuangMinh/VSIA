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
import java.util.List;

public final class IPhonePhotoViewerScreen extends IPhoneScreen {
    private static final int BG = 0xFF000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF5FA9FF;
    private static final int RED = 0xFFFF453A;
    private static final int TOOLBAR = 0xCC1C1C1E;

    private final long photoId;

    private int imageX;
    private int imageY;
    private int imageWidth;
    private int imageHeight;
    private int toolbarY;

    public IPhonePhotoViewerScreen(
            long photoId
    ) {
        super(
                Component.literal(
                        "Photo"
                )
        );

        this.photoId =
                photoId;
    }

    @Override
    protected void init() {
        super.init();

        imageX =
                phoneX + 12;

        imageY =
                phoneY + 72;

        imageWidth =
                PHONE_WIDTH - 24;

        imageHeight =
                235;

        toolbarY =
                phoneY + PHONE_HEIGHT - 69;
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderPhoneShell(
                graphics,
                BG
        );

        renderStatusBar(
                graphics
        );

        drawUiText(
                graphics,
                "‹ Photos",
                phoneX + 16,
                phoneY + 48,
                BLUE
        );

        List<PhonePersonalAppsState.Photo> photos =
                PhonePersonalAppsState.photos();

        int index =
                indexOf(
                        photos,
                        photoId
                );

        if (index >= 0) {
            String position =
                    (index + 1)
                            + " of "
                            + photos.size();

            drawUiText(
                    graphics,
                    position,
                    phoneX + PHONE_WIDTH - 16 - uiWidth(position),
                    phoneY + 48,
                    MUTED
            );
        }

        PhonePersonalAppsState.Photo photo =
                findPhoto(
                        photos
                );

        if (photo == null) {
            drawUiCentered(
                    graphics,
                    "Photo unavailable",
                    phoneX + PHONE_WIDTH / 2,
                    imageY + 90,
                    MUTED
            );
        } else {
            renderPhoto(
                    graphics,
                    photo
            );

            drawUiCentered(
                    graphics,
                    formatTimestamp(
                            photo.capturedAt()
                    ),
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight + 15,
                    TEXT
            );

            String details =
                    shortDimension(
                            photo.dimension()
                    )
                            + " · "
                            + photo.x()
                            + ", "
                            + photo.y()
                            + ", "
                            + photo.z()
                            + " · "
                            + photo.zoom()
                            + "x";

            drawUiCentered(
                    graphics,
                    fitUi(
                            details,
                            PHONE_WIDTH - 50
                    ),
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight + 31,
                    MUTED
            );

            renderToolbar(
                    graphics,
                    index,
                    photos.size()
            );
        }

        renderHomeIndicator(
                graphics
        );
    }

    private void renderPhoto(
            GuiGraphics graphics,
            PhonePersonalAppsState.Photo photo
    ) {
        ResourceLocation texture =
                PhonePhotoTextureCache.textureFor(
                        photo
                );

        roundedRect(
                graphics,
                imageX,
                imageY,
                imageWidth,
                imageHeight,
                12,
                0xFF121214
        );

        if (texture == null) {
            drawUiCentered(
                    graphics,
                    "Image file missing",
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight / 2,
                    MUTED
            );

            return;
        }

        int textureWidth =
                PhonePhotoTextureCache.textureWidth(
                        photo.id()
                );

        int textureHeight =
                PhonePhotoTextureCache.textureHeight(
                        photo.id()
                );

        if (textureWidth <= 0
                || textureHeight <= 0) {
            drawUiCentered(
                    graphics,
                    "Image unavailable",
                    phoneX + PHONE_WIDTH / 2,
                    imageY + imageHeight / 2,
                    MUTED
            );

            return;
        }

        float scale =
                Math.min(
                        imageWidth
                                / (float) textureWidth,
                        imageHeight
                                / (float) textureHeight
                );

        int drawWidth =
                Math.max(
                        1,
                        Math.round(
                                textureWidth
                                        * scale
                        )
                );

        int drawHeight =
                Math.max(
                        1,
                        Math.round(
                                textureHeight
                                        * scale
                        )
                );

        int drawX =
                imageX
                        + (imageWidth - drawWidth)
                        / 2;

        int drawY =
                imageY
                        + (imageHeight - drawHeight)
                        / 2;

        graphics.enableScissor(
                imageX,
                imageY,
                imageX + imageWidth,
                imageY + imageHeight
        );

        graphics.blit(
                texture,
                drawX,
                drawY,
                0.0F,
                0.0F,
                drawWidth,
                drawHeight,
                textureWidth,
                textureHeight
        );

        graphics.disableScissor();
    }

    private void renderToolbar(
            GuiGraphics graphics,
            int index,
            int total
    ) {
        roundedRect(
                graphics,
                phoneX + 18,
                toolbarY,
                PHONE_WIDTH - 36,
                38,
                14,
                TOOLBAR
        );

        drawUiCentered(
                graphics,
                index > 0
                        ? "‹"
                        : "·",
                phoneX + 48,
                toolbarY + 14,
                index > 0
                        ? TEXT
                        : MUTED
        );

        drawUiCentered(
                graphics,
                "Delete",
                phoneX + PHONE_WIDTH / 2,
                toolbarY + 14,
                RED
        );

        drawUiCentered(
                graphics,
                index >= 0
                        && index + 1 < total
                        ? "›"
                        : "·",
                phoneX + PHONE_WIDTH - 48,
                toolbarY + 14,
                index >= 0
                        && index + 1 < total
                        ? TEXT
                        : MUTED
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
                        new IPhonePhotosScreen()
                );

                return true;
            }

            if (inside(
                    mouseX,
                    mouseY,
                    phoneX + 18,
                    toolbarY,
                    PHONE_WIDTH - 36,
                    38
            )) {
                List<PhonePersonalAppsState.Photo> photos =
                        PhonePersonalAppsState.photos();

                int index =
                        indexOf(
                                photos,
                                photoId
                        );

                int third =
                        (PHONE_WIDTH - 36)
                                / 3;

                if (mouseX
                        < phoneX + 18 + third) {
                    if (index > 0) {
                        minecraft.setScreen(
                                new IPhonePhotoViewerScreen(
                                        photos.get(
                                                index - 1
                                        ).id()
                                )
                        );
                    }

                    return true;
                }

                if (mouseX
                        >= phoneX + 18 + third * 2) {
                    if (index >= 0
                            && index + 1 < photos.size()) {
                        minecraft.setScreen(
                                new IPhonePhotoViewerScreen(
                                        photos.get(
                                                index + 1
                                        ).id()
                                )
                        );
                    }

                    return true;
                }

                PhonePersonalAppsState.deletePhoto(
                        photoId
                );

                List<PhonePersonalAppsState.Photo> remaining =
                        PhonePersonalAppsState.photos();

                if (remaining.isEmpty()) {
                    minecraft.setScreen(
                            new IPhonePhotosScreen()
                    );
                } else {
                    int nextIndex =
                            Math.max(
                                    0,
                                    Math.min(
                                            index,
                                            remaining.size() - 1
                                    )
                            );

                    minecraft.setScreen(
                            new IPhonePhotoViewerScreen(
                                    remaining.get(
                                            nextIndex
                                    ).id()
                            )
                    );
                }

                return true;
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    private PhonePersonalAppsState.Photo findPhoto(
            List<PhonePersonalAppsState.Photo> photos
    ) {
        for (PhonePersonalAppsState.Photo photo :
                photos) {
            if (photo.id()
                    == photoId) {
                return photo;
            }
        }

        return null;
    }

    private static int indexOf(
            List<PhonePersonalAppsState.Photo> photos,
            long id
    ) {
        for (int i = 0;
             i < photos.size();
             i++) {
            if (photos.get(
                    i
            ).id() == id) {
                return i;
            }
        }

        return -1;
    }

    private String formatTimestamp(
            long millis
    ) {
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

    private static String shortDimension(
            String dimension
    ) {
        if (dimension == null
                || dimension.isBlank()) {
            return "Unknown";
        }

        int separator =
                dimension.lastIndexOf(
                        ':'
                );

        String value =
                separator >= 0
                        ? dimension.substring(
                        separator + 1
                )
                        : dimension;

        if (value.isBlank()) {
            return dimension;
        }

        return Character.toUpperCase(
                value.charAt(
                        0
                )
        )
                + value.substring(
                1
        );
    }
}
