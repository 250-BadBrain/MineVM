package com.example.vm.block;

import com.example.vm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class ComputerFootprint {
    private ComputerFootprint() {
    }

    public static List<BlockPos> monitorOffsets(Direction facing) {
        return rectangleOffsets(facing, 3, 2, 1);
    }

    public static List<BlockPos> laptopOffsets(Direction facing) {
        return rectangleOffsets(facing, 3, 2, 2);
    }

    public static boolean canPlace(LevelAccessor level, BlockPos origin, List<BlockPos> offsets) {
        for (BlockPos offset : offsets) {
            BlockPos pos = origin.offset(offset);
            BlockState state = level.getBlockState(pos);
            if (!state.canBeReplaced() && !state.is(ModBlocks.COMPUTER_OCCUPANCY.get())) {
                return false;
            }
        }
        return true;
    }

    private static List<BlockPos> rectangleOffsets(Direction facing, int width, int height, int depth) {
        List<BlockPos> offsets = new ArrayList<>();
        int minX = -width / 2;
        int maxX = minX + width - 1;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = minX; x <= maxX; x++) {
                    BlockPos offset = rotate(facing, x, y, z);
                    if (!offset.equals(BlockPos.ZERO)) {
                        offsets.add(offset);
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }

    private static BlockPos rotate(Direction facing, int x, int y, int z) {
        return switch (facing) {
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case EAST -> new BlockPos(-z, y, x);
            default -> new BlockPos(x, y, z);
        };
    }
}
