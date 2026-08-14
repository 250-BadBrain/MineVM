package com.example.vm.test;

public final class ScreenFrameTextureCacheSelfTest {
    private ScreenFrameTextureCacheSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> textureCache = Class.forName("com.example.vm.client.ScreenFrameTextureCache");
        java.lang.reflect.Method method = textureCache.getDeclaredMethod("toNativeRgba", int.class);
        method.setAccessible(true);
        int nativeRgba = (int) method.invoke(null, 0xAA336699);
        if (nativeRgba != 0xAA996633) {
            throw new AssertionError("Unexpected native RGBA conversion: " + Integer.toHexString(nativeRgba));
        }

        java.lang.reflect.Method widthMethod = textureCache.getDeclaredMethod("scaledTextureWidth", int.class);
        widthMethod.setAccessible(true);
        java.lang.reflect.Method heightMethod = textureCache.getDeclaredMethod("scaledTextureHeight", int.class, int.class, int.class);
        heightMethod.setAccessible(true);
        int textureWidth = (int) widthMethod.invoke(null, 1280);
        int textureHeight = (int) heightMethod.invoke(null, 1280, 720, textureWidth);
        if (textureWidth != 1280 || textureHeight != 720) {
            throw new AssertionError("Unexpected scaled texture size: " + textureWidth + "x" + textureHeight);
        }
    }
}
