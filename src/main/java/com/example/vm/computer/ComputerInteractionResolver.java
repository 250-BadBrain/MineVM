package com.example.vm.computer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ComputerInteractionResolver {
    private ComputerInteractionResolver() {
    }

    public static ComputerInteractionTarget resolve(ComputerFormFactor formFactor, Direction facing, BlockPos pos, BlockHitResult hit) {
        return resolveWorldHit(formFactor, facing, pos, hit.getLocation());
    }

    public static ComputerInteractionTarget resolveWorldHit(ComputerFormFactor formFactor, Direction facing, BlockPos origin, Vec3 hitLocation) {
        Vec3 localHit = hitLocation.subtract(origin.getX(), origin.getY(), origin.getZ());
        return resolve(formFactor, facing, localHit.x, localHit.y, localHit.z);
    }

    public static ComputerInteractionTarget resolve(ComputerFormFactor formFactor, Direction facing, double worldX, double y, double worldZ) {
        double localX = localX(facing, worldX, worldZ);
        double localZ = localZ(facing, worldX, worldZ);

        if (formFactor == ComputerFormFactor.LAPTOP) {
            return resolveLaptop(localX, y, localZ);
        }

        return resolveDesktop(localX, y, localZ);
    }

    private static ComputerInteractionTarget resolveDesktop(double x, double y, double z) {
        boolean frontPanel = z <= 0.22D;
        boolean powerButton = x >= 0.10D && x <= 0.32D && y >= 0.62D && y <= 0.84D;
        if (frontPanel && powerButton) {
            return ComputerInteractionTarget.POWER_BUTTON;
        }

        return ComputerInteractionTarget.BODY;
    }

    private static ComputerInteractionTarget resolveLaptop(double x, double y, double z) {
        boolean deckOrPalmRest = inBox(x, y, z, -16.0D, 0.0D, 2.0D, 32.0D, 3.0D, 30.0D);
        boolean keyboard = inBox(x, y, z, -8.0D, 3.1D, 8.0D, 20.0D, 3.5D, 20.0D);
        boolean powerButton = inBox(x, y, z, -13.0D, 3.2D, 7.0D, -8.0D, 4.0D, 11.0D);
        if (powerButton) {
            return ComputerInteractionTarget.POWER_BUTTON;
        }
        boolean screenPanel = inBox(x, y, z, -13.0D, 8.0D, 26.0D, 29.0D, 30.0D, 30.5D);
        if (screenPanel) {
            return ComputerInteractionTarget.DISPLAY;
        }
        if (deckOrPalmRest || keyboard) {
            return ComputerInteractionTarget.BODY;
        }

        return ComputerInteractionTarget.BODY;
    }

    private static boolean inBox(double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return x >= minX / 16.0D && x <= maxX / 16.0D
                && y >= minY / 16.0D && y <= maxY / 16.0D
                && z >= minZ / 16.0D && z <= maxZ / 16.0D;
    }

    private static double localX(Direction facing, double x, double z) {
        return switch (facing) {
            case SOUTH -> 1.0D - x;
            case WEST -> 1.0D - z;
            case EAST -> z;
            default -> x;
        };
    }

    private static double localZ(Direction facing, double x, double z) {
        return switch (facing) {
            case SOUTH -> 1.0D - z;
            case WEST -> x;
            case EAST -> 1.0D - x;
            default -> z;
        };
    }
}
