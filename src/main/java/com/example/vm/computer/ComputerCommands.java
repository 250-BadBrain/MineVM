package com.example.vm.computer;

import com.example.vm.MineVM;
import com.example.vm.block.ComputerOccupancyBlock;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.blockentity.ComputerMonitorBlockEntity;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.config.ComputerRuntimePreflight;
import com.example.vm.screen.FrameMetrics;
import com.example.vm.vm.DiskMaintenance;
import com.example.vm.vm.GuestCompatibility;
import com.example.vm.vm.GuestOperatingSystem;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class ComputerCommands {
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("minevm")
                .executes(context -> help(context.getSource()))
                .then(Commands.literal("help").executes(context -> help(context.getSource())))
                .then(Commands.literal("doctor").executes(context -> doctor(context.getSource())))
                .then(Commands.literal("status").executes(ComputerCommands::status))
                .then(Commands.literal("compatibility").executes(context -> compatibility(context.getSource())));
        var disk = Commands.literal("disk")
                .then(Commands.literal("backup").executes(context -> maintain(context, DiskMaintenance.Operation.BACKUP)))
                .then(Commands.literal("restore").then(Commands.literal("confirm").executes(context -> maintain(context, DiskMaintenance.Operation.RESTORE))))
                .then(Commands.literal("reset").then(Commands.literal("confirm").executes(context -> maintain(context, DiskMaintenance.Operation.RESET))));
        root.then(disk);
        root.then(Commands.literal("access")
                .then(Commands.literal("grant").then(Commands.argument("player", EntityArgument.player()).executes(context -> access(context, true))))
                .then(Commands.literal("revoke").then(Commands.argument("player", EntityArgument.player()).executes(context -> access(context, false)))));
        event.getDispatcher().register(root);
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.minevm.help"), false);
        return 1;
    }

    private static int doctor(CommandSourceStack source) {
        if (!source.hasPermission(2) && !(source.getEntity() instanceof ServerPlayer player
                && source.getServer().isSingleplayerOwner(player.getGameProfile()))) {
            source.sendSuccess(() -> Component.translatable("command.minevm.doctor_restricted"), false);
            return 0;
        }
        try {
            var errors = ComputerRuntimePreflight.validatePowerOn();
            if (errors.isEmpty()) source.sendSuccess(() -> Component.translatable("command.minevm.doctor_ok"), false);
            for (String error : errors) source.sendFailure(Component.translatable("command.minevm.doctor_error", error));
            for (GuestOperatingSystem guest : GuestOperatingSystem.values()) {
                if (guest == GuestOperatingSystem.LINUX) continue;
                String iso = ComputerRuntimeConfig.isoPathFor(guest);
                String status = iso.isBlank() ? "not_configured" : java.nio.file.Files.isRegularFile(java.nio.file.Path.of(iso)) ? "ready" : "missing";
                source.sendSuccess(() -> Component.translatable("command.minevm.iso_status", guest.name(), Component.translatable("status.minevm." + status)), false);
            }
            return errors.isEmpty() ? 1 : 0;
        } catch (RuntimeException invalid) {
            source.sendFailure(Component.translatable("command.minevm.doctor_error", invalid.getMessage()));
            return 0;
        }
    }

    private static int compatibility(CommandSourceStack source) {
        for (GuestOperatingSystem guest : GuestOperatingSystem.values()) {
            if (guest == GuestOperatingSystem.LINUX) continue;
            var entry = GuestCompatibility.forGuest(guest);
            source.sendSuccess(() -> Component.translatable("command.minevm.compatibility", guest.name(),
                    Component.translatable("compatibility.minevm." + entry.evidence().name().toLowerCase(java.util.Locale.ROOT)), entry.testedVersion()), false);
        }
        return 1;
    }

    private static ComputerBlockEntity target(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!(player.pick(8, 0, false) instanceof BlockHitResult hit)) return null;
        BlockPos pos = hit.getBlockPos();
        if (player.level().getBlockState(pos).getBlock() instanceof ComputerOccupancyBlock) {
            BlockPos origin = ComputerOccupancyBlock.origin(player.level(), pos);
            if (origin == null) return null;
            pos = origin;
        }
        BlockEntity entity = player.level().getBlockEntity(pos);
        if (entity instanceof ComputerMonitorBlockEntity monitor && monitor.linkedComputerPos().isPresent()
                && player.level().hasChunkAt(monitor.linkedComputerPos().get())) {
            entity = player.level().getBlockEntity(monitor.linkedComputerPos().get());
            if (entity instanceof ComputerBlockEntity computer && !monitor.linkedComputerId().filter(computer.getComputerId()::equals).isPresent()) return null;
        }
        if (!(entity instanceof ComputerBlockEntity computer)) {
            source.sendFailure(Component.translatable("command.minevm.look_at_computer"));
            return null;
        }
        if (!computer.canManage(player)) {
            source.sendFailure(Component.translatable("message.minevm.not_owner"));
            return null;
        }
        computer.claimOwner(player.getUUID());
        return computer;
    }

    private static int status(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ComputerBlockEntity computer = target(source);
        if (computer == null) return 0;
        String id = computer.getComputerId();
        MineVM.VM_MANAGER.diagnostics(id).whenComplete((diagnostics, failure) -> source.getServer().execute(() -> {
            if (failure != null) { source.sendFailure(Component.translatable("command.minevm.failed", rootMessage(failure))); return; }
            var metrics = FrameMetrics.forComputer(id).snapshot();
            source.sendSuccess(() -> Component.translatable("command.minevm.vm_status", id, diagnostics.state().name(), diagnostics.processId(), diagnostics.cpuMillis()), false);
            source.sendSuccess(() -> Component.translatable("command.minevm.frame_status", metrics.frames(), metrics.frames() == 0 ? 0 : metrics.mergeNanos() / metrics.frames() / 1000,
                    metrics.uploads(), metrics.uploads() == 0 ? 0 : metrics.uploadNanos() / metrics.uploads() / 1000, metrics.networkBytes()), false);
            source.sendSuccess(() -> Component.translatable("command.minevm.wiring_status", computer.keyboardConnected(), computer.mouseConnected(), computer.linkedFloppyDrivePos().isPresent()), false);
            if (!diagnostics.lastError().isBlank()) source.sendFailure(Component.translatable("command.minevm.last_error", diagnostics.lastError()));
        }));
        return 1;
    }

    private static int maintain(CommandContext<CommandSourceStack> context, DiskMaintenance.Operation operation) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ComputerBlockEntity computer = target(source);
        if (computer == null) return 0;
        if (computer.poweredOn() || computer.powerTransitioning()) {
            source.sendFailure(Component.translatable("command.minevm.power_off_first"));
            return 0;
        }
        computer.setPowerTransitioning(true);
        var descriptor = ComputerRuntimeConfig.descriptorFor(computer);
        source.sendSuccess(() -> Component.translatable("command.minevm.maintenance_started", operation.name()), false);
        MineVM.VM_MANAGER.create(descriptor)
                .thenCompose(ignored -> MineVM.VM_MANAGER.maintainDisk(computer.getComputerId(), operation))
                .thenCompose(path -> MineVM.VM_MANAGER.diagnostics(computer.getComputerId()).thenApply(info -> java.util.Map.entry(path, info)))
                .whenComplete((result, failure) -> source.getServer().execute(() -> {
                    computer.setPowerTransitioning(false);
                    if (failure != null) source.sendFailure(Component.translatable("command.minevm.failed", rootMessage(failure)));
                    else {
                        if (!computer.isRemoved() && result.getValue().guest() != null) {
                            computer.setActiveGuestOperatingSystem(result.getValue().guest());
                            computer.rememberDiskGuestOperatingSystem(result.getValue().guest());
                        }
                        source.sendSuccess(() -> Component.translatable("command.minevm.maintenance_done", operation.name(), result.getKey()), false);
                    }
                }));
        return 1;
    }

    private static int access(CommandContext<CommandSourceStack> context, boolean grant) throws CommandSyntaxException {
        ComputerBlockEntity computer = target(context.getSource());
        if (computer == null) return 0;
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        computer.setTrusted(player.getUUID(), grant);
        context.getSource().sendSuccess(() -> Component.translatable("command.minevm.access_changed", player.getGameProfile().getName(), grant), false);
        return 1;
    }

    private static String rootMessage(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }
    private ComputerCommands() {}
}
