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
    private static final int TEXT = 0xFFFFFFFF;
    private static final int MUTED = 0xFFAEAEB2;
    private static final int BLUE = 0xFF0A84FF;

    private static final int COLUMNS = 3;
    private static final int CELL = 64;
    private static final int GAP = 4;
    private static final int PAGE_SIZE = 12;

    private int contentX;
    private int gridY;
    private int firstIndex;

    public IPhonePhotosScreen() {
        super(Component.literal("Photos"));
    }

    @Override
    protected void init() {
        super.init();

        int gridWidth =
                COLUMNS * CELL
                        + (COLUMNS - 1) * GAP;

        contentX =
                phoneX
                        + (PHONE_WIDTH - gridWidth)
                        / 2;

        gridY = phoneY + 94;
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

        drawUiCentered(
                graphics,
                "Photos",
                phoneX + PHONE_WIDTH / 2,
                phoneY + 49,
                TEXT
        );

        List<PhonePersonalAppsState.Photo> photos =
                PhonePersonalAppsState.photos();

        drawUiText(
                graphics,
                "Recents",
                phoneX + 16,
                phoneY + 72,
                TEXT
        );

        drawUiText(
                graphics,
                Integer.toString(
                        photos.size()
                ),
                phoneX + PHONE_WIDTH - 28,
                phoneY + 72,
                MUTED
        );

        beginPhoneClip(
                graphics,
                84
        );

        if (photos.isEmpty()) {
            drawUiCentered(
                    graphics,
                    "No Photos",
                    phoneX + PHONE_WIDTH / 2,
                    gridY + 48,
                    MUTED
            );

            drawUiCentered(
                    graphics,
                    "Take a photo with Camera",
                    phoneX + PHONE_WIDTH / 2,
                    gridY + 68,
                    BLUE
            );
        } else {
            clampFirstIndex(
                    photos.size()
            );

            int end =
                    Math.min(
                            photos.size(),
                            firstIndex + PAGE_SIZE
                    );

            for (int index = firstIndex;
                 index < end;
                 index++) {
                int local =
                        index - firstIndex;

                int column =
                        local % COLUMNS;

                int row =
                        local / COLUMNS;

                int x =
                        contentX
                                + column * (CELL + GAP);

                int y =
                        gridY
                                + row * (CELL + GAP);

                renderThumbnail(
                        graphics,
                        photos.get(index),
                        x,
                        y
                );
            }

            if (photos.size()
                    > PAGE_SIZE) {
                String page =
                        (
                                firstIndex / PAGE_SIZE + 1
                        )
                                + " / "
                                + (
                                (
                                        photos.size()
                                                + PAGE_SIZE - 1
                                )
                                        / PAGE_SIZE
                        );

                drawUiCentered(
                        graphics,
                        page,
                        phoneX + PHONE_WIDTH / 2,
                        phoneY + PHONE_HEIGHT - 42,
                        MUTED
                );
            }
        }

        endPhoneClip(
                graphics
        );

        renderHomeIndicator(
                graphics
        );
    }

    private void renderThumbnail(
            GuiGraphics graphics,
            PhonePersonalAppsState.Photo photo,
            int x,
            int y
    ) {
        ResourceLocation texture =
                PhonePhotoTextureCache.textureFor(
                        photo
                );

        roundedRect(
                graphics,
                x,
                y,
                CELL,
                CELL,
                7,
                0xFF242426
        );

        if (texture != null) {
            graphics.blit(
                    texture,
                    x + 1,
                    y + 1,
                    0.0F,
                    0.0F,
                    CELL - 2,
                    CELL - 2,
                    PhonePhotoTextureCache.textureWidth(
                            photo.id()
                    ),
                    PhonePhotoTextureCache.textureHeight(
                            photo.id()
                    )
            );
        } else {
            int color =
                    thumbnailColor(
                            photo.dimension()
                    );

            graphics.fill(
                    x + 1,
                    y + 1,
                    x + CELL - 1,
                    y + CELL - 1,
                    color
            );

            graphics.fill(
                    x + 1,
                    y + CELL / 2,
                    x + CELL - 1,
                    y + CELL - 1,
                    darken(
                            color,
                            0.62F
                    )
            );
        }

        roundedRect(
                graphics,
                x + 4,
                y + CELL - 16,
                CELL - 8,
                12,
                6,
                0x88000000
        );

        drawUiCentered(
                graphics,
                formatThumbnailTime(
                        photo.capturedAt()
                ),
                x + CELL / 2,
                y + CELL - 13,
                0xFFFFFFFF
        );
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

            if (!photos.isEmpty()) {
                clampFirstIndex(
                        photos.size()
                );

                int gridWidth =
                        COLUMNS * CELL
                                + (COLUMNS - 1) * GAP;

                int gridHeight =
                        4 * CELL
                                + 3 * GAP;

                if (inside(
                        mouseX,
                        mouseY,
                        contentX,
                        gridY,
                        gridWidth,
                        gridHeight
                )) {
                    int relativeX =
                            (int) mouseX
                                    - contentX;

                    int relativeY =
                            (int) mouseY
                                    - gridY;

                    int column =
                            relativeX
                                    / (CELL + GAP);

                    int row =
                            relativeY
                                    / (CELL + GAP);

                    int inCellX =
                            relativeX
                                    % (CELL + GAP);

                    int inCellY =
                            relativeY
                                    % (CELL + GAP);

                    if (column < COLUMNS
                            && row < 4
                            && inCellX < CELL
                            && inCellY < CELL) {
                        int index =
                                firstIndex
                                        + row * COLUMNS
                                        + column;

                        if (index >= 0
                                && index < photos.size()) {
                            minecraft.setScreen(
                                    new IPhonePhotoViewerScreen(
                                            photos.get(index).id()
                                    )
                            );

                            return true;
                        }
                    }
                }
            }
        }

        return super.mouseClicked(
                mouseX,
                mouseY,
                button
        );
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double delta
    ) {
        List<PhonePersonalAppsState.Photo> photos =
                PhonePersonalAppsState.photos();

        if (photos.size()
                <= PAGE_SIZE) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    delta
            );
        }

        if (delta < 0.0D) {
            firstIndex =
                    Math.min(
                            maxPageStart(
                                    photos.size()
                            ),
                            firstIndex + PAGE_SIZE
                    );
        } else if (delta > 0.0D) {
            firstIndex =
                    Math.max(
                            0,
                            firstIndex - PAGE_SIZE
                    );
        }

        return true;
    }

    private void clampFirstIndex(
            int size
    ) {
        firstIndex =
                Math.max(
                        0,
                        Math.min(
                                firstIndex,
                                maxPageStart(
                                        size
                                )
                        )
                );
    }

    private static int maxPageStart(
            int size
    ) {
        if (size <= PAGE_SIZE) {
            return 0;
        }

        return (
                (size - 1)
                        / PAGE_SIZE
        )
                * PAGE_SIZE;
    }

    private String formatThumbnailTime(
            long millis
    ) {
        LocalDateTime time =
                LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(
                                millis
                        ),
                        PhoneLocaleSettings.regionZoneId()
                );

        return PhoneLocaleSettings.formatTime(
                time.toLocalTime(),
                PhoneSystemSettings.use24HourTime()
        );
    }

    private static int thumbnailColor(
            String dimension
    ) {
        if (dimension != null
                && dimension.contains(
                "nether"
        )) {
            return 0xFF80483D;
        }

        if (dimension != null
                && dimension.contains(
                "end"
        )) {
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

        r = Math.round(
                r * factor
        );

        g = Math.round(
                g * factor
        );

        b = Math.round(
                b * factor
        );

        return a << 24
                | r << 16
                | g << 8
                | b;
    }
}
