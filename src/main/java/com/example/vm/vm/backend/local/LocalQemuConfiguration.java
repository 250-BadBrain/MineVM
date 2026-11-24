package com.example.vm.vm.backend.local;

import java.nio.file.Path;

public record LocalQemuConfiguration(
        Path qemuSystemPath,
        Path qemuImgPath,
        Path vmStorageDirectory,
        String accelerator
) {
    public LocalQemuConfiguration {
        if (qemuSystemPath == null) {
            throw new IllegalArgumentException("qemuSystemPath must not be null");
        }
        if (qemuImgPath == null) {
            throw new IllegalArgumentException("qemuImgPath must not be null");
        }
        if (vmStorageDirectory == null) {
            throw new IllegalArgumentException("vmStorageDirectory must not be null");
        }
        if (accelerator == null || accelerator.isBlank()) {
            accelerator = "whpx";
        }
    }

    public static LocalQemuConfiguration windowsDefault(Path vmStorageDirectory) {
        return new LocalQemuConfiguration(
                Path.of("D:", "qemu", "qemu-system-x86_64.exe"),
                Path.of("D:", "qemu", "qemu-img.exe"),
                vmStorageDirectory,
                "whpx"
        );
    }

    public Path qemuIoPath() {
        Path parent = qemuImgPath.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return Path.of("qemu-io");
        }
        return parent.resolve("qemu-io.exe").normalize();
    }
}
