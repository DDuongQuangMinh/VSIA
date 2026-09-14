package com.k1ngtle.vsia.phone.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class PhonePhotoTextureCache {
    private static final Map<Long, ResourceLocation>
            LOCATIONS =
            new HashMap<>();

    private static final Map<Long, DynamicTexture>
            TEXTURES =
            new HashMap<>();

    private PhonePhotoTextureCache() {
    }

    public static ResourceLocation textureFor(
            PhonePersonalAppsState.Photo photo
    ) {
        if (photo == null
                || photo.imagePath() == null
                || photo.imagePath().isBlank()) {
            return null;
        }

        ResourceLocation cached =
                LOCATIONS.get(
                        photo.id()
                );

        if (cached != null) {
            return cached;
        }

        Path path =
                Path.of(
                        photo.imagePath()
                );

        if (!Files.isRegularFile(path)) {
            return null;
        }

        try (InputStream input =
                     Files.newInputStream(path)) {
            NativeImage image =
                    NativeImage.read(
                            input
                    );

            DynamicTexture texture =
                    new DynamicTexture(
                            image
                    );

            ResourceLocation location =
                    Minecraft.getInstance()
                            .getTextureManager()
                            .register(
                                    "vsia_phone_photo_"
                                            + photo.id(),
                                    texture
                            );

            LOCATIONS.put(
                    photo.id(),
                    location
            );

            TEXTURES.put(
                    photo.id(),
                    texture
            );

            return location;
        } catch (IOException ignored) {
            return null;
        }
    }

    public static int textureWidth(long photoId) {
        DynamicTexture texture =
                TEXTURES.get(
                        photoId
                );

        if (texture == null
                || texture.getPixels() == null) {
            return 1;
        }

        return texture.getPixels()
                .getWidth();
    }

    public static int textureHeight(long photoId) {
        DynamicTexture texture =
                TEXTURES.get(
                        photoId
                );

        if (texture == null
                || texture.getPixels() == null) {
            return 1;
        }

        return texture.getPixels()
                .getHeight();
    }

    public static void release(long photoId) {
        ResourceLocation location =
                LOCATIONS.remove(
                        photoId
                );

        DynamicTexture texture =
                TEXTURES.remove(
                        photoId
                );

        if (location != null) {
            Minecraft.getInstance()
                    .getTextureManager()
                    .release(
                            location
                    );
        } else if (texture != null) {
            texture.close();
        }
    }

    public static void clear() {
        for (Long photoId :
                LOCATIONS.keySet()
                        .stream()
                        .toList()) {
            release(photoId);
        }

        LOCATIONS.clear();
        TEXTURES.clear();
    }
}
