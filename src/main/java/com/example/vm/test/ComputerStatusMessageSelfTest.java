package com.example.vm.test;

import com.example.vm.computer.ComputerHardwareConfiguration;
import com.example.vm.computer.ComputerStatusMessage;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineState;

public final class ComputerStatusMessageSelfTest {
    private ComputerStatusMessageSelfTest() {
    }

    public static void main(String[] args) {
        ComputerHardwareConfiguration configuration = new ComputerHardwareConfiguration(4, 4096, "computer-test.qcow2");
        String message = ComputerStatusMessage.describe("computer-test", configuration, VirtualMachineState.STOPPED);
        String expected = "Computer computer-test | CPU: 4 | Memory: 4096 MB | Disk: computer-test.qcow2 | Guest: UBUNTU_SERVER | Disc: EMPTY | VM: STOPPED";

        if (!message.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + message);
        }

        String screenMessage = ComputerStatusMessage.describe("computer-test", configuration, VirtualMachineState.RUNNING, "127.0.0.1:5910");
        String screenExpected = "Computer computer-test | CPU: 4 | Memory: 4096 MB | Disk: computer-test.qcow2 | Guest: UBUNTU_SERVER | Disc: EMPTY | VM: RUNNING | Screen: 127.0.0.1:5910";

        if (!screenMessage.equals(screenExpected)) {
            throw new AssertionError("Expected " + screenExpected + " but got " + screenMessage);
        }

        String windowsMessage = ComputerStatusMessage.describe("computer-test", configuration, VirtualMachineState.STOPPED, "", GuestOperatingSystem.WINDOWS_11, "WINDOWS_11");
        String windowsExpected = "Computer computer-test | CPU: 4 | Memory: 4096 MB | Disk: computer-test.qcow2 | Guest: WINDOWS_11 | Disc: WINDOWS_11 | VM: STOPPED";
        if (!windowsMessage.equals(windowsExpected)) {
            throw new AssertionError("Expected " + windowsExpected + " but got " + windowsMessage);
        }

        String desktopMessage = ComputerStatusMessage.describe("computer-test", configuration, VirtualMachineState.RUNNING, "127.0.0.1:5910", GuestOperatingSystem.UBUNTU_DESKTOP, "UBUNTU_DESKTOP");
        String desktopExpected = "Computer computer-test | CPU: 4 | Memory: 4096 MB | Disk: computer-test.qcow2 | Guest: UBUNTU_DESKTOP | Disc: UBUNTU_DESKTOP | VM: RUNNING | Screen: 127.0.0.1:5910";
        if (!desktopMessage.equals(desktopExpected)) {
            throw new AssertionError("Expected " + desktopExpected + " but got " + desktopMessage);
        }
    }
}
