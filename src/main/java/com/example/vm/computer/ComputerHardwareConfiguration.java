package com.example.vm.computer;

public record ComputerHardwareConfiguration(int cpuCores, int memoryMb, String diskFile) {
    public static final int DEFAULT_CPU_CORES = 4;
    public static final int DEFAULT_MEMORY_MB = 4096;

    public ComputerHardwareConfiguration {
        if (cpuCores < 1) {
            throw new IllegalArgumentException("cpuCores must be at least 1");
        }
        if (memoryMb < 512) {
            throw new IllegalArgumentException("memoryMb must be at least 512");
        }
        if (diskFile == null || diskFile.isBlank()) {
            throw new IllegalArgumentException("diskFile must not be blank");
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
