package com.example.vm.item;

import com.example.vm.vm.GuestOperatingSystem;

public record SystemDiscDefinition(GuestOperatingSystem guestOperatingSystem, String displayNameKey) {
    public SystemDiscDefinition {
        if (guestOperatingSystem == null) {
            throw new IllegalArgumentException("guestOperatingSystem must not be null");
        }
        if (displayNameKey == null || displayNameKey.isBlank()) {
            throw new IllegalArgumentException("displayNameKey must not be blank");
        }
    }
}
