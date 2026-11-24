package com.example.vm.vm.backend.local;

import com.example.vm.MineVM;
import com.example.vm.config.ComputerRuntimeConfig;
import com.example.vm.floppy.FloppyMedia;
import com.example.vm.input.VirtualMachineInput;
import com.example.vm.qmp.QmpClient;
import com.example.vm.qmp.QmpCommand;
import com.example.vm.qmp.QmpEndpoint;
import com.example.vm.qmp.QmpInputCommand;
import com.example.vm.screen.VncEndpoint;
import com.example.vm.vm.GuestOperatingSystem;
import com.example.vm.vm.VirtualMachineBootRequest;
import com.example.vm.vm.VirtualMachineDescriptor;
import com.example.vm.vm.VirtualMachineHandle;
import com.example.vm.vm.VirtualMachineState;
import com.example.vm.vm.backend.VirtualMachineBackend;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class LocalQemuBackend implements VirtualMachineBackend {
    private static final long QMP_KEEP_ALIVE_INITIAL_DELAY_SECONDS = 5L;
    private static final long QMP_KEEP_ALIVE_INTERVAL_SECONDS = 10L;
    private static final int QMP_READY_ATTEMPTS = 20;
    private static final long QMP_READY_DELAY_MS = 250L;
    private static final long INPUT_QMP_IDLE_RESET_NANOS = TimeUnit.SECONDS.toNanos(10L);
    private static final int FLOPPY_IMAGE_BYTES = 1_474_560;

    private final LocalQemuConfiguration configuration;
    private final QemuCommandBuilder commandBuilder;
    private final QmpClient qmpClient;
    private boolean closed;
    private final Map<String, VirtualMachineDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, VirtualMachineState> states = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> qmpKeepAliveTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> lastInputNanos = new ConcurrentHashMap<>();
    private final Map<String, String> lastErrors = new ConcurrentHashMap<>();
    private final ScheduledExecutorService qmpKeepAliveExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "minevm-qmp-keepalive");
        thread.setDaemon(true);
        return thread;
    });

    public LocalQemuBackend(LocalQemuConfiguration configuration) {
        this(configuration, new QemuCommandBuilder(configuration), new QmpClient());
    }

    LocalQemuBackend(LocalQemuConfiguration configuration, QemuCommandBuilder commandBuilder, QmpClient qmpClient) {
        this.configuration = configuration;
        this.commandBuilder = commandBuilder;
        this.qmpClient = qmpClient;
    }

    @Override
    public synchronized VirtualMachineHandle create(VirtualMachineDescriptor descriptor) {
        requireOpen();
        LocalQemuPreflight.validateTools(configuration);
        Process existingProcess = processes.get(descriptor.computerId());
        if (existingProcess != null && existingProcess.isAlive()) {
            return new VirtualMachineHandle(descriptor.computerId(), VirtualMachineState.RUNNING);
        }
        descriptors.put(descriptor.computerId(), descriptor);
        states.putIfAbsent(descriptor.computerId(), VirtualMachineState.STOPPED);

        Path diskPath = commandBuilder.diskPath(descriptor);
        if (!Files.exists(diskPath)) {
            runCreateDiskCommand(descriptor, diskPath);
        }
        prepareFirmwareFiles(descriptor);

        return new VirtualMachineHandle(descriptor.computerId(), VirtualMachineState.STOPPED);
    }

    @Override
    public VirtualMachineHandle start(String computerId) {
        return start(VirtualMachineBootRequest.vmDisk(computerId));
    }

    @Override
    public synchronized VirtualMachineHandle start(VirtualMachineBootRequest bootRequest) {
        requireOpen();
        LocalQemuPreflight.validateTools(configuration);
        LocalQemuPreflight.validateBootRequest(bootRequest);
        VirtualMachineDescriptor descriptor = requireDescriptor(bootRequest.computerId());
        Process existingProcess = processes.get(bootRequest.computerId());
        if (existingProcess != null && existingProcess.isAlive()) {
            states.put(bootRequest.computerId(), VirtualMachineState.RUNNING);
            lastErrors.remove(bootRequest.computerId());
            scheduleQmpKeepAlive(bootRequest.computerId(), existingProcess);
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        }

        states.put(bootRequest.computerId(), VirtualMachineState.STARTING);
        try {
            if (bootRequest.floppyMedia() != null && bootRequest.floppyMedia().isWritable()) {
                ensureBlankFloppyImage(bootRequest.floppyMedia().mediaId());
            }
            qmpClient.close(QmpEndpoint.forComputer(bootRequest.computerId()));
            java.util.List<String> command = commandBuilder.startCommand(descriptor, bootRequest);
            writeCommandLog(descriptor, command);
            Process process = new ProcessBuilder(command)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(commandBuilder.vmDirectory(descriptor).resolve("qemu-stderr.log").toFile())
                    .start();
            processes.put(bootRequest.computerId(), process);
            registerProcessExitHandler(bootRequest.computerId(), process);
            waitForQmpReady(bootRequest.computerId(), process);
            scheduleQmpKeepAlive(bootRequest.computerId(), process);
            MineVM.SCREEN_SERVICE.startVncReceiver(bootRequest.computerId(), VncEndpoint.forComputer(bootRequest.computerId()));
            states.put(bootRequest.computerId(), VirtualMachineState.RUNNING);
            lastErrors.remove(bootRequest.computerId());
            return new VirtualMachineHandle(bootRequest.computerId(), VirtualMachineState.RUNNING);
        } catch (IOException | RuntimeException exception) {
            lastErrors.put(bootRequest.computerId(), exception.toString());
            failStart(bootRequest.computerId());
            throw new IllegalStateException("Failed to start local QEMU VM: " + bootRequest.computerId(), exception);
        }
    }

    @Override
    public synchronized VirtualMachineHandle stop(String computerId) {
        requireDescriptor(computerId);
        states.put(computerId, VirtualMachineState.STOPPING);

        Process process = processes.get(computerId);
        if (process != null) {
            cancelQmpKeepAlive(computerId);
            trySendQmpCommand(computerId, QmpCommand.SYSTEM_POWERDOWN);
            if (!waitForProcessExit(process)) {
                process.destroy();
                if (!waitForProcessExit(process)) {
                    process.destroyForcibly();
                    waitForProcessExit(process);
                }
            }
            if (process.isAlive()) {
                states.put(computerId, VirtualMachineState.ERROR);
                throw new IllegalStateException("QEMU has not stopped; disk maintenance remains disabled");
            }
            processes.remove(computerId);
        }

        MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
        qmpClient.close(QmpEndpoint.forComputer(computerId));
        lastInputNanos.remove(computerId);
        states.put(computerId, VirtualMachineState.STOPPED);
        return new VirtualMachineHandle(computerId, VirtualMachineState.STOPPED);
    }

    @Override
    public synchronized com.example.vm.vm.VirtualMachineDiagnostics diagnostics(String computerId) {
        Process process = processes.get(computerId);
        long pid = process != null && process.isAlive() ? process.pid() : -1;
        long cpuMillis = process == null ? 0 : process.info().totalCpuDuration().orElse(java.time.Duration.ZERO).toMillis();
        String error = lastErrors.getOrDefault(computerId, "");
        VirtualMachineDescriptor descriptor = descriptors.get(computerId);
        if (descriptor != null) {
            Path stderr = commandBuilder.vmDirectory(descriptor).resolve("qemu-stderr.log");
            if (Files.isRegularFile(stderr)) {
                try (java.io.RandomAccessFile log = new java.io.RandomAccessFile(stderr.toFile(), "r")) {
                    log.seek(Math.max(0, log.length() - 2048));
                    byte[] tail = new byte[(int) Math.min(2048, log.length() - log.getFilePointer())];
                    log.readFully(tail);
                    String text = new String(tail, java.nio.charset.StandardCharsets.UTF_8).strip();
                    if (!text.isBlank()) error = text;
                } catch (IOException failure) { error = error + " | " + failure.getMessage(); }
            }
        }
        return new com.example.vm.vm.VirtualMachineDiagnostics(getState(computerId), pid, cpuMillis, error,
                descriptor == null ? null : descriptor.guestOperatingSystem());
    }

    @Override
    public synchronized String maintainDisk(String computerId, com.example.vm.vm.DiskMaintenance.Operation operation) {
        requireOpen();
        VirtualMachineDescriptor descriptor = requireDescriptor(computerId);
        Process process = processes.get(computerId);
        if (process != null && process.isAlive()) throw new IllegalStateException("Power off the VM before disk maintenance");
        Path disk = commandBuilder.diskPath(descriptor);
        Path vars = commandBuilder.vmDirectory(descriptor).resolve("OVMF_VARS.fd");
        Path profileBackup = disk.resolveSibling(disk.getFileName() + ".backup-profile");
        try {
            Path result;
            switch (operation) {
                case BACKUP -> {
                    result = com.example.vm.vm.DiskMaintenance.backup(disk);
                    if (Files.isRegularFile(vars)) com.example.vm.vm.DiskMaintenance.backup(vars);
                    Files.writeString(profileBackup, descriptor.guestOperatingSystem().name(), java.nio.charset.StandardCharsets.UTF_8);
                }
                case RESTORE -> {
                    GuestOperatingSystem restoredGuest = Files.isRegularFile(profileBackup)
                            ? GuestOperatingSystem.valueOf(Files.readString(profileBackup).strip()) : descriptor.guestOperatingSystem();
                    if (com.example.vm.vm.FirmwareConfiguration.forGuest(restoredGuest).requiresUefi()
                            && !Files.isRegularFile(vars.resolveSibling("OVMF_VARS.fd.backup")))
                        throw new IOException("Backup is missing its UEFI variable store");
                    result = com.example.vm.vm.DiskMaintenance.restore(disk);
                    if (com.example.vm.vm.FirmwareConfiguration.forGuest(restoredGuest).requiresUefi()) com.example.vm.vm.DiskMaintenance.restore(vars);
                    descriptors.put(computerId, new VirtualMachineDescriptor(computerId, descriptor.cpuCores(), descriptor.memoryMb(),
                            descriptor.diskFile(), descriptor.diskSizeGb(), restoredGuest, com.example.vm.vm.FirmwareConfiguration.forGuest(restoredGuest)));
                }
                case RESET -> {
                    com.example.vm.vm.DiskMaintenance.backup(disk);
                    Files.writeString(profileBackup, descriptor.guestOperatingSystem().name(), java.nio.charset.StandardCharsets.UTF_8);
                    result = com.example.vm.vm.DiskMaintenance.archive(disk);
                    if (Files.isRegularFile(vars)) {
                        com.example.vm.vm.DiskMaintenance.backup(vars);
                        com.example.vm.vm.DiskMaintenance.archive(vars);
                    }
                    runCreateDiskCommand(descriptor, disk);
                    prepareFirmwareFiles(descriptor);
                }
                default -> throw new IllegalArgumentException("Unknown disk operation");
            }
            states.put(computerId, VirtualMachineState.STOPPED);
            return result.toString();
        } catch (IOException failure) {
            throw new IllegalStateException("Disk maintenance failed; retained copies were not deleted", failure);
        }
    }

    @Override
    public synchronized VirtualMachineHandle restart(String computerId) {
        requireOpen();
        requireDescriptor(computerId);
        Process process = processes.get(computerId);
        if (process == null || !process.isAlive()) {
            return start(computerId);
        }

        sendQmpCommand(computerId, QmpCommand.SYSTEM_RESET);
        states.put(computerId, VirtualMachineState.RUNNING);
        return new VirtualMachineHandle(computerId, VirtualMachineState.RUNNING);
    }

    @Override
    public synchronized VirtualMachineHandle ejectSystemDisc(String computerId) {
        requireDescriptor(computerId);
        Process process = processes.get(computerId);
        VirtualMachineState state = getState(computerId);
        if (process != null && process.isAlive()) {
            sendQmpJson(computerId, QmpCommand.eject(QemuCommandBuilder.SYSTEM_DISC_DEVICE_ID));
        }
        return new VirtualMachineHandle(computerId, state);
    }

    @Override
    public synchronized VirtualMachineHandle ejectBootFloppy(String computerId) {
        requireDescriptor(computerId);
        Process process = processes.get(computerId);
        VirtualMachineState state = getState(computerId);
        if (process != null && process.isAlive()) {
            sendQmpJson(computerId, QmpCommand.eject(QemuCommandBuilder.BOOT_FLOPPY_DEVICE_ID));
        }
        return new VirtualMachineHandle(computerId, state);
    }

    @Override
    public synchronized VirtualMachineHandle changeBootFloppyMedium(String computerId, FloppyMedia media) {
        if (media == null) {
            throw new IllegalArgumentException("media must not be null");
        }
        VirtualMachineDescriptor descriptor = requireDescriptor(computerId);
        Process process = processes.get(computerId);
        if (process == null || !process.isAlive()) {
            throw new IllegalStateException("VM is not running: " + computerId);
        }

        Path imagePath = media.isWindows95Boot()
                ? commandBuilder.preparedBootFloppyPath(descriptor, ComputerRuntimeConfig.bootFloppyPathFor(GuestOperatingSystem.WINDOWS_95))
                : ensureBlankFloppyImage(media.mediaId());
        try {
            sendQmpJson(computerId, QmpCommand.changeMedium(QemuCommandBuilder.BOOT_FLOPPY_DEVICE_ID, imagePath, !media.isWritable()));
        } catch (RuntimeException exception) {
            if (!isUnsupportedReadOnlyMode(exception)) {
                throw new IllegalStateException("Failed to change floppy medium for " + computerId, exception);
            }
            sendQmpJson(computerId, QmpCommand.changeMediumLegacy(QemuCommandBuilder.BOOT_FLOPPY_DEVICE_ID, imagePath, !media.isWritable()));
        }
        VirtualMachineState state = getState(computerId);
        return new VirtualMachineHandle(computerId, state);
    }

    @Override
    public VirtualMachineState getState(String computerId) {
        Process process = processes.get(computerId);
        if (process != null && !process.isAlive()) {
            processes.remove(computerId);
            cancelQmpKeepAlive(computerId);
            MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
            qmpClient.close(QmpEndpoint.forComputer(computerId));
            states.put(computerId, VirtualMachineState.STOPPED);
        }
        if (states.get(computerId) == VirtualMachineState.ERROR && (process == null || !process.isAlive())) {
            states.put(computerId, descriptors.containsKey(computerId) ? VirtualMachineState.STOPPED : VirtualMachineState.NOT_CREATED);
        }
        return states.getOrDefault(computerId, VirtualMachineState.NOT_CREATED);
    }

    @Override
    public void sendInput(VirtualMachineInput input) {
        requireDescriptor(input.computerId());
        Process process = processes.get(input.computerId());
        if (process == null || !process.isAlive()) {
            if (process != null) {
                processes.remove(input.computerId(), process);
            }
            lastInputNanos.remove(input.computerId());
            states.put(input.computerId(), VirtualMachineState.STOPPED);
            return;
        }
        resetInputQmpSessionIfIdle(input.computerId());
        trySendQmpJson(input.computerId(), QmpInputCommand.toJson(input));
        lastInputNanos.put(input.computerId(), System.nanoTime());
    }

    @Override
    public synchronized void close() {
        closed = true;
        processes.keySet().forEach(MineVM.SCREEN_SERVICE::stopVncReceiver);
        qmpKeepAliveTasks.values().forEach(task -> task.cancel(false));
        qmpKeepAliveTasks.clear();
        qmpKeepAliveExecutor.shutdownNow();
        processes.values().forEach(LocalQemuBackend::terminateProcess);
        processes.clear();
        qmpClient.close();
        descriptors.clear();
        states.clear();
        lastInputNanos.clear();
    }

    private void runCreateDiskCommand(VirtualMachineDescriptor descriptor, Path diskPath) {
        Process process = null;
        try {
            Files.createDirectories(diskPath.getParent());
            process = new ProcessBuilder(commandBuilder.createDiskCommand(descriptor))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                states.put(descriptor.computerId(), VirtualMachineState.ERROR);
                throw new IllegalStateException("qemu-img exited with code " + exitCode + " for " + descriptor.computerId());
            }
        } catch (IOException exception) {
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to create VM disk: " + diskPath, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            terminateProcess(process);
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Interrupted while creating VM disk: " + diskPath, exception);
        }
    }

    private Path ensureBlankFloppyImage(String mediaId) {
        Path imagePath = commandBuilder.blankFloppyPath(mediaId);
        try {
            Files.createDirectories(imagePath.getParent());
            if (!Files.exists(imagePath)) {
                Files.write(imagePath, new byte[FLOPPY_IMAGE_BYTES], StandardOpenOption.CREATE_NEW);
            }
            return imagePath;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare writable floppy image: " + imagePath, exception);
        }
    }

    private static boolean isUnsupportedReadOnlyMode(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("read-only-mode") && message.contains("unexpected")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void writeCommandLog(VirtualMachineDescriptor descriptor, java.util.List<String> command) {
        Path commandPath = commandBuilder.vmDirectory(descriptor).resolve("qemu-command.txt");
        try {
            Files.createDirectories(commandPath.getParent());
            Files.writeString(commandPath, String.join(System.lineSeparator(), command));
        } catch (IOException exception) {
            MineVM.LOGGER.warn("Failed to write QEMU command log for {}", descriptor.computerId(), exception);
        }
    }

    private void prepareFirmwareFiles(VirtualMachineDescriptor descriptor) {
        if (!descriptor.firmwareConfiguration().requiresUefi()) {
            return;
        }

        Path varsPath = commandBuilder.vmDirectory(descriptor).resolve("OVMF_VARS.fd").normalize();
        if (Files.exists(varsPath)) {
            return;
        }

        try {
            Files.copy(new LocalVmPaths(configuration).uefiVarsTemplatePath(), varsPath);
        } catch (IOException exception) {
            states.put(descriptor.computerId(), VirtualMachineState.ERROR);
            throw new IllegalStateException("Failed to prepare UEFI variable store for " + descriptor.computerId(), exception);
        }
    }

    private void sendQmpCommand(String computerId, String command) {
        sendQmpJson(computerId, QmpCommand.execute(command));
    }

    private void trySendQmpCommand(String computerId, String command) {
        trySendQmpJson(computerId, QmpCommand.execute(command));
    }

    private void sendQmpJson(String computerId, String json) {
        try {
            qmpClient.sendJson(QmpEndpoint.forComputer(computerId), json);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to send QMP command to " + computerId, exception);
        }
    }

    private void trySendQmpJson(String computerId, String json) {
        try {
            qmpClient.sendJson(QmpEndpoint.forComputer(computerId), json);
        } catch (IOException exception) {
            MineVM.LOGGER.warn("Ignored non-critical QMP failure for {}", computerId, exception);
        }
    }

    private void waitForQmpReady(String computerId, Process process) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < QMP_READY_ATTEMPTS; attempt++) {
            if (!process.isAlive()) {
                throw new IOException("QEMU exited before QMP became ready");
            }
            try {
                qmpClient.sendJson(QmpEndpoint.forComputer(computerId), QmpCommand.execute(QmpCommand.QUERY_STATUS));
                return;
            } catch (IOException exception) {
                lastFailure = exception;
                sleepBeforeQmpRetry();
            }
        }
        throw lastFailure == null ? new IOException("QMP did not become ready") : lastFailure;
    }

    private static void sleepBeforeQmpRetry() {
        try {
            Thread.sleep(QMP_READY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean waitForProcessExit(Process process) {
        try {
            return process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void failStart(String computerId) {
        Process process = processes.remove(computerId);
        terminateProcess(process);
        cancelQmpKeepAlive(computerId);
        MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
        qmpClient.close(QmpEndpoint.forComputer(computerId));
        lastInputNanos.remove(computerId);
        states.put(computerId, VirtualMachineState.ERROR);
    }

    private static void terminateProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        if (!waitForProcessExit(process)) {
            process.destroyForcibly();
            waitForProcessExit(process);
        }
    }

    private void registerProcessExitHandler(String computerId, Process process) {
        process.onExit().thenAccept(exitedProcess -> {
            if (processes.remove(computerId, exitedProcess)) {
                cancelQmpKeepAlive(computerId);
                MineVM.SCREEN_SERVICE.stopVncReceiver(computerId);
                qmpClient.close(QmpEndpoint.forComputer(computerId));
                lastInputNanos.remove(computerId);
                states.put(computerId, VirtualMachineState.STOPPED);
            }
        });
    }

    private void resetInputQmpSessionIfIdle(String computerId) {
        Long lastInput = lastInputNanos.get(computerId);
        if (lastInput == null) {
            return;
        }
        long now = System.nanoTime();
        if (shouldResetInputQmpSession(lastInput, now)) {
            qmpClient.close(QmpEndpoint.forComputer(computerId));
            MineVM.LOGGER.debug("Reset idle QMP input session for {}", computerId);
        }
    }

    public static boolean shouldResetInputQmpSession(long lastInputNanos, long nowNanos) {
        return nowNanos - lastInputNanos >= INPUT_QMP_IDLE_RESET_NANOS;
    }

    private void scheduleQmpKeepAlive(String computerId, Process process) {
        qmpKeepAliveTasks.computeIfAbsent(computerId, ignored -> qmpKeepAliveExecutor.scheduleAtFixedRate(
                () -> {
                    if (!process.isAlive()) {
                        cancelQmpKeepAlive(computerId);
                        return;
                    }
                    trySendQmpCommand(computerId, QmpCommand.QUERY_STATUS);
                },
                QMP_KEEP_ALIVE_INITIAL_DELAY_SECONDS,
                QMP_KEEP_ALIVE_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        ));
    }

    private void cancelQmpKeepAlive(String computerId) {
        ScheduledFuture<?> task = qmpKeepAliveTasks.remove(computerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    private VirtualMachineDescriptor requireDescriptor(String computerId) {
        VirtualMachineDescriptor descriptor = descriptors.get(computerId);
        if (descriptor == null) {
            throw new IllegalStateException("VM has not been created: " + computerId);
        }
        return descriptor;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("QEMU backend is closed");
        }
    }
}
