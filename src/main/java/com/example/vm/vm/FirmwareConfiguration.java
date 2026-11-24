package com.example.vm.vm;

public record FirmwareConfiguration(
        boolean requiresUefi,
        boolean requiresSecureBoot,
        boolean requiresTpm,
        boolean prefersVirtio
) {
    public static FirmwareConfiguration forGuest(GuestOperatingSystem guestOperatingSystem) {
        return switch (guestOperatingSystem) {
            case FREEDOS -> new FirmwareConfiguration(false, false, false, false);
            case WINDOWS_95 -> new FirmwareConfiguration(false, false, false, false);
            case WINDOWS_98, WINDOWS_XP, WINDOWS_VISTA -> new FirmwareConfiguration(false, false, false, false);
            case WINDOWS_10 -> new FirmwareConfiguration(true, false, false, true);
            case WINDOWS_11 -> new FirmwareConfiguration(true, true, true, true);
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> new FirmwareConfiguration(false, false, false, true);
        };
    }
}
