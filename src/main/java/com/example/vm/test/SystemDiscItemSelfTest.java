package com.example.vm.test;

import com.example.vm.item.SystemDiscDefinition;
import com.example.vm.vm.GuestOperatingSystem;

public final class SystemDiscItemSelfTest {
    private SystemDiscItemSelfTest() {
    }

    public static void main(String[] args) {
        SystemDiscDefinition ubuntuDisc = new SystemDiscDefinition(
                GuestOperatingSystem.UBUNTU_SERVER,
                "item.computer_in_minecraft.ubuntu_server_disc"
        );
        assertEquals(ubuntuDisc.guestOperatingSystem(), GuestOperatingSystem.UBUNTU_SERVER);
        assertEquals(ubuntuDisc.displayNameKey(), "item.computer_in_minecraft.ubuntu_server_disc");

        SystemDiscDefinition ubuntuDesktopDisc = new SystemDiscDefinition(
                GuestOperatingSystem.UBUNTU_DESKTOP,
                "item.computer_in_minecraft.ubuntu_desktop_disc"
        );
        assertEquals(ubuntuDesktopDisc.guestOperatingSystem(), GuestOperatingSystem.UBUNTU_DESKTOP);
        assertEquals(ubuntuDesktopDisc.displayNameKey(), "item.computer_in_minecraft.ubuntu_desktop_disc");

        SystemDiscDefinition windows11Disc = new SystemDiscDefinition(
                GuestOperatingSystem.WINDOWS_11,
                "item.computer_in_minecraft.windows_11_disc"
        );
        assertEquals(windows11Disc.guestOperatingSystem(), GuestOperatingSystem.WINDOWS_11);
    }

    private static void assertEquals(Object actual, Object expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
