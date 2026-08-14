package com.example.vm.client;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.screen.ScreenFrame;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ScreenFrameTextureCache {
    static final int MAX_TEXTURE_WIDTH = 1280;
    static final long MIN_UPLOAD_INTERVAL_NANOS = 33_000_000L;
    private static final Map<String, CachedScreenTexture> TEXTURES = new HashMap<>();

    private ScreenFrameTextureCache() {
    }

    public static Optional<ResourceLocation> updateAndGet(String computerId, ScreenFrame frame) {
        if (computerId == null || computerId.isBlank() || frame == null) {
            return Optional.empty();
        }

        CachedScreenTexture cachedTexture = TEXTURES.get(computerId);
        int textureWidth = scaledTextureWidth(frame.width());
        int textureHeight = scaledTextureHeight(frame.width(), frame.height(), textureWidth);
        if (cachedTexture == null || cachedTexture.sourceWidth != frame.width() || cachedTexture.sourceHeight != frame.height() || cachedTexture.width != textureWidth || cachedTexture.height != textureHeight) {
            if (cachedTexture != null) {
                cachedTexture.close();
            }
            cachedTexture = CachedScreenTexture.create(computerId, frame.width(), frame.height(), textureWidth, textureHeight);
            TEXTURES.put(computerId, cachedTexture);
        }

        cachedTexture.apply(frame);
        return Optional.of(cachedTexture.location);
    }

    public static void clear(String computerId) {
        CachedScreenTexture texture = TEXTURES.remove(computerId);
        if (texture != null) {
            texture.close();
        }
    }

    public static void clearAll() {
        TEXTURES.values().forEach(CachedScreenTexture::close);
        TEXTURES.clear();
    }

    private static final class CachedScreenTexture {
        private final int width;
        private final int height;
        private final int sourceWidth;
        private final int sourceHeight;
        private final int[] sourcePixels;
        private final DynamicTexture texture;
        private final ResourceLocation location;
        private long lastUploadNanos;

        private CachedScreenTexture(int sourceWidth, int sourceHeight, int width, int height, DynamicTexture texture, ResourceLocation location) {
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            this.width = width;
            this.height = height;
            this.sourcePixels = new int[sourceWidth * sourceHeight];
            this.texture = texture;
            this.location = location;
        }

        private static CachedScreenTexture create(String computerId, int sourceWidth, int sourceHeight, int width, int height) {
            DynamicTexture texture = new DynamicTexture(width, height, true);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register(
                    ComputerInMinecraft.MOD_ID + "/vm_screen/" + safeTextureId(computerId),
                    texture
            );
            return new CachedScreenTexture(sourceWidth, sourceHeight, width, height, texture, location);
        }

        private void apply(ScreenFrame frame) {
            long now = System.nanoTime();
            if (lastUploadNanos != 0L && now - lastUploadNanos < MIN_UPLOAD_INTERVAL_NANOS) {
                return;
            }

            NativeImage pixels = texture.getPixels();
            if (pixels == null) {
                return;
            }

            mergeChangedFrame(frame);

            for (int targetY = 0; targetY < height; targetY++) {
                int sourceY = clamp(mapFloor(targetY, height, sourceHeight), 0, sourceHeight - 1);
                for (int targetX = 0; targetX < width; targetX++) {
                    int sourceX = clamp(mapFloor(targetX, width, sourceWidth), 0, sourceWidth - 1);
                    pixels.setPixelRGBA(targetX, targetY, toNativeRgba(sourcePixels[sourceY * sourceWidth + sourceX]));
                }
            }
            texture.upload();
            lastUploadNanos = now;
        }

        private void mergeChangedFrame(ScreenFrame frame) {
            int[] argbPixels = frame.argbPixels();
            for (int y = 0; y < frame.changedHeight(); y++) {
                int sourceOffset = (frame.changedY() + y) * sourceWidth + frame.changedX();
                int changedOffset = y * frame.changedWidth();
                System.arraycopy(argbPixels, changedOffset, sourcePixels, sourceOffset, frame.changedWidth());
            }
        }

        private void close() {
            texture.close();
            Minecraft.getInstance().getTextureManager().release(location);
        }
    }

    static int toNativeRgba(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        return (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    static int scaledTextureWidth(int sourceWidth) {
        return Math.max(1, Math.min(MAX_TEXTURE_WIDTH, sourceWidth));
    }

    static int scaledTextureHeight(int sourceWidth, int sourceHeight, int textureWidth) {
        return Math.max(1, Math.round((float) sourceHeight * textureWidth / sourceWidth));
    }

    private static int mapFloor(int value, int sourceSize, int targetSize) {
        return Math.floorDiv(value * targetSize, sourceSize);
    }

    private static int mapCeil(int value, int sourceSize, int targetSize) {
        return Math.floorDiv(value * targetSize + sourceSize - 1, sourceSize);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String safeTextureId(String computerId) {
        return computerId.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_/.-]", "_");
    }
}
