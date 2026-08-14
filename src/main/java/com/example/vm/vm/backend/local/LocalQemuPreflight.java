package com.example.vm.vm.backend.local;

import com.example.vm.vm.VirtualMachineBootDevice;
import com.example.vm.vm.VirtualMachineBootRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class LocalQemuPreflight {
    private LocalQemuPreflight() {
    }

    public static void validateTools(LocalQemuConfiguration configuration) {
        requireRegularFile(configuration.qemuSystemPath(), "qemu-system-x86_64");
        requireRegularFile(configuration.qemuImgPath(), "qemu-img");

        Path storageDirectory = configuration.vmStorageDirectory();
        if (Files.exists(storageDirectory) && !Files.isDirectory(storageDirectory)) {
            throw new IllegalStateException("VM storage path is not a directory: " + storageDirectory);
        }
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create VM storage directory: " + storageDirectory, exception);
        }
    }

    public static void validateBootRequest(VirtualMachineBootRequest bootRequest) {
        if (bootRequest.bootDevice() != VirtualMachineBootDevice.SYSTEM_DISC) {
            return;
        }

        Path isoPath = Path.of(bootRequest.isoPath());
        if (!Files.isRegularFile(isoPath)) {
            throw new IllegalStateException("System disc ISO does not exist: " + isoPath);
        }
    }

    private static void requireRegularFile(Path path, String label) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing " + label + " executable: " + path);
        }
    }
}
