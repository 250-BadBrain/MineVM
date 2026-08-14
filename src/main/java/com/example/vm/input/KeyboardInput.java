package com.example.vm.input;

public record KeyboardInput(String computerId, String key, boolean pressed) implements VirtualMachineInput {
    public KeyboardInput {
        requireComputerId(computerId);
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }

    static void requireComputerId(String computerId) {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
    }
}
