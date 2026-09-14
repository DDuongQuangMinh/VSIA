package com.k1ngtle.vsia.phone.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PhoneCameraCaptureService {
    private static final long PREVIEW_INTERVAL_MS = 180L;

    private static DynamicTexture previewTexture;
    private static ResourceLocation previewLocation;
    private static NativeImage previewImage;

    private static int previewWidth;
    private static int previewHeight;
    private static long lastPreviewCaptureAt;

    private static volatile boolean stillCaptureRequested;
    private static volatile String completedStillPath = "";

    private PhoneCameraCaptureService() {
    }

    public static ResourceLocation previewLocation() {
        return previewLocation;
    }

    public static boolean hasPreview() {
        return previewLocation != null
                && previewImage != null;
    }

    public static void updatePreview(
            int targetWidth,
            int targetHeight,
            int zoom,
            boolean frontCamera
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null
                || targetWidth <= 0
                || targetHeight <= 0) {
            return;
        }

        long now =
                System.currentTimeMillis();

        if (!stillCaptureRequested
                && hasPreview()
                && previewWidth == targetWidth
                && previewHeight == targetHeight
                && now - lastPreviewCaptureAt
                < PREVIEW_INTERVAL_MS) {
            return;
        }

        NativeImage source = null;

        try {
            source =
                    Screenshot.takeScreenshot(
                            minecraft.getMainRenderTarget()
                    );

            if (stillCaptureRequested) {
                completedStillPath =
                        saveFullResolutionCrop(
                                minecraft,
                                source,
                                targetWidth,
                                targetHeight,
                                zoom,
                                frontCamera
                        );

                stillCaptureRequested = false;
            }

            NativeImage next =
                    cropAndScale(
                            source,
                            targetWidth,
                            targetHeight,
                            zoom,
                            frontCamera
                    );

            replacePreview(
                    minecraft,
                    next
            );

            lastPreviewCaptureAt = now;
        } catch (Throwable ignored) {
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    public static void requestStillCapture() {
        stillCaptureRequested = true;
        completedStillPath = "";
    }

    public static String consumeCompletedStillPath() {
        String value =
                completedStillPath;

        completedStillPath = "";
        return value == null
                ? ""
                : value;
    }

    public static void releasePreview() {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (previewLocation != null) {
            minecraft.getTextureManager()
                    .release(
                            previewLocation
                    );
        } else if (previewTexture != null) {
            previewTexture.close();
        }

        previewTexture = null;
        previewLocation = null;
        previewImage = null;
        previewWidth = 0;
        previewHeight = 0;
        lastPreviewCaptureAt = 0L;
        stillCaptureRequested = false;
        completedStillPath = "";
    }

    private static String saveFullResolutionCrop(
            Minecraft minecraft,
            NativeImage source,
            int targetWidth,
            int targetHeight,
            int zoom,
            boolean frontCamera
    ) {
        int sourceWidth =
                source.getWidth();

        int sourceHeight =
                source.getHeight();

        double targetAspect =
                targetWidth
                        / (double) targetHeight;

        int cropWidth =
                sourceWidth;

        int cropHeight =
                Math.max(
                        1,
                        (int) Math.round(
                                cropWidth
                                        / targetAspect
                        )
                );

        if (cropHeight > sourceHeight) {
            cropHeight =
                    sourceHeight;

            cropWidth =
                    Math.max(
                            1,
                            (int) Math.round(
                                    cropHeight
                                            * targetAspect
                            )
                    );
        }

        int safeZoom =
                Math.max(
                        1,
                        Math.min(
                                4,
                                zoom
                        )
                );

        cropWidth =
                Math.max(
                        1,
                        cropWidth / safeZoom
                );

        cropHeight =
                Math.max(
                        1,
                        cropHeight / safeZoom
                );

        int cropX =
                Math.max(
                        0,
                        (sourceWidth - cropWidth)
                                / 2
                );

        int cropY =
                Math.max(
                        0,
                        (sourceHeight - cropHeight)
                                / 2
                );

        NativeImage still =
                new NativeImage(
                        cropWidth,
                        cropHeight,
                        false
                );

        try {
            source.resizeSubRectTo(
                    cropX,
                    cropY,
                    cropWidth,
                    cropHeight,
                    still
            );

            if (frontCamera) {
                mirrorHorizontally(
                        still
                );
            }

            Path directory =
                    minecraft.gameDirectory
                            .toPath()
                            .resolve("config")
                            .resolve("vsia")
                            .resolve("phone_photos");

            Files.createDirectories(
                    directory
            );

            Path file =
                    directory.resolve(
                            "IMG_"
                                    + System.currentTimeMillis()
                                    + ".png"
                    );

            still.writeToFile(
                    file
            );

            return file
                    .toAbsolutePath()
                    .normalize()
                    .toString();
        } catch (IOException ignored) {
            return "";
        } finally {
            still.close();
        }
    }

    private static NativeImage cropAndScale(
            NativeImage source,
            int targetWidth,
            int targetHeight,
            int zoom,
            boolean frontCamera
    ) {
        int sourceWidth =
                source.getWidth();

        int sourceHeight =
                source.getHeight();

        double targetAspect =
                targetWidth
                        / (double) targetHeight;

        int cropWidth =
                sourceWidth;

        int cropHeight =
                Math.max(
                        1,
                        (int) Math.round(
                                cropWidth
                                        / targetAspect
                        )
                );

        if (cropHeight > sourceHeight) {
            cropHeight =
                    sourceHeight;

            cropWidth =
                    Math.max(
                            1,
                            (int) Math.round(
                                    cropHeight
                                            * targetAspect
                            )
                    );
        }

        int safeZoom =
                Math.max(
                        1,
                        Math.min(
                                4,
                                zoom
                        )
                );

        cropWidth =
                Math.max(
                        1,
                        cropWidth / safeZoom
                );

        cropHeight =
                Math.max(
                        1,
                        cropHeight / safeZoom
                );

        int cropX =
                Math.max(
                        0,
                        (sourceWidth - cropWidth)
                                / 2
                );

        int cropY =
                Math.max(
                        0,
                        (sourceHeight - cropHeight)
                                / 2
                );

        NativeImage target =
                new NativeImage(
                        targetWidth,
                        targetHeight,
                        false
                );

        source.resizeSubRectTo(
                cropX,
                cropY,
                cropWidth,
                cropHeight,
                target
        );

        if (frontCamera) {
            mirrorHorizontally(
                    target
            );
        }

        return target;
    }

    private static void replacePreview(
            Minecraft minecraft,
            NativeImage image
    ) {
        int width =
                image.getWidth();

        int height =
                image.getHeight();

        if (previewTexture == null
                || previewLocation == null
                || previewWidth != width
                || previewHeight != height) {
            releasePreview();

            previewTexture =
                    new DynamicTexture(
                            width,
                            height,
                            false
                    );

            previewLocation =
                    minecraft.getTextureManager()
                            .register(
                                    "vsia_phone_camera_preview",
                                    previewTexture
                            );

            previewWidth = width;
            previewHeight = height;
        }

        NativeImage pixels =
                previewTexture.getPixels();

        if (pixels == null) {
            image.close();
            return;
        }

        image.resizeSubRectTo(
                0,
                0,
                width,
                height,
                pixels
        );

        image.close();

        previewImage = pixels;
        previewTexture.upload();
    }

    private static void mirrorHorizontally(
            NativeImage image
    ) {
        int width =
                image.getWidth();

        int height =
                image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0;
                 x < width / 2;
                 x++) {
                int otherX =
                        width - 1 - x;

                int left =
                        image.getPixelRGBA(
                                x,
                                y
                        );

                int right =
                        image.getPixelRGBA(
                                otherX,
                                y
                        );

                image.setPixelRGBA(
                        x,
                        y,
                        right
                );

                image.setPixelRGBA(
                        otherX,
                        y,
                        left
                );
            }
        }
    }
}
