package com.example.vm.vm;

public record VirtualMachineHandle(String computerId, VirtualMachineState state) {
    public VirtualMachineHandle {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}
