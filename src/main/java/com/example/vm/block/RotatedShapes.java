package com.example.vm.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RotatedShapes {
    private RotatedShapes() {
    }

    public static VoxelShape rotateHorizontal(VoxelShape northShape, Direction facing) {
        return switch (facing) {
            case EAST -> rotateOnce(northShape);
            case SOUTH -> rotateOnce(rotateOnce(northShape));
            case WEST -> rotateOnce(rotateOnce(rotateOnce(northShape)));
            default -> northShape;
        };
    }

    public static VoxelShape clipToBlock(VoxelShape shape) {
        VoxelShape[] clipped = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double clippedMinX = Math.max(0.0D, minX);
            double clippedMinY = Math.max(0.0D, minY);
            double clippedMinZ = Math.max(0.0D, minZ);
            double clippedMaxX = Math.min(1.0D, maxX);
            double clippedMaxY = Math.min(1.0D, maxY);
            double clippedMaxZ = Math.min(1.0D, maxZ);
            if (clippedMinX < clippedMaxX && clippedMinY < clippedMaxY && clippedMinZ < clippedMaxZ) {
                clipped[0] = Shapes.or(clipped[0], Shapes.box(
                        clippedMinX,
                        clippedMinY,
                        clippedMinZ,
                        clippedMaxX,
                        clippedMaxY,
                        clippedMaxZ
                ));
            }
        });
        return clipped[0].optimize();
    }

    private static VoxelShape rotateOnce(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                rotated[0] = Shapes.or(rotated[0], Block.box(
                        16.0D - maxZ * 16.0D,
                        minY * 16.0D,
                        minX * 16.0D,
                        16.0D - minZ * 16.0D,
                        maxY * 16.0D,
                        maxX * 16.0D
                ))
        );
        return rotated[0];
    }
}
