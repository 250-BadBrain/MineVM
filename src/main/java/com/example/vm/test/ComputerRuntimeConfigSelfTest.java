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
        assertInvalidDiskNamesRejected();
    }

    private static void assertInvalidDiskNamesRejected() {
        for (String name : new String[] {".", "..", "../outside.qcow2"}) {
            try {
                new ComputerHardwareConfiguration(1, 512, name);
                throw new AssertionError("Managed disks must reject " + name);
            } catch (IllegalArgumentException expected) {
                // A disk filename must name one file inside its own VM directory.
            }
        }
    }

    private static void assertDiscIsoDefaultsAreSafe() {
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.UBUNTU_SERVER), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.LINUX), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.UBUNTU_DESKTOP), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.FREEDOS), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_95), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_XP), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_10), "");
        assertEquals(ComputerRuntimeConfig.isoPathFor(GuestOperatingSystem.WINDOWS_11), "");
        assertEquals(ComputerRuntimeConfig.bootFloppyPathFor(GuestOperatingSystem.WINDOWS_95), "");
        assertEquals(ComputerRuntimeConfig.bootFloppyPathFor(GuestOperatingSystem.WINDOWS_XP), "");
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

        VirtualMachineDescriptor ubuntuDesktopDescriptor = ComputerRuntimeConfig.descriptor("computer-desktop", configuration, GuestOperatingSystem.UBUNTU_DESKTOP);
        assertEquals(ubuntuDesktopDescriptor.diskSizeGb(), 25);

        VirtualMachineDescriptor windowsXpDescriptor = ComputerRuntimeConfig.descriptor("computer-xp", configuration, GuestOperatingSystem.WINDOWS_XP);
        assertEquals(windowsXpDescriptor.diskSizeGb(), 20);
        if (windowsXpDescriptor.firmwareConfiguration().requiresUefi()) {
            throw new AssertionError("Windows XP profile must use legacy BIOS");
        }

        ComputerHardwareConfiguration lowDiskConfiguration = new ComputerHardwareConfiguration(1, 512, "computer-dos.qcow2");
        VirtualMachineDescriptor freeDosDescriptor = ComputerRuntimeConfig.descriptor("computer-dos", lowDiskConfiguration, GuestOperatingSystem.FREEDOS);
        assertEquals(ComputerRuntimeConfig.minimumDiskSizeGb(GuestOperatingSystem.FREEDOS), 1);
        assertEquals(freeDosDescriptor.diskSizeGb(), 20);
        if (freeDosDescriptor.firmwareConfiguration().requiresUefi() || freeDosDescriptor.firmwareConfiguration().prefersVirtio()) {
            throw new AssertionError("FreeDOS profile must use legacy BIOS and IDE-compatible devices");
        }

        VirtualMachineDescriptor windows95Descriptor = ComputerRuntimeConfig.descriptor("computer-win95", configuration, GuestOperatingSystem.WINDOWS_95);
        assertEquals(windows95Descriptor.diskSizeGb(), 2);
        if (windows95Descriptor.firmwareConfiguration().requiresUefi() || windows95Descriptor.firmwareConfiguration().prefersVirtio()) {
            throw new AssertionError("Windows 95 profile must use legacy BIOS and IDE-compatible devices");
        }

        VirtualMachineDescriptor windows10Descriptor = ComputerRuntimeConfig.descriptor("computer-windows", configuration, GuestOperatingSystem.WINDOWS_10);
        assertEquals(windows10Descriptor.diskSizeGb(), 64);
        if (!windows10Descriptor.firmwareConfiguration().requiresUefi()) {
            throw new AssertionError("Windows 10 profile must require UEFI");
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
