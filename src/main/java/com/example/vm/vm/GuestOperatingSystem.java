package com.example.vm.vm;

import java.util.Locale;

public enum GuestOperatingSystem {
    UBUNTU_SERVER,
    UBUNTU_DESKTOP,
    LINUX,
    FREEDOS,
    WINDOWS_95,
    WINDOWS_XP,
    WINDOWS_10,
    WINDOWS_11;

    public boolean usesGraphicalPointer() {
        return switch (this) {
            case UBUNTU_DESKTOP, WINDOWS_95, WINDOWS_XP, WINDOWS_10, WINDOWS_11 -> true;
            case UBUNTU_SERVER, LINUX, FREEDOS -> false;
        };
    }

    public static GuestOperatingSystem parse(String value) {
        if (value == null || value.isBlank()) {
            return UBUNTU_SERVER;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("LINUX".equals(normalized)) {
            return UBUNTU_SERVER;
        }

        try {
            return GuestOperatingSystem.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            return UBUNTU_SERVER;
        }
    }
}
