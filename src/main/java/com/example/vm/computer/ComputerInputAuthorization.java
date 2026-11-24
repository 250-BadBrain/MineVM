package com.example.vm.computer;

import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Server-side authority check for client-originated VM input. */
public final class ComputerInputAuthorization {
    private static final double MAX_CONTROL_DISTANCE_SQUARED = 64.0D;

    private ComputerInputAuthorization() {
    }

    public static boolean allows(ServerPlayer player, BlockPos screenPos, String computerId) {
        if (player == null || screenPos == null || computerId == null || computerId.isBlank()) {
            return false;
        }
        if (player.distanceToSqr(Vec3.atCenterOf(screenPos)) > MAX_CONTROL_DISTANCE_SQUARED) {
            return false;
        }

        BlockEntity screen = player.level().getBlockEntity(screenPos);
        if (screen instanceof ComputerBlockEntity computer) {
            return player.level().getBlockState(screenPos).is(ModBlocks.LAPTOP_COMPUTER.get())
                    && computer.poweredOn()
                    && computer.canControl(player)
                    && computer.getComputerId().equals(computerId);
        }
        if (!(screen instanceof ComputerMonitorBlockEntity monitor)
                || !monitor.displayPowered()
                || monitor.linkedComputerPos().isEmpty()
                || !player.level().hasChunkAt(monitor.linkedComputerPos().get())) {
            return false;
        }

        BlockEntity linkedEntity = player.level().getBlockEntity(monitor.linkedComputerPos().get());
        return linkedEntity instanceof ComputerBlockEntity computer
                && computer.poweredOn()
                && computer.canControl(player)
                && computer.hasDesktopInputDevices()
                && monitor.linkedComputerId().filter(computerId::equals).isPresent()
                && computer.getComputerId().equals(computerId);
    }
}
