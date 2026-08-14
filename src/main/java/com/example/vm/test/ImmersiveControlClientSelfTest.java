package com.example.vm.test;

import org.lwjgl.glfw.GLFW;

public final class ImmersiveControlClientSelfTest {
    private ImmersiveControlClientSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> client = Class.forName("com.example.vm.client.ImmersiveControlClient");
        java.lang.reflect.Method qcode = client.getDeclaredMethod("qcode", int.class, int.class);
        qcode.setAccessible(true);

        assertEquals((String) qcode.invoke(null, GLFW.GLFW_KEY_W, GLFW.GLFW_MOD_CONTROL), "ctrl-w");
        assertEquals((String) qcode.invoke(null, GLFW.GLFW_KEY_F4, GLFW.GLFW_MOD_ALT), "alt-f4");
        assertEquals((String) qcode.invoke(null, GLFW.GLFW_KEY_T, GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT), "ctrl-shift-t");
        assertEquals((String) qcode.invoke(null, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_MOD_SHIFT), "shift-dot");
    }

    private static void assertEquals(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
