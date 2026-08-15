package com.example.vm.input;

public record MouseWheelInput(String computerId, int steps) implements VirtualMachineInput {
    public MouseWheelInput {
        KeyboardInput.requireComputerId(computerId);
        if (steps == 0 || steps < -8 || steps > 8) {
            throw new IllegalArgumentException("wheel steps must be between -8 and 8, excluding 0");
        }
    }
}
