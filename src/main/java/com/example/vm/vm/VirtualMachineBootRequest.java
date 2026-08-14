package com.example.vm.vm;

public record VirtualMachineBootRequest(String computerId, VirtualMachineBootDevice bootDevice, String isoPath) {
    public VirtualMachineBootRequest {
        if (computerId == null || computerId.isBlank()) {
            throw new IllegalArgumentException("computerId must not be blank");
        }
        if (bootDevice == null) {
            bootDevice = VirtualMachineBootDevice.VM_DISK;
        }
        if (isoPath == null) {
            isoPath = "";
        }
        if (bootDevice == VirtualMachineBootDevice.SYSTEM_DISC && isoPath.isBlank()) {
            throw new IllegalArgumentException("system disc boot requires an ISO path");
        }
    }

    public static VirtualMachineBootRequest vmDisk(String computerId) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.VM_DISK, "");
    }

    public static VirtualMachineBootRequest systemDisc(String computerId, String isoPath) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.SYSTEM_DISC, isoPath);
    }
}
