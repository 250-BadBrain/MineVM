package com.example.vm.vm;

import com.example.vm.floppy.FloppyMedia;

public record VirtualMachineBootRequest(String computerId, VirtualMachineBootDevice bootDevice, String isoPath, String attachedIsoPath, String bootFloppyPath, FloppyMedia floppyMedia) {
    public VirtualMachineBootRequest(String computerId, VirtualMachineBootDevice bootDevice, String isoPath, String attachedIsoPath, String bootFloppyPath) {
        this(computerId, bootDevice, isoPath, attachedIsoPath, bootFloppyPath,
                bootFloppyPath == null || bootFloppyPath.isBlank() ? null : FloppyMedia.windows95Boot());
    }

    public VirtualMachineBootRequest(String computerId, VirtualMachineBootDevice bootDevice, String isoPath) {
        this(computerId, bootDevice, isoPath, "");
    }

    public VirtualMachineBootRequest(String computerId, VirtualMachineBootDevice bootDevice, String isoPath, String attachedIsoPath) {
        this(computerId, bootDevice, isoPath, attachedIsoPath, "");
    }

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
        if (attachedIsoPath == null) {
            attachedIsoPath = "";
        }
        if (bootFloppyPath == null) {
            bootFloppyPath = "";
        }
        if (floppyMedia != null && floppyMedia.isWritable() && !bootFloppyPath.isBlank()) {
            throw new IllegalArgumentException("Writable floppy must not use a boot image path");
        }
        if (bootDevice == VirtualMachineBootDevice.SYSTEM_DISC && isoPath.isBlank()) {
            throw new IllegalArgumentException("system disc boot requires an ISO path");
        }
    }

    public static VirtualMachineBootRequest vmDisk(String computerId) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.VM_DISK, "");
    }

    public static VirtualMachineBootRequest vmDiskWithAttachedDisc(String computerId, String isoPath) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.VM_DISK, "", isoPath);
    }

    public static VirtualMachineBootRequest systemDisc(String computerId, String isoPath) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.SYSTEM_DISC, isoPath);
    }

    public static VirtualMachineBootRequest systemDiscWithBootFloppy(String computerId, String isoPath, String bootFloppyPath) {
        return new VirtualMachineBootRequest(computerId, VirtualMachineBootDevice.SYSTEM_DISC, isoPath, "", bootFloppyPath);
    }

    public boolean hasAttachedDisc() {
        return !attachedIsoPath.isBlank();
    }

    public boolean hasBootFloppy() {
        return !bootFloppyPath.isBlank();
    }

    public VirtualMachineBootRequest withFloppyMedia(FloppyMedia media, String bootImagePath) {
        return new VirtualMachineBootRequest(computerId, bootDevice, isoPath, attachedIsoPath,
                media != null && media.isWindows95Boot() ? bootImagePath : "", media);
    }
}
