package com.example.vm.test;

import com.example.vm.computer.ComputerHardwareConfiguration;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.backend.NoopVirtualMachineBackend;
import com.example.vm.vm.backend.VirtualMachineBackend;

public final class ComputerRuntimeConfigSelfTest {
    private ComputerRuntimeConfigSelfTest() {
    }

    public static void main(String[] args) {
        assertDefaultBackendIsSafe();
        assertDescriptorUsesConfiguredRuntimeDefaults();
        assertDiscIsoDefaultsAreSafe();
    }

    private static void assertDiscIsoDefaultsAreSafe() {
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.UBUNTU_SERVER), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.LINUX), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.UBUNTU_DESKTOP), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_10), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_11), "");
    }

    private static void assertDefaultBackendIsSafe() {
        VirtualMachineBackend backend = ComputerRuntimeConfig.createBackend();
        try {
            if (!(backend instanceof NoopVirtualMachineBackend)) {
                throw new AssertionError("Default backend must be NoopVirtualMachineBackend but got " + backend.getClass().getName());
            }
        } finally {
            backend.close();
        }
    }

    private static void assertDescriptorUsesConfiguredRuntimeDefaults() {
        ComputerHardwareConfiguration configuration = new ComputerHardwareConfiguration(6, 8192, "computer-config.qcow2");
        VirtualMachineDescriptor descriptor = ComputerRuntimeConfig.descriptor("computer-config", configuration);

        assertEquals(descriptor.computerId(), "computer-config");
        assertEquals(descriptor.cpuCores(), 6);
        assertEquals(descriptor.memoryMb(), 8192);
        assertEquals(descriptor.diskFile(), "computer-config.qcow2");
        assertEquals(descriptor.diskSizeGb(), 20);
        assertEquals(descriptor.guestOperatingSystem().name(), "UBUNTU_SERVER");
        if (descriptor.firmwareConfiguration().requiresUefi()) {
            throw new AssertionError("Ubuntu default profile must not require UEFI");
        }
    }

    private static void assertEquals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int actual, int expected) {
        if (actual != expected) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
