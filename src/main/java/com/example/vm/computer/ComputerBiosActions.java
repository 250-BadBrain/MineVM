package com.example.vm.computer;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.FloppyDriveBlockEntity;
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
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D
                || !player.level().hasChunkAt(pos)) {
            player.sendSystemMessage(Component.literal("BIOS action denied: computer is too far away or unloaded."));
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof ComputerBlockEntity computer)) {
            player.sendSystemMessage(Component.literal("No computer found at " + pos.toShortString()));
            return;
        }
        if (!computer.canManage(player)) {
            player.sendSystemMessage(Component.translatable("message.minevm.not_owner"));
            return;
        }
        computer.claimOwner(player.getUUID());

        if ((action == ComputerBiosAction.BOOT_FROM_DISC || action == ComputerBiosAction.BOOT_VM_DISK)
                && (!computer.poweredOn() || computer.powerTransitioning())) {
            player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Power on the computer first."));
            return;
        }
        if ((action == ComputerBiosAction.BOOT_FROM_DISC || action == ComputerBiosAction.BOOT_VM_DISK)
                && computer.linkedFloppyDrivePos().filter(player.level()::hasChunkAt)
                .map(player.level()::getBlockEntity)
                .filter(FloppyDriveBlockEntity.class::isInstance)
                .map(FloppyDriveBlockEntity.class::cast)
                .filter(FloppyDriveBlockEntity::mediaChanging).isPresent()) {
            player.sendSystemMessage(Component.literal("Computer " + computer.getComputerId() + " | Wait for the floppy media change to finish."));
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
        String bootFloppyPath = connectedBootFloppyPath(player, computer, guestOperatingSystem);
        VirtualMachineBootRequest bootRequest = attachFloppy(computer, discBootRequest(computerId, isoPath, bootFloppyPath));
        java.util.List<String> preflightErrors = ComputerRuntimePreflight.validateSystemDiscBoot(guestOperatingSystem);
        if (guestOperatingSystem == GuestOperatingSystem.WINDOWS_95
                && computer.linkedFloppyDrivePos().flatMap(floppyPos -> bootFloppy(player, floppyPos)).filter(FloppyDriveBlockEntity::hasWin95BootFloppy).isPresent()) {
            preflightErrors.addAll(ComputerRuntimePreflight.validateBootFloppyPath(bootFloppyPath));
        }
        if (!preflightErrors.isEmpty()) {
            player.sendSystemMessage(Component.literal("Computer " + computerId + " | Disc boot blocked: " + preflightErrors.get(0)));
            return;
        }

        MineVM.VM_MANAGER.create(descriptor)
                .thenCompose(ignored -> MineVM.VM_MANAGER.getState(computerId))
                .whenComplete((state, throwable) -> server.execute(() -> {
                    if (!isCurrentComputer(computer) || !computer.poweredOn()) {
                        return;
                    }
                    if (throwable != null) {
                        send(server, player, "Computer " + computerId + " | Disc boot failed: " + rootMessage(throwable));
                        return;
                    }

                    if (isRunningOrStarting(state)) {
                        send(server, player, "Computer " + computerId + " | Already running | State: " + state);
                    } else if (state == VirtualMachineState.NOT_CREATED) {
                        computer.rememberDiskGuestOperatingSystem(guestOperatingSystem);
                        createThenStart(server, player, computer, descriptor, guestOperatingSystem, bootRequest, "Booting from " + guestOperatingSystem + " disc", false);
                    } else if (state == VirtualMachineState.STOPPED) {
                        computer.rememberDiskGuestOperatingSystem(guestOperatingSystem);
                        start(server, player, computer, guestOperatingSystem, bootRequest, "Booting from inserted disc", false);
                    } else {
                        send(server, player, "Computer " + computerId + " | VM busy | State: " + state);
                    }
                }));
    }

    private static void bootVmDisk(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        String computerId = computer.getComputerId();
        ensureRegistered(computer)
                .thenCompose(ignored -> MineVM.VM_MANAGER.getState(computerId))
                .whenComplete((state, throwable) -> server.execute(() -> {
                    if (!isCurrentComputer(computer) || !computer.poweredOn()) {
                        return;
                    }
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
                        createThenStart(server, player, computer, descriptor, selectedGuestOperatingSystem, bootDiskRequest(computer), "Booting from VM disk", true);
                    } else if (state == VirtualMachineState.STOPPED) {
                        start(server, player, computer, selectedGuestOperatingSystem(computer), bootDiskRequest(computer), "Booting from VM disk", true);
                    } else if (state == VirtualMachineState.ERROR && ComputerRuntimeConfig.vmDiskExists(computer)) {
                        GuestOperatingSystem selectedGuestOperatingSystem = selectedGuestOperatingSystem(computer);
                        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer, selectedGuestOperatingSystem);
                        computer.setActiveGuestOperatingSystem(selectedGuestOperatingSystem);
                        createThenStart(server, player, computer, descriptor, selectedGuestOperatingSystem, bootDiskRequest(computer), "Recovering and booting from VM disk", true);
                    } else if (isRunningOrStarting(state)) {
                        send(server, player, "Computer " + computerId + " | Already running | State: " + state);
                    } else {
                        send(server, player, "Computer " + computerId + " | VM busy | State: " + state);
                    }
                }));
    }

    private static GuestOperatingSystem selectedGuestOperatingSystem(ComputerBlockEntity computer) {
        return computer.selectedGuestOperatingSystem().orElseGet(ComputerRuntimeConfig::configuredGuestOperatingSystem);
    }

    private static VirtualMachineBootRequest bootDiskRequest(ComputerBlockEntity computer) {
        VirtualMachineBootRequest request = computer.insertedDisc()
                .map(ComputerRuntimeConfig::isoPathFor)
                .filter(isoPath -> !isoPath.isBlank())
                .map(isoPath -> VirtualMachineBootRequest.vmDiskWithAttachedDisc(computer.getComputerId(), isoPath))
                .orElseGet(() -> VirtualMachineBootRequest.vmDisk(computer.getComputerId()));
        return attachFloppy(computer, request);
    }

    private static VirtualMachineBootRequest attachFloppy(ComputerBlockEntity computer, VirtualMachineBootRequest request) {
        return computer.linkedFloppyDrivePos()
                .filter(pos -> computer.getLevel() != null && computer.getLevel().hasChunkAt(pos))
                .map(pos -> computer.getLevel().getBlockEntity(pos))
                .filter(FloppyDriveBlockEntity.class::isInstance)
                .map(FloppyDriveBlockEntity.class::cast)
                .filter(drive -> drive.connectedComputerId().filter(computer.getComputerId()::equals).isPresent())
                .flatMap(FloppyDriveBlockEntity::media)
                .map(media -> request.withFloppyMedia(media, media.isWindows95Boot()
                        ? ComputerRuntimeConfig.bootFloppyPathFor(GuestOperatingSystem.WINDOWS_95) : ""))
                .orElse(request);
    }

    private static VirtualMachineBootRequest discBootRequest(String computerId, String isoPath, String bootFloppyPath) {
        if (bootFloppyPath == null || bootFloppyPath.isBlank()) {
            return VirtualMachineBootRequest.systemDisc(computerId, isoPath);
        }
        return VirtualMachineBootRequest.systemDiscWithBootFloppy(computerId, isoPath, bootFloppyPath);
    }

    private static String connectedBootFloppyPath(ServerPlayer player, ComputerBlockEntity computer, GuestOperatingSystem guestOperatingSystem) {
        if (guestOperatingSystem != GuestOperatingSystem.WINDOWS_95) {
            return "";
        }
        boolean hasInsertedBootFloppy = computer.linkedFloppyDrivePos()
                .flatMap(pos -> bootFloppy(player, pos))
                .filter(drive -> drive.connectedComputerId().filter(computer.getComputerId()::equals).isPresent())
                .filter(FloppyDriveBlockEntity::hasWin95BootFloppy)
                .isPresent();
        return hasInsertedBootFloppy ? ComputerRuntimeConfig.bootFloppyPathFor(guestOperatingSystem) : "";
    }

    private static java.util.Optional<FloppyDriveBlockEntity> bootFloppy(ServerPlayer player, BlockPos pos) {
        if (!player.level().hasChunkAt(pos)) {
            return java.util.Optional.empty();
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof FloppyDriveBlockEntity floppyDrive && floppyDrive.connectedComputerId().isPresent()) {
            return java.util.Optional.of(floppyDrive);
        }
        return java.util.Optional.empty();
    }

    private static void powerOff(ServerPlayer player, BlockPos pos, ComputerBlockEntity computer) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        ComputerPowerActions.powerOff(server, player, pos, computer);
    }

    private static void createThenStart(MinecraftServer server, ServerPlayer player, ComputerBlockEntity computer, VirtualMachineDescriptor descriptor, GuestOperatingSystem guestOperatingSystem, VirtualMachineBootRequest bootRequest, String actionLabel, boolean persistGuest) {
        MineVM.VM_MANAGER.create(descriptor)
                .thenCompose(handle -> MineVM.VM_MANAGER.start(bootRequest))
                .whenComplete((handle, throwable) -> server.execute(() -> {
                    if (!isCurrentComputer(computer) || !computer.poweredOn()) {
                        if (throwable == null) {
                            MineVM.VM_MANAGER.stop(descriptor.computerId());
                        }
                        return;
                    }
                    if (throwable != null) {
                        computer.setVmDisplayActive(false);
                        player.sendSystemMessage(Component.literal("Computer " + descriptor.computerId() + " | " + actionLabel + " failed: " + rootMessage(throwable)));
                        return;
                    }
                    setComputerVmDisplay(computer, true, guestOperatingSystem, persistGuest);
                    ModNetwork.updateScreenControlMode(player, handle.computerId(), true);
                    player.sendSystemMessage(Component.literal("Computer " + handle.computerId() + " | " + actionLabel + " | State: " + handle.state()));
                }));
    }

    private static void start(MinecraftServer server, ServerPlayer player, ComputerBlockEntity computer, GuestOperatingSystem guestOperatingSystem, VirtualMachineBootRequest bootRequest, String actionLabel, boolean persistGuest) {
        MineVM.VM_MANAGER.start(bootRequest)
                .whenComplete((handle, throwable) -> sendHandle(server, player, computer, bootRequest.computerId(), guestOperatingSystem, actionLabel, handle, throwable, true, persistGuest));
    }

    private static void sendHandle(MinecraftServer server, ServerPlayer player, ComputerBlockEntity computer, String computerId, GuestOperatingSystem guestOperatingSystem, String actionLabel, VirtualMachineHandle handle, Throwable throwable, boolean vmDisplayActive, boolean persistGuest) {
        server.execute(() -> {
            if (!isCurrentComputer(computer) || !computer.poweredOn()) {
                if (throwable == null) {
                    MineVM.VM_MANAGER.stop(computerId);
                }
                return;
            }
            if (throwable != null) {
                player.sendSystemMessage(Component.literal("Computer " + computerId + " | " + actionLabel + " failed: " + rootMessage(throwable)));
                return;
            }
            setComputerVmDisplay(computer, vmDisplayActive, guestOperatingSystem, persistGuest);
            if (vmDisplayActive) {
                ModNetwork.updateScreenControlMode(player, handle.computerId(), true);
            }
            player.sendSystemMessage(Component.literal("Computer " + handle.computerId() + " | " + actionLabel + " | State: " + handle.state()));
        });
    }

    private static void setComputerVmDisplay(ComputerBlockEntity computer, boolean active, GuestOperatingSystem guestOperatingSystem, boolean persistGuest) {
        computer.setActiveGuestOperatingSystem(guestOperatingSystem);
        computer.setVmDisplayActive(active);
    }

    private static boolean isCurrentComputer(ComputerBlockEntity computer) {
        return !computer.isRemoved() && computer.getLevel() != null
                && computer.getLevel().hasChunkAt(computer.getBlockPos())
                && computer.getLevel().getBlockEntity(computer.getBlockPos()) == computer;
    }

    private static java.util.concurrent.CompletableFuture<VirtualMachineHandle> ensureRegistered(ComputerBlockEntity computer) {
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptorFor(computer);
        if (!ComputerRuntimeConfig.vmDiskExists(computer)) {
            return java.util.concurrent.CompletableFuture.completedFuture(new VirtualMachineHandle(computer.getComputerId(), VirtualMachineState.NOT_CREATED));
        }
        return MineVM.VM_MANAGER.create(descriptor);
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
