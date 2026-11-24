package com.example.vm.vm.backend.local;

import com.example.vm.vm.VirtualMachineBootDevice;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.MineVM;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.screen.VncEndpoint;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class QemuCommandBuilder {
    public static final String SYSTEM_DISC_DEVICE_ID = "system_disc";
    public static final String BOOT_FLOPPY_DEVICE_ID = "boot_floppy";
    private static final String VM_DISK_DEVICE_ID = "vm_disk";
    private static final String WINDOWS_95_WORKING_BOOT_FLOPPY = "windows95-boot-working.img";

    private final LocalQemuConfiguration configuration;
    private final LocalVmPaths paths;

    public QemuCommandBuilder(LocalQemuConfiguration configuration) {
        this.configuration = configuration;
        this.paths = new LocalVmPaths(configuration);
    }

    public Path diskPath(VirtualMachineDescriptor descriptor) {
        return paths.diskPath(descriptor);
    }

    public Path vmDirectory(VirtualMachineDescriptor descriptor) {
        return paths.vmDirectory(descriptor);
    }

    public Path blankFloppyPath(String mediaId) {
        java.util.UUID.fromString(mediaId);
        return paths.floppyDirectory().resolve(mediaId + ".img").normalize();
    }

    public Path preparedBootFloppyPath(VirtualMachineDescriptor descriptor, String bootFloppyPath) {
        return effectiveBootFloppyPath(descriptor, bootFloppyPath);
    }

    public List<String> createDiskCommand(VirtualMachineDescriptor descriptor) {
        return List.of(
                configuration.qemuImgPath().toString(),
                "create",
                "-f",
                "qcow2",
                diskPath(descriptor).toString(),
                descriptor.diskSizeGb() + "G"
        );
    }

    public List<String> startCommand(VirtualMachineDescriptor descriptor) {
        return startCommand(descriptor, VirtualMachineBootRequest.vmDisk(descriptor.computerId()));
    }

    public List<String> startCommand(VirtualMachineDescriptor descriptor, VirtualMachineBootRequest bootRequest) {
        if (!descriptor.computerId().equals(bootRequest.computerId())) {
            throw new IllegalArgumentException("Boot request computer id does not match descriptor");
        }

        List<String> command = new ArrayList<>();
        command.add(configuration.qemuSystemPath().toString());
        if (descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_95 || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_98) {
            command.add("-machine");
            command.add("pc");
        } else if (descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-machine");
            command.add("q35");
        }
        command.add("-accel");
        command.add(accelerator(descriptor));
        addCpuArgs(command, descriptor);
        command.add("-m");
        command.add(Integer.toString(effectiveMemoryMb(descriptor)));
        command.add("-smp");
        command.add(Integer.toString(effectiveCpuCores(descriptor)));
        addFirmwareArgs(command, descriptor);
        if (descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-device");
            command.add("ich9-ahci,id=sata");
        }
        addDiskArgs(command, descriptor, bootRequest.bootDevice());
        if (usesLegacyIdeStorage(descriptor) || bootRequest.floppyMedia() != null) {
            addFloppyArgs(command, descriptor, bootRequest);
        }

        if (bootRequest.bootDevice() == VirtualMachineBootDevice.SYSTEM_DISC) {
            addSystemDiscArgs(command, descriptor, bootRequest);
            command.add("-boot");
            command.add(bootRequest.hasBootFloppy() ? "order=a" : "order=d");
        } else {
            if (bootRequest.hasAttachedDisc()) {
                addAttachedSystemDiscArgs(command, descriptor, bootRequest);
            }
            command.add("-boot");
            command.add("order=c");
        }

        command.add("-display");
        command.add("none");
        command.add("-vga");
        command.add(vgaDevice(descriptor));
        addInputDeviceArgs(command, descriptor);
        addNetworkArgs(command, descriptor);
        command.add("-vnc");
        command.add(VncEndpoint.forComputer(descriptor.computerId()).toQemuArgument() + ",share=ignore");
        command.add("-qmp");
        command.add(QmpEndpoint.forComputer(descriptor.computerId()).toQemuArgument());
        command.add("-name");
        command.add(descriptor.computerId());
        return List.copyOf(command);
    }

    private void addCpuArgs(List<String> command, VirtualMachineDescriptor descriptor) {
        switch (descriptor.guestOperatingSystem()) {
            case FREEDOS, WINDOWS_XP, WINDOWS_VISTA -> {
                command.add("-cpu");
                command.add("qemu32");
            }
            case WINDOWS_95, WINDOWS_98 -> {
                command.add("-cpu");
                command.add("pentium");
            }
            case WINDOWS_10, WINDOWS_11 -> {
                command.add("-cpu");
                command.add("max");
            }
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> {
            }
        }
    }

    private int effectiveCpuCores(VirtualMachineDescriptor descriptor) {
        return switch (descriptor.guestOperatingSystem()) {
            case FREEDOS, WINDOWS_95, WINDOWS_98 -> 1;
            case WINDOWS_XP, WINDOWS_VISTA -> Math.min(descriptor.cpuCores(), 2);
            case WINDOWS_10, WINDOWS_11 -> Math.min(descriptor.cpuCores(), 2);
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> descriptor.cpuCores();
        };
    }

    private String accelerator(VirtualMachineDescriptor descriptor) {
        return switch (descriptor.guestOperatingSystem()) {
            case WINDOWS_95, WINDOWS_98 -> "tcg,thread=single";
            case FREEDOS, WINDOWS_XP, WINDOWS_VISTA -> "tcg,thread=multi";
            case WINDOWS_10, WINDOWS_11 -> "tcg,thread=multi";
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> configuration.accelerator();
        };
    }

    private String vgaDevice(VirtualMachineDescriptor descriptor) {
        return switch (descriptor.guestOperatingSystem()) {
            case FREEDOS -> "std";
            case WINDOWS_95, WINDOWS_98, WINDOWS_XP -> "cirrus";
            case WINDOWS_VISTA -> "std";
            case WINDOWS_10, WINDOWS_11 -> "std";
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> "virtio";
        };
    }

    private void addInputDeviceArgs(List<String> command, VirtualMachineDescriptor descriptor) {
        if (descriptor.guestOperatingSystem() == GuestOperatingSystem.FREEDOS) {
            return;
        }

        if (descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_95
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_98
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_XP
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_VISTA) {
            command.add("-usb");
            command.add("-device");
            command.add("usb-tablet");
            return;
        }

        command.add("-device");
        command.add("qemu-xhci,id=usb_bus");
        command.add("-device");
        command.add("usb-tablet,bus=usb_bus.0");
    }

    private void addNetworkArgs(List<String> command, VirtualMachineDescriptor descriptor) {
        command.add("-nic");
        command.add("user,model=" + networkModel(descriptor));
    }

    private String networkModel(VirtualMachineDescriptor descriptor) {
        return switch (descriptor.guestOperatingSystem()) {
            case FREEDOS, WINDOWS_95, WINDOWS_98, WINDOWS_XP -> "rtl8139";
            case WINDOWS_VISTA -> "e1000";
            case WINDOWS_10, WINDOWS_11 -> "e1000e";
            case UBUNTU_SERVER, UBUNTU_DESKTOP, LINUX -> "virtio-net-pci";
        };
    }

    private void addDiskArgs(List<String> command, VirtualMachineDescriptor descriptor, VirtualMachineBootDevice bootDevice) {
        if (usesLegacyIdeStorage(descriptor)) {
            command.add("-drive");
            command.add("file=" + diskPath(descriptor) + ",format=qcow2,if=ide,index=0,media=disk,id=" + VM_DISK_DEVICE_ID);
            return;
        }

        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-drive");
            command.add("file=" + diskPath(descriptor) + ",format=qcow2,if=virtio");
            return;
        }

        command.add("-drive");
        command.add("if=none,file=" + diskPath(descriptor) + ",format=qcow2,id=" + VM_DISK_DEVICE_ID);
        command.add("-device");
        int bootIndex = bootDevice == VirtualMachineBootDevice.SYSTEM_DISC ? 2 : 1;
        command.add("ide-hd,bus=sata.1,drive=" + VM_DISK_DEVICE_ID + ",bootindex=" + bootIndex);
    }

    private void addSystemDiscArgs(List<String> command, VirtualMachineDescriptor descriptor, VirtualMachineBootRequest bootRequest) {
        addSystemDiscArgs(command, descriptor, bootRequest.isoPath(), true);
    }

    private void addAttachedSystemDiscArgs(List<String> command, VirtualMachineDescriptor descriptor, VirtualMachineBootRequest bootRequest) {
        addSystemDiscArgs(command, descriptor, bootRequest.attachedIsoPath(), false);
    }

    private void addFloppyArgs(List<String> command, VirtualMachineDescriptor descriptor, VirtualMachineBootRequest bootRequest) {
        command.add("-drive");
        if (bootRequest.hasBootFloppy()) {
            command.add("file=" + effectiveBootFloppyPath(descriptor, bootRequest.bootFloppyPath()) + ",format=raw,if=floppy,index=0,readonly=on,id=" + BOOT_FLOPPY_DEVICE_ID);
        } else if (bootRequest.floppyMedia() != null && bootRequest.floppyMedia().isWritable()) {
            command.add("file=" + blankFloppyPath(bootRequest.floppyMedia().mediaId()) + ",format=raw,if=floppy,index=0,readonly=off,id=" + BOOT_FLOPPY_DEVICE_ID);
        } else {
            command.add("if=floppy,index=0,id=" + BOOT_FLOPPY_DEVICE_ID);
        }
    }

    private Path effectiveBootFloppyPath(VirtualMachineDescriptor descriptor, String bootFloppyPath) {
        Path source = Path.of(bootFloppyPath);
        if (descriptor.guestOperatingSystem() != GuestOperatingSystem.WINDOWS_95 || !Files.isRegularFile(source)) {
            return source;
        }

        Path workingCopy = vmDirectory(descriptor).resolve(WINDOWS_95_WORKING_BOOT_FLOPPY).normalize();
        try {
            Files.createDirectories(workingCopy.getParent());
            byte[] bytes = Files.readAllBytes(source);
            patchWindows95BootFloppyCdDriveLetter(bytes);
            Files.write(workingCopy, bytes);
            return workingCopy;
        } catch (IOException exception) {
            if (Files.isRegularFile(workingCopy)) {
                MineVM.LOGGER.warn("Using existing patched Windows 95 boot floppy for {}", descriptor.computerId(), exception);
                return workingCopy;
            }
            MineVM.LOGGER.warn("Failed to prepare patched Windows 95 boot floppy for {}", descriptor.computerId(), exception);
            return source;
        }
    }

    private static void patchWindows95BootFloppyCdDriveLetter(byte[] bytes) {
        replaceAsciiPadded(bytes, "LH A:\\MSCDEX.EXE /D:mscd001 /L:%CDROM%", "LH A:\\MSCDEX.EXE /D:mscd001 /L:D");
        replaceAsciiPadded(bytes, "LH A:\\MSCDEX.EXE /D:MSCD001 /L:%CDROM%", "LH A:\\MSCDEX.EXE /D:MSCD001 /L:D");
        replaceAsciiPadded(bytes, "LH A:\\MSCDEX.EXE /D:mscd001", "MSCDEX /D:mscd001 /L:D");
        replaceAsciiPadded(bytes, "LH A:\\MSCDEX.EXE /D:MSCD001", "MSCDEX /D:MSCD001 /L:D");
        replaceAscii(bytes, "/L:C", "/L:D");
        replaceAscii(bytes, "/L:c", "/L:D");
        replaceAscii(bytes, "/l:C", "/L:D");
        replaceAscii(bytes, "/l:c", "/L:D");
    }

    private static void replaceAscii(byte[] bytes, String needle, String replacement) {
        byte[] needleBytes = needle.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] replacementBytes = replacement.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (needleBytes.length != replacementBytes.length) {
            throw new IllegalArgumentException("Replacement must preserve boot floppy size");
        }

        for (int index = 0; index <= bytes.length - needleBytes.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < needleBytes.length; offset++) {
                if (bytes[index + offset] != needleBytes[offset]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                System.arraycopy(replacementBytes, 0, bytes, index, replacementBytes.length);
            }
        }
    }

    private static void replaceAsciiPadded(byte[] bytes, String needle, String replacement) {
        if (replacement.length() > needle.length()) {
            throw new IllegalArgumentException("Replacement must not grow boot floppy files");
        }
        replaceAscii(bytes, needle, replacement + " ".repeat(needle.length() - replacement.length()));
    }

    private void addSystemDiscArgs(List<String> command, VirtualMachineDescriptor descriptor, String isoPath, boolean bootable) {
        if (usesLegacyIdeStorage(descriptor)) {
            command.add("-drive");
            command.add("file=" + isoPath + ",media=cdrom,readonly=on,if=ide,index=2,id=" + SYSTEM_DISC_DEVICE_ID);
            return;
        }

        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-drive");
            command.add("file=" + isoPath + ",media=cdrom,readonly=on,id=" + SYSTEM_DISC_DEVICE_ID);
            return;
        }

        command.add("-drive");
        command.add("if=none,file=" + isoPath + ",media=cdrom,readonly=on,id=" + SYSTEM_DISC_DEVICE_ID);
        command.add("-device");
        String bootIndex = bootable ? ",bootindex=1" : "";
        command.add("ide-cd,bus=sata.0,drive=" + SYSTEM_DISC_DEVICE_ID + bootIndex);
    }

    private boolean usesLegacyIdeStorage(VirtualMachineDescriptor descriptor) {
        return descriptor.guestOperatingSystem() == GuestOperatingSystem.FREEDOS
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_95
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_98
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_VISTA
                || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_XP;
    }

    private int effectiveMemoryMb(VirtualMachineDescriptor descriptor) {
        if (descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_95 || descriptor.guestOperatingSystem() == GuestOperatingSystem.WINDOWS_98) {
            return Math.min(descriptor.memoryMb(), 256);
        }
        return descriptor.memoryMb();
    }

    private void addFirmwareArgs(List<String> command, VirtualMachineDescriptor descriptor) {
        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            return;
        }

        command.add("-drive");
        command.add("if=pflash,format=raw,readonly=on,file=" + paths.uefiCodePath(descriptor.firmwareConfiguration().requiresSecureBoot()));
        command.add("-drive");
        command.add("if=pflash,format=raw,file=" + paths.ovmfVarsPath(descriptor));

        if (descriptor.firmwareConfiguration().requiresTpm()) {
            command.add("-chardev");
            command.add("socket,id=chrtpm,path=" + descriptor.computerId() + "-swtpm.sock");
            command.add("-tpmdev");
            command.add("emulator,id=tpm0,chardev=chrtpm");
            command.add("-device");
            command.add("tpm-tis,tpmdev=tpm0");
        }
    }
}
