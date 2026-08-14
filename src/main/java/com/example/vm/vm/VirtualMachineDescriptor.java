package com.example.vm.vm;

import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.computer.ComputerHardwareConfiguration;

public record VirtualMachineDescriptor(
        String computerId,
        int cpuCores,
        int memoryMb,
        String diskFile,
        int diskSizeGb,
        GuestOperatingSystem guestOperatingSystem,
        FirmwareConfiguration firmwareConfiguration
) {
    public VirtualMachineDescriptor(String computerId, int cpuCores, int memoryMb, String diskFile, int diskSizeGb) {
        this(computerId, cpuCores, memoryMb, diskFile, diskSizeGb, GuestOperatingSystem.UBUNTU_SERVER, FirmwareConfiguration.forGuest(GuestOperatingSystem.UBUNTU_SERVER));
    }

    public VirtualMachineDescriptor(String computerId, int cpuCores, int memoryMb, String diskFile) {
        this(computerId, cpuCores, memoryMb, diskFile, 20, GuestOperatingSystem.UBUNTU_SERVER, FirmwareConfiguration.forGuest(GuestOperatingSystem.UBUNTU_SERVER));
    }

    public VirtualMachineDescriptor {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
        if (cpuCores < 1) {
            throw new IllegalArgumentException("cpuCores must be at least 1");
        }
        if (memoryMb < 512) {
            throw new IllegalArgumentException("memoryMb must be at least 512");
        }
        if (diskFile == null || diskFile.isBlank()) {
            throw new IllegalArgumentException("diskFile must not be blank");
        }
        if (diskSizeGb < 1) {
            throw new IllegalArgumentException("diskSizeGb must be at least 1");
        }
        if (guestOperatingSystem == null) {
            guestOperatingSystem = GuestOperatingSystem.UBUNTU_SERVER;
        }
        if (firmwareConfiguration == null) {
            firmwareConfiguration = FirmwareConfiguration.forGuest(guestOperatingSystem);
        }
    }

    public static VirtualMachineDescriptor from(ComputerBlockEntity computer) {
        ComputerHardwareConfiguration configuration = computer.getHardwareConfiguration();
        return new VirtualMachineDescriptor(
                computer.getComputerId(),
                configuration.cpuCores(),
                configuration.memoryMb(),
                configuration.diskFile()
        );
    }
}
