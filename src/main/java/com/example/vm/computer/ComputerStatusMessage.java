package com.example.vm.computer;

import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.GuestOperatingSystem;

public final class ComputerStatusMessage {
    private ComputerStatusMessage() {
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state) {
        return describe(computerId, configuration, state, "");
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress) {
        return describe(computerId, configuration, state, screenAddress, GuestOperatingSystem.UBUNTU_SERVER);
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress, GuestOperatingSystem guestOperatingSystem) {
        return describe(computerId, configuration, state, screenAddress, guestOperatingSystem, "");
    }

    public static String describe(String computerId, ComputerHardwareConfiguration configuration, VirtualMachineState state, String screenAddress, GuestOperatingSystem guestOperatingSystem, String insertedDisc) {
        String screenPart = screenAddress == null || screenAddress.isBlank() ? "" : " | Screen: " + screenAddress;
        String discPart = insertedDisc == null || insertedDisc.isBlank() ? "EMPTY" : insertedDisc;
        return "Computer " + computerId
                + " | CPU: " + configuration.cpuCores()
                + " | Memory: " + configuration.memoryMb() + " MB"
                + " | Disk: " + configuration.diskFile()
                + " | Guest: " + guestOperatingSystem
                + " | Disc: " + discPart
                + " | VM: " + state
                + screenPart;
    }
}
