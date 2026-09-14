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

public final class IPhonePhotosScreen extends IPhoneScreen {
    private static final int BG = 0xFF111113;
    private static final int CARD = 0xFF2C2C2E;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;

    private static final int ROW_HEIGHT = 55;

    private int contentX;
    private int contentWidth;
    private int listY;

    public IPhonePhotosScreen() {
        super(Component.literal("Photos"));
    }

    @Override
    protected void init() {
        super.init();

        contentX = phoneX + 14;
        contentWidth = PHONE_WIDTH - 28;
        listY = phoneY + 82;
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
                "Photos",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 51,
                TEXT
        );

        beginPhoneClip(graphics, 68);

        List<PhonePersonalAppsState.Photo> photos =
                PhonePersonalAppsState.photos();

        if (photos.isEmpty()) {
            drawUiCentered(
                    graphics,
                    "No Photos",
                    phoneX + PHONE_WIDTH / 2,
                    listY + 40,
                    MUTED
            );
        } else {
            int visible =
                    Math.min(
                            6,
                            photos.size()
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
                PhonePersonalAppsState.Photo photo =
                        photos.get(i);

                int y =
                        listY + i * ROW_HEIGHT;

                ResourceLocation texture =
                        PhonePhotoTextureCache.textureFor(
                                photo
                        );

                if (texture != null) {
                    graphics.blit(
                            texture,
                            contentX + 8,
                            y + 7,
                            0.0F,
                            0.0F,
                            41,
                            41,
                            PhonePhotoTextureCache.textureWidth(
                                    photo.id()
                            ),
                            PhonePhotoTextureCache.textureHeight(
                                    photo.id()
                            )
                    );
                } else {
                    int thumb =
                            thumbnailColor(
                                    photo.dimension()
                            );

                    roundedRect(
                            graphics,
                            contentX + 8,
                            y + 7,
                            41,
                            41,
                            8,
                            thumb
                    );

                    graphics.fill(
                            contentX + 8,
                            y + 28,
                            contentX + 49,
                            y + 48,
                            darken(
                                    thumb,
                                    0.63F
                            )
                    );
                }

                drawUiText(
                        graphics,
                        fitUi(
                                formatTimestamp(
                                        photo.capturedAt()
                                ),
                                contentWidth - 78
                        ),
                        contentX + 58,
                        y + 10,
                        TEXT
                );

                drawUiText(
                        graphics,
                        fitUi(
                                photo.dimension(),
                                contentWidth - 78
                        ),
                        contentX + 58,
                        y + 25,
                        MUTED
                );

                drawUiText(
                        graphics,
                        fitUi(
                                photo.x()
                                        + ", "
                                        + photo.y()
                                        + ", "
                                        + photo.z()
                                        + " · "
                                        + photo.zoom()
                                        + "x",
                                contentWidth - 78
                        ),
                        contentX + 58,
                        y + 39,
                        BLUE
                );
            }
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
            List<PhonePersonalAppsState.Photo> photos =
                    PhonePersonalAppsState.photos();

            int visible =
                    Math.min(
                            6,
                            photos.size()
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
                            new IPhonePhotoViewerScreen(
                                    photos.get(index).id()
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

    private static int thumbnailColor(
            String dimension
    ) {
        if (dimension != null
                && dimension.contains("nether")) {
            return 0xFF80483D;
        }

        if (dimension != null
                && dimension.contains("end")) {
            return 0xFF5A5778;
        }

        return 0xFF66A6C8;
    }

    private static int darken(
            int color,
            float factor
    ) {
        int a =
                color >>> 24
                        & 0xFF;

        int r =
                color >>> 16
                        & 0xFF;

        int g =
                color >>> 8
                        & 0xFF;

        int b =
                color
                        & 0xFF;

        r = Math.round(r * factor);
        g = Math.round(g * factor);
        b = Math.round(b * factor);

        return a << 24
                | r << 16
                | g << 8
                | b;
    }
}
