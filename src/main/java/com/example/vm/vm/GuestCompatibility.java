package com.example.vm.vm;

import java.util.List;

public final class GuestCompatibility {
    public enum Evidence { USER_REPORTED, INSTALLATION_PENDING }
    public record Entry(GuestOperatingSystem guest, Evidence evidence, String testedVersion, List<String> requiredChecks) {}
    private static final List<String> CHECKS = List.of("install", "disk_boot", "restart", "power_off", "media_swap", "input", "idle_15m");
    public static Entry forGuest(GuestOperatingSystem guest) {
        return switch (guest) {
            case FREEDOS -> new Entry(guest, Evidence.USER_REPORTED, "1.4 LiveCD", CHECKS);
            case WINDOWS_XP -> new Entry(guest, Evidence.USER_REPORTED, "image revision not recorded", CHECKS);
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> new Entry(guest, Evidence.USER_REPORTED, "image revision not recorded", CHECKS);
            case WINDOWS_95 -> new Entry(guest, Evidence.INSTALLATION_PENDING, "OSR2/OSR2.5; final boot regression pending", CHECKS);
            case WINDOWS_98 -> new Entry(guest, Evidence.INSTALLATION_PENDING, "Second Edition profile; not installed", CHECKS);
            case WINDOWS_VISTA -> new Entry(guest, Evidence.INSTALLATION_PENDING, "32-bit BIOS profile; not installed", CHECKS);
            case WINDOWS_10, WINDOWS_11 -> new Entry(guest, Evidence.INSTALLATION_PENDING, "profile only; not installed", CHECKS);
        };
    }
    private GuestCompatibility() {}
}
