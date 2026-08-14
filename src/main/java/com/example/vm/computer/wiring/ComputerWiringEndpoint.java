package com.example.vm.computer.wiring;

import net.minecraft.core.BlockPos;

public record ComputerWiringEndpoint(BlockPos pos, ComputerPortType portType) {
    public ComputerWiringEndpoint {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        if (portType == null) {
            throw new IllegalArgumentException("portType must not be null");
        }
    }
}
