package com.example.vm.computer;

import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.GuestOperatingSystem;
import org.jetbrains.annotations.Nullable;

public final class ComputerStatusMessage {
    private ComputerStatusMessage() {
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state) {
        return describe(computerId, configuration, state, "");
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress) {
        return describe(computerId, configuration, state, screenAddress, null);
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress, @Nullable GuestOperatingSystem guestOperatingSystem) {
        return describe(computerId, configuration, state, screenAddress, guestOperatingSystem, "");
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress, @Nullable GuestOperatingSystem guestOperatingSystem, String insertedDisc) {
        String screenPart = screenAddress == null || screenAddress.isBlank() ? "" : " | Screen: " + screenAddress;
        String discPart = insertedDisc == null || insertedDisc.isBlank() ? "EMPTY" : insertedDisc;
        String guestPart = guestOperatingSystem == null ? "NULL" : guestOperatingSystem.name();
        return "Computer " + computerId
                + " | CPU: " + configuration.cpuCores()
                + " | Memory: " + configuration.memoryMb() + " MB"
                + " | Disk: " + configuration.diskFile()
                + " | Guest: " + guestPart
                + " | Disc: " + discPart
                + " | VM: " + state
                + screenPart;
    }
}
