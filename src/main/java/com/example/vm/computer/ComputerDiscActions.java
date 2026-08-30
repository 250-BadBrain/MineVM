package com.example.vm.computer;

import com.example.vm.MineVM;
import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.registry.ModItems;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletionException;

public final class ComputerDiscActions {
    private ComputerDiscActions() {
    }

    public static void ejectIfStopped(MinecraftServer server, Player player, ComputerBlockEntity computer, BlockPos pos) {
        if (server == null) {
            return;
        }

        String computerId = computer.getComputerId();
        MineVM.VM_MANAGER.getState(computerId)
                .whenComplete((state, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        player.sendSystemMessage(Component.literal("Computer " + computerId + " | Disc eject failed: " + rootMessage(throwable)));
                        return;
                    }

                    if (isVmBusyForDiscEject(state)) {
                        if (state == VirtualMachineState.STOPPING) {
                            player.sendSystemMessage(Component.literal("Computer " + computerId + " | Cannot eject disc while VM is stopping"));
                            return;
                        }
                        ejectRunningVmDisc(server, player, computer);
                        return;
                    }

                    ejectNow(player, computer, computerId, "Ejected disc");
                }));
    }

    private static void ejectRunningVmDisc(MinecraftServer server, Player player, ComputerBlockEntity computer) {
        String computerId = computer.getComputerId();
        MineVM.VM_MANAGER.ejectSystemDisc(computerId)
                .whenComplete((handle, throwable) -> server.execute(() -> {
                    if (throwable != null) {
                        player.sendSystemMessage(Component.literal("Computer " + computerId + " | VM disc eject failed: " + rootMessage(throwable)));
                        return;
                    }

                    ejectNow(player, computer, computerId, "Ejected disc from VM");
                }));
    }

    private static void ejectNow(Player player, ComputerBlockEntity computer, String computerId, String actionLabel) {
        computer.ejectDisc()
                .ifPresentOrElse(
                        guestOperatingSystem -> {
                            player.addItem(new ItemStack(discItemFor(guestOperatingSystem)));
                            player.sendSystemMessage(Component.literal("Computer " + computerId + " | " + actionLabel + ": " + guestOperatingSystem + " | Power on, then Boot Disk to start the installed system."));
                        },
                        () -> player.sendSystemMessage(Component.literal("Computer " + computerId + " | Disc slot empty"))
                );
    }

    private static boolean isVmBusyForDiscEject(VirtualMachineState state) {
        return state == VirtualMachineState.RUNNING
                || state == VirtualMachineState.STARTING
                || state == VirtualMachineState.STOPPING;
    }

    private static Item discItemFor(GuestOperatingSystem guestOperatingSystem) {
        return switch (guestOperatingSystem) {
            case FREEDOS -> ModItems.FREEDOS_DISC.get();
            case WINDOWS_95 -> ModItems.WINDOWS_95_DISC.get();
            case WINDOWS_XP -> ModItems.WINDOWS_XP_DISC.get();
            case WINDOWS_10 -> ModItems.WINDOWS_10_DISC.get();
            case WINDOWS_11 -> ModItems.WINDOWS_11_DISC.get();
            case UBUNTU_DESKTOP -> ModItems.UBUNTU_DESKTOP_DISC.get();
            case UBUNTU_SERVER, LINUX -> ModItems.UBUNTU_SERVER_DISC.get();
        };
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
}
