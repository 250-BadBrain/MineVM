package com.example.vm.input;

public record MouseButtonInput(String computerId, int button, boolean pressed) implements VirtualMachineInput {
    public MouseButtonInput {
        KeyboardInput.requireComputerId(computerId);
        if (button < 0 || button > 8) {
            throw new IllegalArgumentException("button must be between 0 and 8");
        }
    }
}
