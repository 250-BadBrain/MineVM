package com.example.vm.test;

import com.example.vm.block.ComputerFootprint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ComputerFootprintSelfTest {
    private ComputerFootprintSelfTest() {
    }

    public static void main(String[] args) {
        assertFootprint("monitor north", ComputerFootprint.monitorOffsets(Direction.NORTH), 5);
        assertFootprint("laptop north", ComputerFootprint.laptopOffsets(Direction.NORTH), 11);
        assertContains(ComputerFootprint.monitorOffsets(Direction.NORTH), new BlockPos(-1, 1, 0));
        assertContains(ComputerFootprint.monitorOffsets(Direction.NORTH), new BlockPos(1, 1, 0));
        assertContains(ComputerFootprint.monitorOffsets(Direction.EAST), new BlockPos(0, 1, -1));
        assertContains(ComputerFootprint.monitorOffsets(Direction.EAST), new BlockPos(0, 1, 1));
        assertContains(ComputerFootprint.laptopOffsets(Direction.NORTH), new BlockPos(-1, 1, 1));
        assertContains(ComputerFootprint.laptopOffsets(Direction.NORTH), new BlockPos(1, 1, 1));
        assertContains(ComputerFootprint.laptopOffsets(Direction.WEST), new BlockPos(1, 1, -1));
    }

    private static void assertFootprint(String label, List<BlockPos> offsets, int expectedSize) {
        if (offsets.size() != expectedSize) {
            throw new AssertionError(label + " expected " + expectedSize + " offsets but got " + offsets.size() + ": " + offsets);
        }
        Set<BlockPos> uniqueOffsets = new HashSet<>(offsets);
        if (uniqueOffsets.size() != offsets.size()) {
            throw new AssertionError(label + " contains duplicate offsets: " + offsets);
        }
        if (uniqueOffsets.contains(BlockPos.ZERO)) {
            throw new AssertionError(label + " must not include origin");
        }
    }

    private static void assertContains(List<BlockPos> offsets, BlockPos expected) {
        if (!offsets.contains(expected)) {
            throw new AssertionError("Expected offsets to contain " + expected + " but got " + offsets);
        }
    }
}
