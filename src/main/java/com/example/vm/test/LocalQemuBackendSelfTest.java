package com.example.vm.test;

import com.example.vm.ComputerInMinecraft;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.FirmwareConfiguration;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.backend.local.LocalQemuConfiguration;
import com.example.vm.vm.backend.local.LocalQemuPreflight;
import com.example.vm.vm.backend.local.QemuCommandBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LocalQemuBackendSelfTest {
    private LocalQemuBackendSelfTest() {
    }

    public static void main(String[] args) {
        LocalQemuConfiguration configuration = new LocalQemuConfiguration(
                Path.of("D:", "qemu", "qemu-system-x86_64.exe"),
                Path.of("D:", "qemu", "qemu-img.exe"),
                Path.of("E:\\MinecraftVMs"),
                "whpx"
        );
        QemuCommandBuilder builder = new QemuCommandBuilder(configuration);
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-test",
                4,
                4096,
                "computer-test.qcow2",
                20
        );

        assertCreateDiskCommand(builder.createDiskCommand(descriptor));
        assertDiscStartCommand(builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-test", "E:\\Downloads\\ubuntu-22.04.5-live-server-amd64.iso")));
        assertDiskStartCommand(builder.startCommand(descriptor));
        assertWindows10Command(builder);
        assertWindows11Command(builder);
        assertPreflightRejectsMissingIso();
        assertDefaultBackendRemainsSafe();
    }

    private static void assertCreateDiskCommand(List<String> command) {
        assertEquals(command.get(0), "D:\\qemu\\qemu-img.exe");
        assertEquals(command.get(1), "create");
        assertEquals(command.get(2), "-f");
        assertEquals(command.get(3), "qcow2");
        assertEquals(command.get(4), "E:\\MinecraftVMs\\computer-test\\computer-test.qcow2");
        assertEquals(command.get(5), "20G");
    }

    private static void assertDiscStartCommand(List<String> command) {
        assertContains(command, "D:\\qemu\\qemu-system-x86_64.exe");
        assertContains(command, "-accel");
        assertContains(command, "whpx");
        assertContains(command, "-m");
        assertContains(command, "4096");
        assertContains(command, "-smp");
        assertContains(command, "4");
        assertContains(command, "file=E:\\MinecraftVMs\\computer-test\\computer-test.qcow2,format=qcow2,if=virtio");
        assertContains(command, "file=E:\\Downloads\\ubuntu-22.04.5-live-server-amd64.iso,media=cdrom,readonly=on,id=system_disc");
        assertContains(command, "-boot");
        assertContains(command, "order=d");
        assertContains(command, "-display");
        assertContains(command, "none");
        assertContains(command, "-vga");
        assertContains(command, "virtio");
        assertNotContains(command, "std");
        assertContains(command, "-vnc");
        assertContains(command, VncEndpoint.forComputer("computer-test").toQemuArgument() + ",share=ignore");
        assertContains(command, "-qmp");
        assertContains(command, QmpEndpoint.forComputer("computer-test").toQemuArgument());
    }

    private static void assertDiskStartCommand(List<String> command) {
        assertContains(command, "D:\\qemu\\qemu-system-x86_64.exe");
        assertContains(command, "file=E:\\MinecraftVMs\\computer-test\\computer-test.qcow2,format=qcow2,if=virtio");
        assertNotContains(command, "file=E:\\Downloads\\ubuntu-22.04.5-live-server-amd64.iso,media=cdrom,readonly=on,id=system_disc");
        assertContains(command, "-boot");
        assertContains(command, "order=c");
    }

    private static void assertWindows10Command(QemuCommandBuilder builder) {
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-windows-10",
                4,
                8192,
                "computer-windows-10.qcow2",
                64,
                GuestOperatingSystem.WINDOWS_10,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_10)
        );
        List<String> command = builder.startCommand(descriptor);
        assertContains(command, "-machine");
        assertContains(command, "q35");
        assertContains(command, "if=pflash,format=raw,readonly=on,file=D:\\qemu\\share\\edk2-x86_64-code.fd");
        assertContains(command, "if=pflash,format=raw,file=E:\\MinecraftVMs\\computer-windows-10\\OVMF_VARS.fd");
        assertContains(command, "-cpu");
        assertContains(command, "qemu64");
        assertContains(command, "ich9-ahci,id=sata");
        assertContains(command, "if=none,file=E:\\MinecraftVMs\\computer-windows-10\\computer-windows-10.qcow2,format=qcow2,id=vm_disk");
        assertContains(command, "ide-hd,bus=sata.1,drive=vm_disk,bootindex=1");

        List<String> discCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-windows-10", "E:\\Downloads\\Win10.iso"));
        assertContains(discCommand, "if=none,file=E:\\Downloads\\Win10.iso,media=cdrom,readonly=on,id=system_disc");
        assertContains(discCommand, "ide-cd,bus=sata.0,drive=system_disc,bootindex=1");
        assertContains(discCommand, "ide-hd,bus=sata.1,drive=vm_disk,bootindex=2");
        assertContains(discCommand, "-vga");
        assertContains(discCommand, "std");
    }

    private static void assertWindows11Command(QemuCommandBuilder builder) {
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-windows-11",
                4,
                8192,
                "computer-windows-11.qcow2",
                64,
                GuestOperatingSystem.WINDOWS_11,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_11)
        );
        List<String> command = builder.startCommand(descriptor);
        assertContains(command, "if=pflash,format=raw,readonly=on,file=D:\\qemu\\share\\edk2-x86_64-secure-code.fd");
        assertContains(command, "if=pflash,format=raw,file=E:\\MinecraftVMs\\computer-windows-11\\OVMF_VARS.fd");
        assertContains(command, "-tpmdev");
        assertContains(command, "emulator,id=tpm0,chardev=chrtpm");
        assertContains(command, "-device");
        assertContains(command, "tpm-tis,tpmdev=tpm0");
    }

    private static void assertDefaultBackendRemainsSafe() {
        String managerClassName = ComputerInMinecraft.VM_MANAGER.getClass().getName();
        if (!managerClassName.endsWith("VirtualMachineManager")) {
            throw new AssertionError("Unexpected VM manager type: " + managerClassName);
        }
    }

    private static void assertPreflightRejectsMissingIso() {
        try {
            Path missingIso = Files.createTempDirectory("computer-vm-missing-iso").resolve("missing.iso");
            LocalQemuPreflight.validateBootRequest(VirtualMachineBootRequest.systemDisc("computer-test", missingIso.toString()));
            throw new AssertionError("Missing ISO path must fail preflight");
        } catch (IllegalStateException expected) {
            assertContains(expected.getMessage(), "System disc ISO does not exist");
        } catch (java.io.IOException exception) {
            throw new AssertionError("Failed to create preflight test directory", exception);
        }
    }

    private static void assertContains(List<String> command, String expected) {
        if (!command.contains(expected)) {
            throw new AssertionError("Expected command to contain " + expected + " but got " + command);
        }
    }

    private static void assertNotContains(List<String> command, String unexpected) {
        if (command.contains(unexpected)) {
            throw new AssertionError("Expected command not to contain " + unexpected + " but got " + command);
        }
    }

    private static void assertEquals(String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertContains(String actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("Expected text to contain " + expected + " but got " + actual);
        }
    }
}
