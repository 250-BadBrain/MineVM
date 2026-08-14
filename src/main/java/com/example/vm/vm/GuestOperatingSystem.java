package com.example.vm.vm;

import java.util.Locale;

public enum GuestOperatingSystem {
    UBUNTU_SERVER,
    UBUNTU_DESKTOP,
    LINUX,
    WINDOWS_10,
    WINDOWS_11;

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
