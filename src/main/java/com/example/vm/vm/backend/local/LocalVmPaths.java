package com.example.vm.vm.backend.local;

import com.example.vm.vm.VirtualMachineDescriptor;

import java.nio.file.Path;

public final class LocalVmPaths {
    private final Path storageRoot;
    private final Path qemuShareDirectory;

    public LocalVmPaths(LocalQemuConfiguration configuration) {
        this.storageRoot = configuration.vmStorageDirectory().toAbsolutePath().normalize();
        this.qemuShareDirectory = configuration.qemuSystemPath().toAbsolutePath().normalize().getParent().resolve("share").normalize();
    }

    public Path storageRoot() {
        return storageRoot;
    }

    public Path vmDirectory(VirtualMachineDescriptor descriptor) {
        Path directory = storageRoot.resolve(descriptor.computerId()).normalize();
        if (!directory.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Computer id escapes VM storage root: " + descriptor.computerId());
        }
        return directory;
    }

    public Path diskPath(VirtualMachineDescriptor descriptor) {
        Path diskFile = Path.of(descriptor.diskFile());
        if (diskFile.isAbsolute() || diskFile.getNameCount() != 1) {
            throw new IllegalArgumentException("VM disk file must be a managed file name: " + descriptor.diskFile());
        }
        return vmDirectory(descriptor).resolve(diskFile).normalize();
    }

    public Path ovmfVarsPath(VirtualMachineDescriptor descriptor) {
        return vmDirectory(descriptor).resolve("OVMF_VARS.fd").normalize();
    }

    public Path uefiVarsTemplatePath() {
        return qemuShareDirectory.resolve("edk2-i386-vars.fd").normalize();
    }

    public Path uefiCodePath(boolean secureBoot) {
        String fileName = secureBoot ? "edk2-x86_64-secure-code.fd" : "edk2-x86_64-code.fd";
        return qemuShareDirectory.resolve(fileName).normalize();
    }
}
