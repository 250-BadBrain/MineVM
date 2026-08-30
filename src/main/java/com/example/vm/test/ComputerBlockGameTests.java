package com.example.vm.test;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.registry.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder(MineVM.MOD_ID)
public final class ComputerBlockGameTests {
    private ComputerBlockGameTests() {
    }

    @GameTest(template = "empty")
    public static void computerBlockCanBePlaced(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);

        helper.setBlock(pos, ModBlocks.COMPUTER_BLOCK.get());
        helper.assertBlockPresent(ModBlocks.COMPUTER_BLOCK.get(), pos);

        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (!(blockEntity instanceof ComputerBlockEntity computerBlockEntity)) {
            helper.fail("Expected computer block entity at " + pos);
            return;
        }

        if (computerBlockEntity.getComputerId().isBlank()) {
            helper.fail("Expected generated computer id");
            return;
        }

        if (computerBlockEntity.getHardwareConfiguration().cpuCores() != 4) {
            helper.fail("Expected default CPU core count");
            return;
        }

        helper.succeed();
    }
}
