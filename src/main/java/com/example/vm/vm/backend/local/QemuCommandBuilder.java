package com.example.vm.vm.backend.local;

import com.example.vm.vm.VirtualMachineBootDevice;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.screen.VncEndpoint;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class QemuCommandBuilder {
    public static final String SYSTEM_DISC_DEVICE_ID = "system_disc";
    private static final String VM_DISK_DEVICE_ID = "vm_disk";

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
        command.add("-accel");
        command.add(configuration.accelerator());
        if (descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-machine");
            command.add("q35");
        }
        command.add("-m");
        command.add(Integer.toString(descriptor.memoryMb()));
        command.add("-smp");
        command.add(Integer.toString(descriptor.cpuCores()));
        addFirmwareArgs(command, descriptor);
        if (descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-device");
            command.add("ich9-ahci,id=sata");
        }
        addDiskArgs(command, descriptor, bootRequest.bootDevice());

        if (bootRequest.bootDevice() == VirtualMachineBootDevice.SYSTEM_DISC) {
            addSystemDiscArgs(command, descriptor, bootRequest);
            command.add("-boot");
            command.add("order=d");
        } else {
            command.add("-boot");
            command.add("order=c");
        }

        command.add("-display");
        command.add("none");
        command.add("-vga");
        command.add("virtio");
        command.add("-device");
        command.add("qemu-xhci,id=usb_bus");
        command.add("-device");
        command.add("usb-tablet,bus=usb_bus.0");
        command.add("-vnc");
        command.add(VncEndpoint.forComputer(descriptor.computerId()).toQemuArgument() + ",share=ignore");
        command.add("-qmp");
        command.add(QmpEndpoint.forComputer(descriptor.computerId()).toQemuArgument());
        command.add("-name");
        command.add(descriptor.computerId());
        return List.copyOf(command);
    }

    private void addDiskArgs(List<String> command, VirtualMachineDescriptor descriptor, VirtualMachineBootDevice bootDevice) {
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
        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            command.add("-drive");
            command.add("file=" + bootRequest.isoPath() + ",media=cdrom,readonly=on,id=" + SYSTEM_DISC_DEVICE_ID);
            return;
        }

        command.add("-drive");
        command.add("if=none,file=" + bootRequest.isoPath() + ",media=cdrom,readonly=on,id=" + SYSTEM_DISC_DEVICE_ID);
        command.add("-device");
        command.add("ide-cd,bus=sata.0,drive=" + SYSTEM_DISC_DEVICE_ID + ",bootindex=1");
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
