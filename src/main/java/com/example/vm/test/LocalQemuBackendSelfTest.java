package com.example.vm.test;

import com.example.vm.MineVM;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.floppy.FloppyMedia;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.FirmwareConfiguration;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.backend.local.LocalQemuBackend;
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
        assertFreeDosCommand(builder);
        assertWindows95Command(builder);
        assertExperimentalGuestCommands(builder);
        assertWindowsXpCommand(builder);
        assertWindows10Command(builder);
        assertWindows11Command(builder);
        assertPreflightRejectsMissingIso();
        assertIdleInputResetsQmpSession();
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
        assertContains(command, "-nic");
        assertContains(command, "user,model=virtio-net-pci");
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
        assertContains(command, "-accel");
        assertContains(command, "tcg,thread=multi");
        assertNotContains(command, "whpx");
        assertContains(command, "if=pflash,format=raw,readonly=on,file=D:\\qemu\\share\\edk2-x86_64-code.fd");
        assertContains(command, "if=pflash,format=raw,file=E:\\MinecraftVMs\\computer-windows-10\\OVMF_VARS.fd");
        assertContains(command, "-cpu");
        assertContains(command, "max");
        assertValueAfter(command, "-smp", "2");
        assertContains(command, "ich9-ahci,id=sata");
        assertContains(command, "if=none,file=E:\\MinecraftVMs\\computer-windows-10\\computer-windows-10.qcow2,format=qcow2,id=vm_disk");
        assertContains(command, "ide-hd,bus=sata.1,drive=vm_disk,bootindex=1");

        List<String> discCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-windows-10", "E:\\Downloads\\Win10.iso"));
        assertContains(discCommand, "if=none,file=E:\\Downloads\\Win10.iso,media=cdrom,readonly=on,id=system_disc");
        assertContains(discCommand, "ide-cd,bus=sata.0,drive=system_disc,bootindex=1");
        assertContains(discCommand, "ide-hd,bus=sata.1,drive=vm_disk,bootindex=2");
        assertContains(discCommand, "-vga");
        assertContains(discCommand, "std");
        assertContains(discCommand, "-nic");
        assertContains(discCommand, "user,model=e1000e");
    }

    private static void assertExperimentalGuestCommands(QemuCommandBuilder builder) {
        for (GuestOperatingSystem guest : new GuestOperatingSystem[] {GuestOperatingSystem.WINDOWS_98, GuestOperatingSystem.WINDOWS_VISTA}) {
            VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor("experimental-" + guest.name(), 4, 4096, "disk.qcow2", 32, guest, FirmwareConfiguration.forGuest(guest));
            List<String> command = builder.startCommand(descriptor);
            assertContains(command, "if=floppy,index=0,id=boot_floppy");
            assertContains(command, "file=" + builder.diskPath(descriptor) + ",format=qcow2,if=ide,index=0,media=disk,id=vm_disk");
            assertNotContains(command, "q35");
            assertNotContains(command, "virtio");
            assertValueAfter(command, "-smp", guest == GuestOperatingSystem.WINDOWS_98 ? "1" : "2");
        }
    }

    private static void assertWindowsXpCommand(QemuCommandBuilder builder) {
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-windows-xp",
                4,
                2048,
                "computer-windows-xp.qcow2",
                10,
                GuestOperatingSystem.WINDOWS_XP,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_XP)
        );
        List<String> command = builder.startCommand(descriptor);
        assertNotContains(command, "-machine");
        assertNotContains(command, "q35");
        assertContains(command, "-accel");
        assertContains(command, "tcg,thread=multi");
        assertContains(command, "-cpu");
        assertContains(command, "qemu32");
        assertValueAfter(command, "-smp", "2");
        assertContains(command, "file=E:\\MinecraftVMs\\computer-windows-xp\\computer-windows-xp.qcow2,format=qcow2,if=ide,index=0,media=disk,id=vm_disk");
        assertContains(command, "-vga");
        assertContains(command, "cirrus");
        assertContains(command, "-usb");
        assertContains(command, "usb-tablet");
        assertContains(command, "-nic");
        assertContains(command, "user,model=rtl8139");
        assertNotContains(command, "qemu-xhci,id=usb_bus");
        assertNotContains(command, "virtio");

        List<String> discCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-windows-xp", "E:\\Downloads\\WinXP.iso"));
        assertContains(discCommand, "file=E:\\Downloads\\WinXP.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(discCommand, "-boot");
        assertContains(discCommand, "order=d");

        List<String> secondStageCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.vmDiskWithAttachedDisc("computer-windows-xp", "E:\\Downloads\\WinXP.iso"));
        assertContains(secondStageCommand, "file=E:\\Downloads\\WinXP.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(secondStageCommand, "-boot");
        assertContains(secondStageCommand, "order=c");
    }

    private static void assertWindows95Command(QemuCommandBuilder builder) {
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-windows-95",
                4,
                1024,
                "computer-windows-95.qcow2",
                2,
                GuestOperatingSystem.WINDOWS_95,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_95)
        );
        List<String> command = builder.startCommand(descriptor);
        assertContains(command, "-machine");
        assertContains(command, "pc");
        assertContains(command, "if=floppy,index=0,id=boot_floppy");
        assertContains(command, "-accel");
        assertContains(command, "tcg,thread=single");
        assertContains(command, "-cpu");
        assertContains(command, "pentium");
        assertValueAfter(command, "-m", "256");
        assertValueAfter(command, "-smp", "1");
        assertContains(command, "file=E:\\MinecraftVMs\\computer-windows-95\\computer-windows-95.qcow2,format=qcow2,if=ide,index=0,media=disk,id=vm_disk");
        assertContains(command, "-vga");
        assertContains(command, "cirrus");
        assertContains(command, "-usb");
        assertContains(command, "usb-tablet");
        assertContains(command, "-nic");
        assertContains(command, "user,model=rtl8139");
        assertNotContains(command, "qemu-xhci,id=usb_bus");
        assertNotContains(command, "virtio");

        List<String> discCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-windows-95", "E:\\Downloads\\Win95.iso"));
        assertContains(discCommand, "file=E:\\Downloads\\Win95.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(discCommand, "-boot");
        assertContains(discCommand, "order=d");

        List<String> floppyBootCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDiscWithBootFloppy("computer-windows-95", "E:\\Downloads\\Win95.iso", "E:\\Downloads\\Win95Boot.img"));
        assertContains(floppyBootCommand, "file=E:\\Downloads\\Win95.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(floppyBootCommand, "file=E:\\Downloads\\Win95Boot.img,format=raw,if=floppy,index=0,readonly=on,id=boot_floppy");
        assertContains(floppyBootCommand, "-boot");
        assertContains(floppyBootCommand, "order=a");

        List<String> secondStageCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.vmDiskWithAttachedDisc("computer-windows-95", "E:\\Downloads\\Win95.iso"));
        assertContains(secondStageCommand, "file=E:\\Downloads\\Win95.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(secondStageCommand, "-boot");
        assertContains(secondStageCommand, "order=c");

        String mediaId = "fca6d5ca-5a1c-4c81-89a1-d611c3aab919";
        List<String> writableCommand = builder.startCommand(descriptor,
                VirtualMachineBootRequest.vmDisk(descriptor.computerId()).withFloppyMedia(FloppyMedia.blank(mediaId), ""));
        assertContains(writableCommand, "file=E:\\MinecraftVMs\\floppies\\" + mediaId + ".img,format=raw,if=floppy,index=0,readonly=off,id=boot_floppy");
        assertContains(writableCommand, "order=c");
        List<String> attachedBootCommand = builder.startCommand(descriptor,
                VirtualMachineBootRequest.vmDisk(descriptor.computerId())
                        .withFloppyMedia(FloppyMedia.windows95Boot(), "E:\\Downloads\\Win95Boot.img"));
        assertContains(attachedBootCommand, "file=E:\\Downloads\\Win95Boot.img,format=raw,if=floppy,index=0,readonly=on,id=boot_floppy");
        assertContains(attachedBootCommand, "order=c");

        assertEquals(
                builder.blankFloppyPath("fca6d5ca-5a1c-4c81-89a1-d611c3aab919").toString(),
                "E:\\MinecraftVMs\\floppies\\fca6d5ca-5a1c-4c81-89a1-d611c3aab919.img"
        );
    }

    private static void assertFreeDosCommand(QemuCommandBuilder builder) {
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(
                "computer-freedos",
                4,
                512,
                "computer-freedos.qcow2",
                1,
                GuestOperatingSystem.FREEDOS,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.FREEDOS)
        );
        List<String> command = builder.startCommand(descriptor);
        assertNotContains(command, "-machine");
        assertContains(command, "-accel");
        assertContains(command, "tcg,thread=multi");
        assertContains(command, "-cpu");
        assertContains(command, "qemu32");
        assertValueAfter(command, "-smp", "1");
        assertContains(command, "file=E:\\MinecraftVMs\\computer-freedos\\computer-freedos.qcow2,format=qcow2,if=ide,index=0,media=disk,id=vm_disk");
        assertContains(command, "-vga");
        assertContains(command, "std");
        assertContains(command, "-nic");
        assertContains(command, "user,model=rtl8139");
        assertNotContains(command, "usb-tablet");
        assertNotContains(command, "qemu-xhci,id=usb_bus");
        assertNotContains(command, "virtio");

        List<String> discCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.systemDisc("computer-freedos", "E:\\Downloads\\FD14LIVE.iso"));
        assertContains(discCommand, "file=E:\\Downloads\\FD14LIVE.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(discCommand, "-boot");
        assertContains(discCommand, "order=d");

        List<String> secondStageCommand = builder.startCommand(descriptor, VirtualMachineBootRequest.vmDiskWithAttachedDisc("computer-freedos", "E:\\Downloads\\FD14LIVE.iso"));
        assertContains(secondStageCommand, "file=E:\\Downloads\\FD14LIVE.iso,media=cdrom,readonly=on,if=ide,index=2,id=system_disc");
        assertContains(secondStageCommand, "-boot");
        assertContains(secondStageCommand, "order=c");
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
        String managerClassName = MineVM.VM_MANAGER.getClass().getName();
        if (!managerClassName.endsWith("VirtualMachineManager")) {
            throw new AssertionError("Unexpected VM manager type: " + managerClassName);
        }
    }

    private static void assertIdleInputResetsQmpSession() {
        long now = 50_000_000_000L;
        if (LocalQemuBackend.shouldResetInputQmpSession(now - 9_999_999_999L, now)) {
            throw new AssertionError("Input session should stay open before the idle reset threshold");
        }
        if (!LocalQemuBackend.shouldResetInputQmpSession(now - 10_000_000_000L, now)) {
            throw new AssertionError("Input session should reset at the idle reset threshold");
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

    private static void assertValueAfter(List<String> command, String option, String expectedValue) {
        int optionIndex = command.indexOf(option);
        if (optionIndex < 0 || optionIndex + 1 >= command.size()) {
            throw new AssertionError("Expected command to contain option " + option + " but got " + command);
        }
        assertEquals(command.get(optionIndex + 1), expectedValue);
    }

    private static void assertContains(String actual, String expected) {
        if (!actual.contains(expected)) {
            throw new AssertionError("Expected text to contain " + expected + " but got " + actual);
        }
    }

}
