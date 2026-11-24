package com.example.vm.test;

import com.example.vm.floppy.FloppyMedia;
import com.example.vm.qmp.QmpClient;
import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.vm.FirmwareConfiguration;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.DiskMaintenance;
import com.example.vm.vm.backend.local.LocalQemuBackend;
import com.example.vm.vm.backend.local.LocalQemuConfiguration;
import com.example.vm.vm.backend.local.QemuCommandBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Opt-in check against real QEMU, using only a disposable VM directory. */
public final class QemuMediaIntegrationSelfTest {
    public static void main(String[] args) throws Exception {
        Path qemu = Path.of(args[0]).toAbsolutePath();
        Path root = Files.createTempDirectory("minevm-media-integration-");
        String computerId = "media-test-" + UUID.randomUUID();
        String mediaId = UUID.randomUUID().toString();
        LocalQemuConfiguration configuration = new LocalQemuConfiguration(qemu,
                qemu.resolveSibling("qemu-img.exe"), root, "tcg");
        QemuCommandBuilder builder = new QemuCommandBuilder(configuration);
        VirtualMachineDescriptor descriptor = new VirtualMachineDescriptor(computerId, 1, 512,
                "disk.qcow2", 1, GuestOperatingSystem.WINDOWS_95,
                FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_95));
        Path image = builder.blankFloppyPath(mediaId);
        Path disk = builder.diskPath(descriptor);
        try {
            try (LocalQemuBackend backend = new LocalQemuBackend(configuration)) {
                backend.create(descriptor);
            }
            Path qemuIo = qemu.resolveSibling("qemu-io.exe");
            Process check = new ProcessBuilder(qemuIo.toString(), "-f", "qcow2", "-c", "read -P 0 0 512", disk.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.INHERIT).start();
            try {
                if (!check.waitFor(5, TimeUnit.SECONDS) || check.exitValue() != 0) {
                    throw new AssertionError("New Win95 disk must have an untouched, zeroed first sector");
                }
            } finally {
                if (check.isAlive()) {
                    check.destroyForcibly().waitFor();
                }
            }
            Files.createDirectories(image.getParent());
            Files.write(image, new byte[1_474_560]);
            verifyMedia(builder, descriptor, VirtualMachineBootRequest.vmDisk(computerId), image, false);
            verifyMedia(builder, descriptor, VirtualMachineBootRequest.vmDisk(computerId)
                    .withFloppyMedia(FloppyMedia.blank(mediaId), ""), image, true);
            verifyMaintenance(configuration, builder, descriptor);
            System.out.println("Real QEMU: blank MBR, empty floppy, writable cold start, hot swap, eject, stopped-only backup/reset/restore and profile restore passed.");
        } finally {
            // All test data is contained in the newly created temporary root, never a configured player disk.
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) deleteAfterProcessExit(path);
            }
        }
    }

    private static void verifyMaintenance(LocalQemuConfiguration configuration, QemuCommandBuilder builder,
                                          VirtualMachineDescriptor descriptor) throws Exception {
        String id = descriptor.computerId();
        Path disk = builder.diskPath(descriptor);
        try (LocalQemuBackend backend = new LocalQemuBackend(configuration)) {
            backend.create(descriptor);
            Path backup = Path.of(backend.maintainDisk(id, DiskMaintenance.Operation.BACKUP));
            if (Files.mismatch(disk, backup) != -1) throw new AssertionError("Backup differs from the stopped disk");
            backend.start(id);
            for (DiskMaintenance.Operation operation : DiskMaintenance.Operation.values()) {
                try {
                    backend.maintainDisk(id, operation);
                    throw new AssertionError("Maintenance was allowed on a running VM: " + operation);
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().contains("Power off")) throw expected;
                }
            }
            backend.stop(id);
            Path retained = Path.of(backend.maintainDisk(id, DiskMaintenance.Operation.RESET));
            if (!Files.isRegularFile(retained) || !Files.isRegularFile(disk))
                throw new AssertionError("Reset must retain the old disk and create a new one");
            byte[] saved = Files.readAllBytes(backup);
            backend.create(new VirtualMachineDescriptor(id, descriptor.cpuCores(), descriptor.memoryMb(),
                    descriptor.diskFile(), descriptor.diskSizeGb(), GuestOperatingSystem.WINDOWS_98,
                    FirmwareConfiguration.forGuest(GuestOperatingSystem.WINDOWS_98)));
            backend.maintainDisk(id, DiskMaintenance.Operation.RESTORE);
            if (!java.util.Arrays.equals(saved, Files.readAllBytes(disk))
                    || backend.diagnostics(id).guest() != GuestOperatingSystem.WINDOWS_95)
                throw new AssertionError("Restore must recover disk bytes and the saved guest profile");
        }
    }

    private static void verifyMedia(QemuCommandBuilder builder, VirtualMachineDescriptor descriptor,
                                    VirtualMachineBootRequest request, Path image, boolean initiallyInserted) throws Exception {
        List<String> command = new ArrayList<>(builder.startCommand(descriptor, request));
        command.add("-S");
        Process process = new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try (QmpClient client = new QmpClient(Duration.ofMillis(500))) {
            QmpEndpoint endpoint = QmpEndpoint.forComputer(descriptor.computerId());
            waitUntilReady(client, endpoint, process);
            assertInserted(client, endpoint, initiallyInserted);
            client.sendJson(endpoint, QmpCommand.changeMedium("boot_floppy", image, false));
            assertInserted(client, endpoint, true);
            client.sendJson(endpoint, QmpCommand.eject("boot_floppy"));
            assertInserted(client, endpoint, false);
            client.sendJson(endpoint, QmpCommand.changeMedium("boot_floppy", image, true));
            JsonObject inserted = floppy(client, endpoint).getAsJsonObject("inserted");
            if (!inserted.get("ro").getAsBoolean()) {
                throw new AssertionError("Boot media must retain write protection after a swap");
            }
            client.sendJson(endpoint, QmpCommand.eject("boot_floppy"));
            assertInserted(client, endpoint, false);
            client.send(endpoint, "quit");
            process.waitFor(5, TimeUnit.SECONDS);
        } finally {
            process.destroy();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor();
            }
        }
    }

    private static void deleteAfterProcessExit(Path path) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try {
                Files.deleteIfExists(path);
                return;
            } catch (java.nio.file.FileSystemException failure) {
                if (attempt >= 20) {
                    throw failure;
                }
                Thread.sleep(50);
            }
        }
    }

    private static void waitUntilReady(QmpClient client, QmpEndpoint endpoint, Process process) throws Exception {
        IOException failure = null;
        for (int attempt = 0; attempt < 30 && process.isAlive(); attempt++) {
            try {
                client.send(endpoint, QmpCommand.QUERY_STATUS);
                return;
            } catch (IOException exception) {
                failure = exception;
                Thread.sleep(100);
            }
        }
        throw new AssertionError("Temporary QEMU did not become ready", failure);
    }

    private static void assertInserted(QmpClient client, QmpEndpoint endpoint, boolean expected) throws IOException {
        JsonObject device = floppy(client, endpoint);
        if (device.has("inserted") != expected) {
            throw new AssertionError("Unexpected floppy medium: " + device);
        }
        if (expected && device.getAsJsonObject("inserted").get("ro").getAsBoolean()) {
            throw new AssertionError("Writable media was mounted read-only");
        }
    }

    private static JsonObject floppy(QmpClient client, QmpEndpoint endpoint) throws IOException {
        JsonObject response = client.sendJson(endpoint, QmpCommand.execute("query-block"));
        for (JsonElement block : response.getAsJsonArray("return")) {
            JsonObject device = block.getAsJsonObject();
            if ("boot_floppy".equals(device.get("device").getAsString())) {
                return device;
            }
        }
        throw new AssertionError("QEMU has no persistent floppy drive: " + response);
    }
}
