package com.example.vm.test;

import com.example.vm.vm.FirmwareConfiguration;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineDescriptor;

public final class GuestOperatingSystemSelfTest {
    private GuestOperatingSystemSelfTest() {
    }

    public static void main(String[] args) {
        assertEquals(GuestOperatingSystem.parse("linux"), GuestOperatingSystem.UBUNTU_SERVER);
        assertEquals(GuestOperatingSystem.parse("UBUNTU_SERVER"), GuestOperatingSystem.UBUNTU_SERVER);
        assertEquals(GuestOperatingSystem.parse("UBUNTU_DESKTOP"), GuestOperatingSystem.UBUNTU_DESKTOP);
        assertEquals(GuestOperatingSystem.parse("FREEDOS"), GuestOperatingSystem.FREEDOS);
        assertEquals(GuestOperatingSystem.parse("WINDOWS_95"), GuestOperatingSystem.WINDOWS_95);
        assertEquals(GuestOperatingSystem.parse("WINDOWS_XP"), GuestOperatingSystem.WINDOWS_XP);
        assertEquals(GuestOperatingSystem.parse("WINDOWS_10"), GuestOperatingSystem.WINDOWS_10);
        assertEquals(GuestOperatingSystem.parse("bad-value"), GuestOperatingSystem.UBUNTU_SERVER);
        assertEquals(GuestOperatingSystem.UBUNTU_SERVER.usesGraphicalPointer(), false);
        assertEquals(GuestOperatingSystem.UBUNTU_DESKTOP.usesGraphicalPointer(), true);
        assertEquals(GuestOperatingSystem.FREEDOS.usesGraphicalPointer(), false);
        assertEquals(GuestOperatingSystem.WINDOWS_95.usesGraphicalPointer(), true);
        assertEquals(GuestOperatingSystem.WINDOWS_XP.usesGraphicalPointer(), true);

        FirmwareConfiguration ubuntuDesktop = FirmwareConfiguration.forGuest(GuestOperatingSystem.UBUNTU_DESKTOP);
        assertFalse(ubuntuDesktop.requiresUefi());
        assertFalse(ubuntuDesktop.requiresTpm());
        assertTrue(ubuntuDesktop.prefersVirtio());

        FirmwareConfiguration windows10 = FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_10);
        assertTrue(windows10.requiresUefi());
        assertFalse(windows10.requiresSecureBoot());
        assertFalse(windows10.requiresTpm());

        FirmwareConfiguration windowsXp = FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_XP);
        assertFalse(windowsXp.requiresUefi());
        assertFalse(windowsXp.prefersVirtio());

        FirmwareConfiguration freeDos = FirmwareConfiguration.forGuest(GuestOperatingSystem.FREEDOS);
        assertFalse(freeDos.requiresUefi());
        assertFalse(freeDos.prefersVirtio());

        FirmwareConfiguration windows95 = FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_95);
        assertFalse(windows95.requiresUefi());
        assertFalse(windows95.prefersVirtio());

        FirmwareConfiguration windows11 = FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_11);
        assertTrue(windows11.requiresUefi());
        assertTrue(windows11.requiresSecureBoot());
        assertTrue(windows11.requiresTpm());

        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor("computer-test", 4, 4096, "disk.qcow2");
        assertEquals(descriptor.guestOperatingSystem(), GuestOperatingSystem.UBUNTU_SERVER);
        assertFalse(descriptor.firmwareConfiguration().requiresUefi());
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
