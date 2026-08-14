package com.example.vm.test;

import com.example.vm.computer.ComputerFormFactor;
import com.example.vm.computer.ComputerInteractionResolver;
import com.example.vm.computer.ComputerInteractionTarget;
import net.minecraft.core.Direction;

public final class ComputerInteractionResolverSelfTest {
    private ComputerInteractionResolverSelfTest() {
    }

    public static void main(String[] args) {
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.DESKTOP, Direction.NORTH, 0.20D, 0.70D, 0.05D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.DESKTOP, Direction.SOUTH, 0.80D, 0.70D, 0.95D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.DESKTOP, Direction.NORTH, 0.75D, 0.70D, 0.05D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.NORTH, -0.65D, 0.22D, 0.55D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.NORTH, 0.40D, 0.18D, 0.90D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.NORTH, 0.40D, 1.20D, 1.68D),
                ComputerInteractionTarget.DISPLAY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.NORTH, 1.70D, 0.22D, 0.55D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.EAST, 0.45D, 0.22D, -0.65D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.WEST, 0.55D, 0.22D, 1.65D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.SOUTH, 1.65D, 0.22D, 0.45D),
                ComputerInteractionTarget.POWER_BUTTON
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.SOUTH, 0.60D, 0.18D, 0.10D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.SOUTH, 0.60D, 1.20D, -0.68D),
                ComputerInteractionTarget.DISPLAY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.EAST, 0.10D, 0.18D, 0.40D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.EAST, -0.68D, 1.20D, 0.40D),
                ComputerInteractionTarget.DISPLAY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.WEST, 0.90D, 0.18D, 0.60D),
                ComputerInteractionTarget.BODY
        );
        assertTarget(
                ComputerInteractionResolver.resolve(ComputerFormFactor.LAPTOP, Direction.WEST, 1.68D, 1.20D, 0.60D),
                ComputerInteractionTarget.DISPLAY
        );
    }

    private static void assertTarget(ComputerInteractionTarget actual, ComputerInteractionTarget expected) {
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
