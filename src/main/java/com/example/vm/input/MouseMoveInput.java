package com.example.vm.input;

public record MouseMoveInput(String computerId, int x, int y, int screenWidth, int screenHeight) implements VirtualMachineInput {
    public MouseMoveInput {
        KeyboardInput.requireComputerId(computerId);
        if (screenWidth < 1 || screenHeight < 1) {
            throw new IllegalArgumentException("screen dimensions must be positive");
        }
        if (x < 0 || y < 0 || x >= screenWidth || y >= screenHeight) {
            throw new IllegalArgumentException("mouse coordinates must be inside the screen");
        }
    }
}
