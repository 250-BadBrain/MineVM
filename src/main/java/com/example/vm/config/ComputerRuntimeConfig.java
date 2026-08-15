package com.example.vm.config;

import com.example.vm.blockentity.ComputerBlockEntity;
import com.example.vm.computer.ComputerHardwareConfiguration;
import com.example.vm.vm.FirmwareConfiguration;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.backend.NoopVirtualMachineBackend;
import com.example.vm.vm.backend.VirtualMachineBackend;
import com.example.vm.vm.backend.local.LocalQemuBackend;
import com.example.vm.vm.backend.local.LocalQemuConfiguration;
import net.minecraftforge.common.ForgeConfigSpec;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ComputerRuntimeConfig {
    public static final ForgeConfigSpec SPEC;
    public static final boolean DEFAULT_ENABLE_LOCAL_QEMU = false;
    public static final String DEFAULT_QEMU_SYSTEM_PATH = "D:\\qemu\\qemu-system-x86_64.exe";
    public static final String DEFAULT_QEMU_IMG_PATH = "D:\\qemu\\qemu-img.exe";
    public static final String DEFAULT_VM_STORAGE_DIRECTORY = "run\\computer-vms";
    public static final String DEFAULT_ACCELERATOR = "whpx";
    public static final int DEFAULT_DISK_SIZE_GB = 20;
    public static final int DEFAULT_UBUNTU_DESKTOP_DISK_SIZE_GB = 25;
    public static final int DEFAULT_WINDOWS_DISK_SIZE_GB = 64;
    public static final String DEFAULT_ISO_PATH = "";
    public static final String DEFAULT_UBUNTU_SERVER_ISO_PATH = "";
    public static final String DEFAULT_UBUNTU_DESKTOP_ISO_PATH = "";
    public static final String DEFAULT_WINDOWS_10_ISO_PATH = "";
    public static final String DEFAULT_WINDOWS_11_ISO_PATH = "";
    public static final String DEFAULT_GUEST_OPERATING_SYSTEM = "UBUNTU_SERVER";

    private static final ForgeConfigSpec.BooleanValue ENABLE_LOCAL_QEMU;
    private static final ForgeConfigSpec.ConfigValue<String> QEMU_SYSTEM_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> QEMU_IMG_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> VM_STORAGE_DIRECTORY;
    private static final ForgeConfigSpec.ConfigValue<String> ACCELERATOR;
    private static final ForgeConfigSpec.IntValue DISK_SIZE_GB;
    private static final ForgeConfigSpec.ConfigValue<String> ISO_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> UBUNTU_SERVER_ISO_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> UBUNTU_DESKTOP_ISO_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> WINDOWS_10_ISO_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> WINDOWS_11_ISO_PATH;
    private static final ForgeConfigSpec.ConfigValue<String> GUEST_OPERATING_SYSTEM;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("vm");

        ENABLE_LOCAL_QEMU = builder
                .comment("Enable the local QEMU backend. Keep false unless QEMU is installed and virtualization is available.")
                .define("enableLocalQemu", DEFAULT_ENABLE_LOCAL_QEMU);
        QEMU_SYSTEM_PATH = builder
                .comment("Path to qemu-system-x86_64.exe.")
                .define("qemuSystemPath", DEFAULT_QEMU_SYSTEM_PATH);
        QEMU_IMG_PATH = builder
                .comment("Path to qemu-img.exe.")
                .define("qemuImgPath", DEFAULT_QEMU_IMG_PATH);
        VM_STORAGE_DIRECTORY = builder
                .comment("Directory used for managed VM disk files.")
                .define("vmStorageDirectory", DEFAULT_VM_STORAGE_DIRECTORY);
        ACCELERATOR = builder
                .comment("QEMU accelerator to request on Windows.")
                .define("accelerator", DEFAULT_ACCELERATOR);
        DISK_SIZE_GB = builder
                .comment("Default qcow2 disk size for newly created computers.")
                .defineInRange("diskSizeGb", DEFAULT_DISK_SIZE_GB, 1, 2048);
        ISO_PATH = builder
                .comment("Legacy fallback ISO path used when an OS-specific disc ISO path is empty.")
                .define("isoPath", DEFAULT_ISO_PATH);
        UBUNTU_SERVER_ISO_PATH = builder
                .comment("ISO path used when booting an inserted Ubuntu Server system disc.")
                .define("ubuntuServerIsoPath", DEFAULT_UBUNTU_SERVER_ISO_PATH);
        UBUNTU_DESKTOP_ISO_PATH = builder
                .comment("ISO path used when booting an inserted Ubuntu Desktop system disc.")
                .define("ubuntuDesktopIsoPath", DEFAULT_UBUNTU_DESKTOP_ISO_PATH);
        WINDOWS_10_ISO_PATH = builder
                .comment("ISO path used when booting an inserted Windows 10 system disc.")
                .define("windows10IsoPath", DEFAULT_WINDOWS_10_ISO_PATH);
        WINDOWS_11_ISO_PATH = builder
                .comment("ISO path used when booting an inserted Windows 11 system disc.")
                .define("windows11IsoPath", DEFAULT_WINDOWS_11_ISO_PATH);
        GUEST_OPERATING_SYSTEM = builder
                .comment("Guest OS profile: UBUNTU_SERVER, UBUNTU_DESKTOP, WINDOWS_10, or WINDOWS_11. Legacy LINUX maps to UBUNTU_SERVER.")
                .define("guestOperatingSystem", DEFAULT_GUEST_OPERATING_SYSTEM);

        builder.pop();
        SPEC = builder.build();
    }

    private ComputerRuntimeConfig() {
    }

    public static VirtualMachineBackend createBackend() {
        if (!localQemuEnabled()) {
            return new NoopVirtualMachineBackend();
        }

        return new LocalQemuBackend(new LocalQemuConfiguration(
                qemuSystemPath(),
                qemuImgPath(),
                vmStorageDirectory(),
                accelerator()
        ));
    }

    public static boolean localQemuEnabled() {
        return valueOrDefault(ENABLE_LOCAL_QEMU, DEFAULT_ENABLE_LOCAL_QEMU);
    }

    public static Path qemuSystemPath() {
        return Path.of(valueOrDefault(QEMU_SYSTEM_PATH, DEFAULT_QEMU_SYSTEM_PATH));
    }

    public static Path qemuImgPath() {
        return Path.of(valueOrDefault(QEMU_IMG_PATH, DEFAULT_QEMU_IMG_PATH));
    }

    public static Path vmStorageDirectory() {
        return Path.of(valueOrDefault(VM_STORAGE_DIRECTORY, DEFAULT_VM_STORAGE_DIRECTORY));
    }

    public static String accelerator() {
        return valueOrDefault(ACCELERATOR, DEFAULT_ACCELERATOR);
    }

    public static VirtualMachineDescriptor descriptorFor(ComputerBlockEntity computer) {
        return descriptorFor(computer, configuredGuestOperatingSystem(computer));
    }

    public static VirtualMachineDescriptor descriptorFor(ComputerBlockEntity computer, GuestOperatingSystem guestOperatingSystem) {
        return descriptor(computer.getComputerId(), computer.getHardwareConfiguration(), guestOperatingSystem);
    }

    public static VirtualMachineDescriptor descriptor(String computerId, ComputerHardwareConfiguration configuration) {
        return descriptor(computerId, configuration, configuredGuestOperatingSystem());
    }

    public static VirtualMachineDescriptor descriptor(String computerId, ComputerHardwareConfiguration configuration, GuestOperatingSystem guestOperatingSystem) {
        return new VirtualMachineDescriptor(
                computerId,
                configuration.cpuCores(),
                configuration.memoryMb(),
                configuration.diskFile(),
                Math.max(valueOrDefault(DISK_SIZE_GB, DEFAULT_DISK_SIZE_GB), minimumDiskSizeGb(guestOperatingSystem)),
                guestOperatingSystem,
                FirmwareConfiguration.forGuest(guestOperatingSystem)
        );
    }

    public static int minimumDiskSizeGb(GuestOperatingSystem guestOperatingSystem) {
        return switch (guestOperatingSystem) {
            case UBUNTU_DESKTOP -> DEFAULT_UBUNTU_DESKTOP_DISK_SIZE_GB;
            case WINDOWS_10, WINDOWS_11 -> DEFAULT_WINDOWS_DISK_SIZE_GB;
            case UBUNTU_SERVER, LINUX -> DEFAULT_DISK_SIZE_GB;
        };
    }

    public static boolean vmDiskExists(ComputerBlockEntity computer) {
        return Files.isRegularFile(vmDiskPathFor(computer));
    }

    public static Path vmDiskPathFor(ComputerBlockEntity computer) {
        return vmDiskPathFor(descriptorFor(computer));
    }

    public static Path vmDiskPathFor(VirtualMachineDescriptor descriptor) {
        Path root = vmStorageDirectory().toAbsolutePath().normalize();
        Path diskPath = root.resolve(descriptor.computerId()).resolve(descriptor.diskFile()).normalize();
        if (!diskPath.startsWith(root)) {
            throw new IllegalArgumentException("VM disk path escapes storage directory: " + descriptor.diskFile());
        }
        return diskPath;
    }

    public static String isoPathFor(GuestOperatingSystem guestOperatingSystem) {
        String isoPath = switch (guestOperatingSystem) {
            case WINDOWS_10 -> valueOrDefault(WINDOWS_10_ISO_PATH, DEFAULT_WINDOWS_10_ISO_PATH);
            case WINDOWS_11 -> valueOrDefault(WINDOWS_11_ISO_PATH, DEFAULT_WINDOWS_11_ISO_PATH);
            case UBUNTU_SERVER, LINUX -> valueOrDefault(UBUNTU_SERVER_ISO_PATH, DEFAULT_UBUNTU_SERVER_ISO_PATH);
            case UBUNTU_DESKTOP -> valueOrDefault(UBUNTU_DESKTOP_ISO_PATH, DEFAULT_UBUNTU_DESKTOP_ISO_PATH);
        };

        if (isoPath == null || isoPath.isBlank()) {
            return valueOrDefault(ISO_PATH, DEFAULT_ISO_PATH);
        }
        return isoPath;
    }

    public static GuestOperatingSystem configuredGuestOperatingSystem(ComputerBlockEntity computer) {
        return computer.selectedGuestOperatingSystem().orElseGet(ComputerRuntimeConfig::configuredGuestOperatingSystem);
    }

    public static GuestOperatingSystem configuredGuestOperatingSystem() {
        return GuestOperatingSystem.parse(valueOrDefault(GUEST_OPERATING_SYSTEM, DEFAULT_GUEST_OPERATING_SYSTEM));
    }

    private static <T> T valueOrDefault(ForgeConfigSpec.ConfigValue<T> value, T defaultValue) {
        try {
            return value.get();
        } catch (IllegalStateException exception) {
            return defaultValue;
        }
    }
}
