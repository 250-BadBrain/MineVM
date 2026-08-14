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
        java.lang.reflect.Field cursorSurfaceZField = renderer.getDeclaredField("CURSOR_SURFACE_Z");
        cursorSurfaceZField.setAccessible(true);

        assertClose((float) widthField.get(null), 200.0F);
        assertClose((float) heightField.get(null), 107.0F);
        assertClose((float) laptopWidthField.get(null), 210.0F);
        assertClose((float) laptopHeightField.get(null), 110.0F);
        if ((float) cursorSurfaceZField.get(null) >= -0.01F) {
            throw new AssertionError("Cursor must render in front of the duplicated VM screen face");
        }

        java.lang.reflect.Method cursorPosition = renderer.getDeclaredMethod("cursorPosition", int.class, int.class, int.class, int.class, float.class, float.class);
        cursorPosition.setAccessible(true);
        float[] topLeft = (float[]) cursorPosition.invoke(null, 0, 0, 1280, 800, 200.0F, 125.0F);
        assertClose(topLeft[0], 0.0F);
        assertClose(topLeft[1], 0.0F);
        float[] bottomRight = (float[]) cursorPosition.invoke(null, 1279, 799, 1280, 800, 200.0F, 125.0F);
        assertClose(bottomRight[0], 200.0F);
        assertClose(bottomRight[1], 125.0F);
    }

    private static void assertClose(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.01F) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
