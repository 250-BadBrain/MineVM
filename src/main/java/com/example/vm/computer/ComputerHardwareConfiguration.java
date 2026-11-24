package com.example.vm.computer;

import java.nio.file.Path;

public record ComputerHardwareConfiguration(int cpuCores, int memoryMb, String diskFile) {
    public static final int DEFAULT_CPU_CORES = 4;
    public static final int DEFAULT_MEMORY_MB = 4096;
    public static final int MAX_CPU_CORES = 16;
    public static final int MAX_MEMORY_MB = 16_384;

    public ComputerHardwareConfiguration {
        if (cpuCores < 1) {
            throw new IllegalArgumentException("cpuCores must be at least 1");
        }
        if (cpuCores > MAX_CPU_CORES) {
            throw new IllegalArgumentException("cpuCores must not exceed " + MAX_CPU_CORES);
        }
        if (memoryMb < 512) {
            throw new IllegalArgumentException("memoryMb must be at least 512");
        }
        if (memoryMb > MAX_MEMORY_MB) {
            throw new IllegalArgumentException("memoryMb must not exceed " + MAX_MEMORY_MB);
        }
        if (diskFile == null || diskFile.isBlank()) {
            throw new IllegalArgumentException("diskFile must not be blank");
        }
        Path diskPath = Path.of(diskFile);
        if (diskPath.isAbsolute() || diskPath.getNameCount() != 1 || diskFile.equals(".") || diskFile.equals("..")) {
            throw new IllegalArgumentException("diskFile must be a managed file name");
        }
    }

    public static ComputerHardwareConfiguration defaultConfiguration(String computerId) {
        return new ComputerHardwareConfiguration(
                DEFAULT_CPU_CORES,
                DEFAULT_MEMORY_MB,
                computerId + ".qcow2"
        );
    }
}
