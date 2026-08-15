package com.example.vm.input;

public sealed interface VirtualMachineInput permits KeyboardInput, MouseButtonInput, MouseMoveInput, MouseWheelInput {
    String computerId();
}
