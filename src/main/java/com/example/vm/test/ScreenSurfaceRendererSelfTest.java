package com.example.vm.test;

public final class ScreenSurfaceRendererSelfTest {
    private ScreenSurfaceRendererSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> renderer = Class.forName("com.example.vm.client.ScreenSurfaceRenderer");
        java.lang.reflect.Field widthField = renderer.getDeclaredField("VM_SCREEN_WIDTH");
        widthField.setAccessible(true);
        java.lang.reflect.Field heightField = renderer.getDeclaredField("VM_SCREEN_HEIGHT");
        heightField.setAccessible(true);
        java.lang.reflect.Field laptopWidthField = renderer.getDeclaredField("LAPTOP_VM_SCREEN_WIDTH");
        laptopWidthField.setAccessible(true);
        java.lang.reflect.Field laptopHeightField = renderer.getDeclaredField("LAPTOP_VM_SCREEN_HEIGHT");
        laptopHeightField.setAccessible(true);

        assertClose((float) widthField.get(null), 200.0F);
        assertClose((float) heightField.get(null), 107.0F);
        assertClose((float) laptopWidthField.get(null), 210.0F);
        assertClose((float) laptopHeightField.get(null), 110.0F);
    }

    private static void assertClose(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.01F) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
