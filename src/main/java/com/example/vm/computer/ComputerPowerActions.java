package com.example.vm.computer;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.network.ModNetwork;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.concurrent.CompletionException;

public final class ComputerPowerActions {
    private ComputerPowerActions() {
    }

    public static void powerOff(MinecraftServer server, Player player, BlockPos pos, ComputerBlockEntity computer) {
        if (server == null || player == null || pos == null || computer == null) {
            return;
        }

        String computerId = computer.getComputerId();
        computer.setPowerTransitioning(true);
        computer.setPoweredOn(false);
        computer.setVmDisplayActive(false);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModNetwork.stopScreenControl(serverPlayer, computerId);
        }
        ensureRegistered(computer)
                .thenCompose(ignored -> ComputerInMinecraft.VM_MANAGER.getState(computerId))
                .whenComplete((state, throwable) -> {
                    if (throwable != null) {
                        server.execute(() -> computer.setPowerTransitioning(false));
                        send(server, player, "Computer " + computerId + " | Power off failed: " + rootMessage(throwable));
                        return;
                    }

                    if (isRunningOrStarting(state)) {
                        stop(server, player, pos, computerId, "Power off");
                    } else if (state == VirtualMachineState.STOPPING) {
                        send(server, player, "Computer " + computerId + " | Power off already in progress");
                    } else {
                        server.execute(() -> computer.setPowerTransitioning(false));
                        send(server, player, "Computer " + computerId + " | Power off | VM: " + state);
                    }
                });
    }

    public static void markPoweredOff(Player player, BlockPos pos) {
        if (player == null || pos == null) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof ComputerBlockEntity computer) {
            computer.setPoweredOn(false);
            computer.setVmDisplayActive(false);
            computer.setPowerTransitioning(false);
        }
    }

    private static void stop(MinecraftServer server, Player player, BlockPos pos, String computerId, String actionLabel) {
        ComputerInMinecraft.VM_MANAGER.stop(computerId)
                .whenComplete((handle, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        player.sendSystemMessage(Component.literal("Computer " + computerId + " | " + actionLabel + " failed: " + rootMessage(throwable)));
                        return;
                    }
                    markPoweredOff(player, pos);
                    VirtualMachineHandle stoppedHandle = handle;
                    player.sendSystemMessage(Component.literal("Computer " + stoppedHandle.computerId() + " | " + actionLabel + " | State: " + stoppedHandle.state()));
                }));
    }

    private static void send(MinecraftServer server, Player player, String message) {
        server.execute(() -> player.sendSystemMessage(Component.literal(message)));
    }

    private static java.util.concurrent.CompletableFuture<VirtualMachineHandle> ensureRegistered(ComputerBlockEntity computer) {
        if (!com.example.vm.config.ComputerRuntimeConfig.vmDiskExists(computer)) {
            return java.util.concurrent.CompletableFuture.completedFuture(new VirtualMachineHandle(computer.getComputerId(), VirtualMachineState.NOT_CREATED));
        }
        return ComputerInMinecraft.VM_MANAGER.create(com.example.vm.config.ComputerRuntimeConfig.descriptorFor(computer));
    }

    private static boolean isRunningOrStarting(VirtualMachineState state) {
        return state == VirtualMachineState.RUNNING || state == VirtualMachineState.STARTING;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
