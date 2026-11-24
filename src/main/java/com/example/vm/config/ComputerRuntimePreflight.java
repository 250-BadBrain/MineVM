package com.example.vm.config;

import com.example.vm.vm.GuestOperatingSystem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ComputerRuntimePreflight {
    private ComputerRuntimePreflight() {
    }

    public static List<String> validatePowerOn() {
        List<String> errors = new ArrayList<>();
        if (!ComputerRuntimeConfig.localQemuEnabled()) {
            errors.add("Local QEMU backend is disabled in config.");
            return errors;
        }

        requireFile(errors, ComputerRuntimeConfig.qemuSystemPath(), "qemu-system-x86_64");
        requireFile(errors, ComputerRuntimeConfig.qemuImgPath(), "qemu-img");
        Path storageDirectory = ComputerRuntimeConfig.vmStorageDirectory();
        if (Files.exists(storageDirectory) && !Files.isDirectory(storageDirectory)) {
            errors.add("VM storage path is not a directory: " + storageDirectory);
        }
        return errors;
    }

    public static List<String> validateSystemDiscBoot(GuestOperatingSystem guestOperatingSystem) {
        List<String> errors = new ArrayList<>(validatePowerOn());
        String isoPath = ComputerRuntimeConfig.isoPathFor(guestOperatingSystem);
        if (isoPath.isBlank()) {
            errors.add("No ISO path configured for " + guestOperatingSystem + " disc.");
        } else if (!Files.isRegularFile(Path.of(isoPath))) {
            errors.add("System disc ISO does not exist: " + isoPath);
        }
        return errors;
    }

    public static List<String> validateBootFloppyPath(String bootFloppyPath) {
        List<String> errors = new ArrayList<>();
        if (bootFloppyPath == null || bootFloppyPath.isBlank()) {
            errors.add("No boot floppy image path configured.");
        } else if (!Files.isRegularFile(Path.of(bootFloppyPath))) {
            errors.add("Boot floppy image does not exist: " + bootFloppyPath);
        }
        return errors;
    }

    private static void requireFile(List<String> errors, Path path, String label) {
        if (!Files.isRegularFile(path)) {
            errors.add("Missing " + label + " executable: " + path);
        }
    }
}
