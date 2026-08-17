package com.example.vm.computer;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.config.ComputerRuntimePreflight;
import com.example.vm.network.ModNetwork;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.concurrent.CompletionException;

public final class ComputerBiosActions {
    private ComputerBiosActions() {
    }

    public static void handle(ServerPlayer player, BlockPos pos, ComputerBiosAction action) {
        if (player == null || pos == null || action == null) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof ComputerBlockEntity computer)) {
            player.sendSystemMessage(Component.literal("No computer found at " + pos.toShortString()));
            return;
        }

        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | BIOS action denied: too far away"));
            return;
        }

        switch (action) {
            case BOOT_FROM_DISC -> bootFromDisc(player, pos, computer);
            case BOOT_VM_DISK -> bootVmDisk(player, pos, computer);
            case POWER_OFF -> powerOff(player, pos, computer);
            case EJECT_DISC -> ComputerDiscActions.ejectIfStopped(player.getServer(), player, computer, pos);
        }
    }

    private static void bootFromDisc(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer) {
        computer.insertedDisc()
                .ifPresentOrElse(
                        guestOperatingSystem -> createAndStartFromDisc(player, pos, computer, guestOperatingSystem),
                        () -> player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | No system disc inserted"))
                );
    }

    private static void createAndStartFromDisc(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer, GuestOperatingSystem guestOperatingSystem) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String computerId = computer.getComputerId();
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer, guestOperatingSystem);
        String isoPath = ComputerRuntimeConfig.isoPathFor(guestOperatingSystem);
        java.util.List<String> preflightErrors = ComputerRuntimePreflight.validateSystemDiscBoot(guestOperatingSystem);
        if (!preflightErrors.isEmpty()) {
            player.sendSystemMessage(Component.literal("Computer " + computerId + " | Disc boot blocked: " + preflightErrors.get(0)));
            return;
        }

        ensureRegistered(computer)
                .thenCompose(ignored -> ComputerInMinecraft.VM_MANAGER.getState(computerId))
                .whenComplete((state, throwable) -> {
                    if (throwable != null) {
                        send(server, player, "Computer " + computerId + " | Disc boot failed: " + rootMessage(throwable));
                        return;
                    }

                    if (isRunningOrStarting(state)) {
                        send(server, player, "Computer " + computerId + " | Already running | State: " + state);
                    } else if (state == VirtualMachineState.NOT_CREATED) {
                        computer.rememberDiskGuestOperatingSystem(guestOperatingSystem);
                        createThenStart(server, player, pos, descriptor, guestOperatingSystem, VirtualMachineBootRequest.systemDisc(computerId, isoPath), "Booting from " + guestOperatingSystem + " disc", false);
                    } else if (state == VirtualMachineState.STOPPED) {
                        computer.rememberDiskGuestOperatingSystem(guestOperatingSystem);
                        start(server, player, pos, guestOperatingSystem, VirtualMachineBootRequest.systemDisc(computerId, isoPath), "Booting from inserted disc", false);
                    } else {
                        send(server, player, "Computer " + computerId + " | VM busy | State: " + state);
                    }
                });
    }

    private static void bootVmDisk(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String computerId = computer.getComputerId();
        ensureRegistered(computer)
                .thenCompose(ignored -> ComputerInMinecraft.VM_MANAGER.getState(computerId))
                .whenComplete((state, throwable) -> {
                    if (throwable != null) {
                        send(server, player, "Computer " + computerId + " | Disk boot failed: " + rootMessage(throwable));
                        return;
                    }

                    if (state == VirtualMachineState.NOT_CREATED) {
                        if (!ComputerRuntimeConfig.vmDiskExists(computer)) {
                            send(server, player, "Computer " + computerId + " | VM disk is not created. Boot from a system disc first.");
                            return;
                        }
                        GuestOperatingSystem selectedGuestOperatingSystem = selectedGuestOperatingSystem(computer);
                        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer, selectedGuestOperatingSystem);
                        computer.setActiveGuestOperatingSystem(selectedGuestOperatingSystem);
                        createThenStart(server, player, pos, descriptor, selectedGuestOperatingSystem, bootDiskRequest(computer), "Booting from VM disk", true);
                    } else if (state == VirtualMachineState.STOPPED) {
                        start(server, player, pos, selectedGuestOperatingSystem(computer), bootDiskRequest(computer), "Booting from VM disk", true);
                    } else if (state == VirtualMachineState.ERROR && ComputerRuntimeConfig.vmDiskExists(computer)) {
                        GuestOperatingSystem selectedGuestOperatingSystem = selectedGuestOperatingSystem(computer);
                        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer, selectedGuestOperatingSystem);
                        computer.setActiveGuestOperatingSystem(selectedGuestOperatingSystem);
                        createThenStart(server, player, pos, descriptor, selectedGuestOperatingSystem, bootDiskRequest(computer), "Recovering and booting from VM disk", true);
                    } else if (isRunningOrStarting(state)) {
                        send(server, player, "Computer " + computerId + " | Already running | State: " + state);
                    } else {
                        send(server, player, "Computer " + computerId + " | VM busy | State: " + state);
                    }
                });
    }

    private static GuestOperatingSystem selectedGuestOperatingSystem(ComputerBlockEntity computer) {
        return computer.selectedGuestOperatingSystem().orElseGet(ComputerRuntimeConfig::configuredGuestOperatingSystem);
    }

    private static VirtualMachineBootRequest bootDiskRequest(ComputerBlockEntity computer) {
        return computer.insertedDisc()
                .map(ComputerRuntimeConfig::isoPathFor)
                .filter(isoPath -> !isoPath.isBlank())
                .map(isoPath -> VirtualMachineBootRequest.vmDiskWithAttachedDisc(computer.getComputerId(), isoPath))
                .orElseGet(() -> VirtualMachineBootRequest.vmDisk(computer.getComputerId()));
    }

    private static void powerOff(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ComputerPowerActions.powerOff(server, player, pos, computer);
    }

    private static void createThenStart(MinecraftServer server, ServerPlayer player, BlockPos pos, VirtualMachineDescriptor descriptor, GuestOperatingSystem guestOperatingSystem, VirtualMachineBootRequest bootRequest, String actionLabel, boolean persistGuest) {
        ComputerInMinecraft.VM_MANAGER.create(descriptor)
                .thenCompose(handle -> ComputerInMinecraft.VM_MANAGER.start(bootRequest))
                .whenComplete((handle, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        setComputerVmDisplay(player, pos, false);
                        player.sendSystemMessage(Component.literal("Computer " + descriptor.computerId() + " | " + actionLabel + " failed: " + rootMessage(throwable)));
                        return;
                    }
                    setComputerVmDisplay(player, pos, true, guestOperatingSystem, persistGuest);
                    ModNetwork.updateScreenControlMode(player, handle.computerId(), true);
                    player.sendSystemMessage(Component.literal("Computer " + handle.computerId() + " | " + actionLabel + " | State: " + handle.state()));
                }));
    }

    private static void start(MinecraftServer server, ServerPlayer player, BlockPos pos, GuestOperatingSystem guestOperatingSystem, String computerId, String actionLabel, boolean persistGuest) {
        ComputerInMinecraft.VM_MANAGER.start(computerId)
                .whenComplete((handle, throwable) -> sendHandle(server, player, pos, computerId, guestOperatingSystem, actionLabel, handle, throwable, true, persistGuest));
    }

    private static void start(MinecraftServer server, ServerPlayer player, BlockPos pos, GuestOperatingSystem guestOperatingSystem, VirtualMachineBootRequest bootRequest, String actionLabel, boolean persistGuest) {
        ComputerInMinecraft.VM_MANAGER.start(bootRequest)
                .whenComplete((handle, throwable) -> sendHandle(server, player, pos, bootRequest.computerId(), guestOperatingSystem, actionLabel, handle, throwable, true, persistGuest));
    }

    private static void sendHandle(MinecraftServer server, ServerPlayer player, BlockPos pos, String computerId, GuestOperatingSystem guestOperatingSystem, String actionLabel, VirtualMachineHandle handle, Throwable throwable, boolean vmDisplayActive, boolean persistGuest) {
        server.execute(() -> {
            if (throwable != null) {
                player.sendSystemMessage(Component.literal("Computer " + computerId + " | " + actionLabel + " failed: " + rootMessage(throwable)));
                return;
            }
            setComputerVmDisplay(player, pos, vmDisplayActive, guestOperatingSystem, persistGuest);
            if (vmDisplayActive) {
                ModNetwork.updateScreenControlMode(player, handle.computerId(), true);
            }
            player.sendSystemMessage(Component.literal("Computer " + handle.computerId() + " | " + actionLabel + " | State: " + handle.state()));
        });
    }

    private static void setComputerVmDisplay(ServerPlayer player, BlockPos pos, boolean active) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof ComputerBlockEntity computer) {
            computer.setVmDisplayActive(active);
        }
    }

    private static void setComputerVmDisplay(ServerPlayer player, BlockPos pos, boolean active, GuestOperatingSystem guestOperatingSystem, boolean persistGuest) {
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof ComputerBlockEntity computer) {
            if (persistGuest) {
                computer.setActiveGuestOperatingSystem(guestOperatingSystem);
            }
            computer.setVmDisplayActive(active);
        }
    }

    private static java.util.concurrent.CompletableFuture<VirtualMachineHandle> ensureRegistered(ComputerBlockEntity computer) {
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer);
        if (!ComputerRuntimeConfig.vmDiskExists(computer)) {
            return java.util.concurrent.CompletableFuture.completedFuture(new VirtualMachineHandle(computer.getComputerId(), VirtualMachineState.NOT_CREATED));
        }
        return ComputerInMinecraft.VM_MANAGER.create(descriptor);
    }

    private static void send(MinecraftServer server, ServerPlayer player, String message) {
        server.execute(() -> player.sendSystemMessage(Component.literal(message)));
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
