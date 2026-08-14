package com.example.vm.test;

import com.example.vm.input.KeyboardInput;
import com.example.vm.input.MouseButtonInput;
import com.example.vm.input.MouseMoveInput;

public final class VirtualMachineInputSelfTest {
    private VirtualMachineInputSelfTest() {
    }

    public static void main(String[] args) {
        KeyboardInput keyboardInput = new KeyboardInput("computer-test", "a", true);
        assertEquals(keyboardInput.computerId(), "computer-test");
        assertEquals(keyboardInput.key(), "a");

        MouseButtonInput buttonInput = new MouseButtonInput("computer-test", 1, false);
        assertEquals(buttonInput.button(), 1);

        MouseMoveInput moveInput = new MouseMoveInput("computer-test", 100, 200, 1024, 768);
        assertEquals(moveInput.x(), 100);
        assertEquals(moveInput.y(), 200);

        assertRejected(() -> new KeyboardInput("", "a", true));
        assertRejected(() -> new KeyboardInput("computer-test", "", true));
        assertRejected(() -> new MouseButtonInput("computer-test", 9, true));
        assertRejected(() -> new MouseMoveInput("computer-test", 1024, 0, 1024, 768));
    }

    private static void assertRejected(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
