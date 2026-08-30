package com.example.vm.test;

import com.example.vm.item.SystemDiscDefinition;
import com.example.vm.vm.GuestOperatingSystem;

public final class SystemDiscItemSelfTest {
    private SystemDiscItemSelfTest() {
    }

    public static void main(String[] args) {
        SystemDiscDefinition ubuntuDisc = new SystemDiscDefinition(
                GuestOperatingSystem.UBUNTU_SERVER,
                "item.minevm.ubuntu_server_disc"
        );
        assertEquals(ubuntuDisc.guestOperatingSystem(), GuestOperatingSystem.UBUNTU_SERVER);
        assertEquals(ubuntuDisc.displayNameKey(), "item.minevm.ubuntu_server_disc");

        SystemDiscDefinition ubuntuDesktopDisc = new SystemDiscDefinition(
                GuestOperatingSystem.UBUNTU_DESKTOP,
                "item.minevm.ubuntu_desktop_disc"
        );
        assertEquals(ubuntuDesktopDisc.guestOperatingSystem(), GuestOperatingSystem.UBUNTU_DESKTOP);
        assertEquals(ubuntuDesktopDisc.displayNameKey(), "item.minevm.ubuntu_desktop_disc");

        SystemDiscDefinition freeDosDisc = new SystemDiscDefinition(
                GuestOperatingSystem.FREEDOS,
                "item.minevm.freedos_disc"
        );
        assertEquals(freeDosDisc.guestOperatingSystem(), GuestOperatingSystem.FREEDOS);
        assertEquals(freeDosDisc.displayNameKey(), "item.minevm.freedos_disc");

        SystemDiscDefinition windows95Disc = new SystemDiscDefinition(
                GuestOperatingSystem.WINDOWS_95,
                "item.minevm.windows_95_disc"
        );
        assertEquals(windows95Disc.guestOperatingSystem(), GuestOperatingSystem.WINDOWS_95);
        assertEquals(windows95Disc.displayNameKey(), "item.minevm.windows_95_disc");

        SystemDiscDefinition windows10Disc = new SystemDiscDefinition(
                GuestOperatingSystem.WINDOWS_10,
                "item.minevm.windows_10_disc"
        );
        assertEquals(windows10Disc.guestOperatingSystem(), GuestOperatingSystem.WINDOWS_10);
        assertEquals(windows10Disc.displayNameKey(), "item.minevm.windows_10_disc");

        SystemDiscDefinition windowsXpDisc = new SystemDiscDefinition(
                GuestOperatingSystem.WINDOWS_XP,
                "item.minevm.windows_xp_disc"
        );
        assertEquals(windowsXpDisc.guestOperatingSystem(), GuestOperatingSystem.WINDOWS_XP);
        assertEquals(windowsXpDisc.displayNameKey(), "item.minevm.windows_xp_disc");

        SystemDiscDefinition windows11Disc = new SystemDiscDefinition(
                GuestOperatingSystem.WINDOWS_11,
                "item.minevm.windows_11_disc"
        );
        assertEquals(windows11Disc.guestOperatingSystem(), GuestOperatingSystem.WINDOWS_11);
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
